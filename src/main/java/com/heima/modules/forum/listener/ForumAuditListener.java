package com.heima.modules.forum.listener;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.heima.modules.audit.enums.AuditStatus;
import com.heima.modules.audit.event.AuditCompletedEvent;
import com.heima.modules.forum.entity.ForumEntities.Board;
import com.heima.modules.forum.entity.ForumEntities.Post;
import com.heima.modules.forum.entity.ForumEntities.PostVersion;
import com.heima.modules.forum.entity.ForumEntities.Topic;
import com.heima.modules.forum.entity.ForumEntities.TopicVersion;
import com.heima.modules.forum.mapper.ForumMappers.BoardMapper;
import com.heima.modules.forum.mapper.ForumMappers.PostMapper;
import com.heima.modules.forum.mapper.ForumMappers.PostVersionMapper;
import com.heima.modules.forum.mapper.ForumMappers.TopicMapper;
import com.heima.modules.forum.mapper.ForumMappers.TopicVersionMapper;
import com.heima.modules.forum.service.ForumService;
import com.heima.modules.message.event.UserNotificationEvent;
import com.heima.modules.message.service.MentionService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ForumAuditListener {
    private static final List<String> OPEN_VERSION_STATUSES = List.of("PENDING", "MANUAL_REVIEW", "REJECTED", "FAILED");
    private final TopicMapper topicMapper;
    private final TopicVersionMapper topicVersionMapper;
    private final PostMapper postMapper;
    private final PostVersionMapper postVersionMapper;
    private final BoardMapper boardMapper;
    private final MentionService mentionService;
    private final ApplicationEventPublisher eventPublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onCompleted(AuditCompletedEvent event) {
        if (ForumService.TOPIC_AUDIT_BIZ_TYPE.equals(event.bizType())) onTopicCompleted(event);
        if (ForumService.POST_AUDIT_BIZ_TYPE.equals(event.bizType())) onPostCompleted(event);
    }

    private void onTopicCompleted(AuditCompletedEvent event) {
        Topic topic = topicMapper.selectById(event.bizId());
        if (topic == null || topic.getDeleted() == 1) return;
        TopicVersion version = topicVersionMapper.selectOne(new LambdaQueryWrapper<TopicVersion>()
                .eq(TopicVersion::getTopicId, topic.getId()).eq(TopicVersion::getVersionNo, event.versionNo()));
        if (version == null) return;
        if (!topic.getLatestVersionNo().equals(event.versionNo())) {
            topicVersionMapper.update(null, new LambdaUpdateWrapper<TopicVersion>().eq(TopicVersion::getId, version.getId())
                    .in(TopicVersion::getStatus, OPEN_VERSION_STATUSES).set(TopicVersion::getStatus, "SUPERSEDED")
                    .set(TopicVersion::getAuditReason, "已有更新版本，本结果不再生效"));
            return;
        }
        if (event.status() == AuditStatus.APPROVED) {
            int approved = topicVersionMapper.update(null, new LambdaUpdateWrapper<TopicVersion>().eq(TopicVersion::getId, version.getId())
                    .in(TopicVersion::getStatus, OPEN_VERSION_STATUSES).set(TopicVersion::getStatus, "APPROVED")
                    .set(TopicVersion::getAuditReason, null));
            if (approved == 0) return;
            boolean firstPublic = topic.getPublishedVersionNo() == null;
            int published = topicMapper.update(null, new LambdaUpdateWrapper<Topic>().eq(Topic::getId, topic.getId())
                    .eq(Topic::getLatestVersionNo, event.versionNo()).eq(Topic::getDeleted, 0).ne(Topic::getStatus, "OFFLINE")
                    .set(Topic::getPublishedVersionNo, event.versionNo()).set(Topic::getStatus, "PUBLISHED")
                    .set(Topic::getAuditReason, null));
            if (published == 0) return;
            if (firstPublic) {
                boardMapper.update(null, new LambdaUpdateWrapper<Board>().eq(Board::getId, topic.getBoardId())
                        .setSql("topic_count = GREATEST(0, topic_count + 1)"));
            }
            mentionService.publishApprovedMentions(version.getContent(), topic.getAuthorId(), "FORUM_TOPIC", topic.getId(), version.getVersionNo());
            return;
        }
        String status = workflowStatus(event.status());
        if (status == null) return;
        topicVersionMapper.update(null, new LambdaUpdateWrapper<TopicVersion>().eq(TopicVersion::getId, version.getId())
                .in(TopicVersion::getStatus, OPEN_VERSION_STATUSES).set(TopicVersion::getStatus, status)
                .set(TopicVersion::getAuditReason, event.reason()));
        topicMapper.update(null, new LambdaUpdateWrapper<Topic>().eq(Topic::getId, topic.getId())
                .eq(Topic::getLatestVersionNo, event.versionNo()).ne(Topic::getStatus, "OFFLINE")
                .set(Topic::getStatus, status).set(Topic::getAuditReason, event.reason()));
    }

    private void onPostCompleted(AuditCompletedEvent event) {
        Post post = postMapper.selectById(event.bizId());
        if (post == null || post.getDeleted() == 1) return;
        PostVersion version = postVersionMapper.selectOne(new LambdaQueryWrapper<PostVersion>()
                .eq(PostVersion::getPostId, post.getId()).eq(PostVersion::getVersionNo, event.versionNo()));
        if (version == null) return;
        if (!post.getLatestVersionNo().equals(event.versionNo())) {
            postVersionMapper.update(null, new LambdaUpdateWrapper<PostVersion>().eq(PostVersion::getId, version.getId())
                    .in(PostVersion::getStatus, OPEN_VERSION_STATUSES).set(PostVersion::getStatus, "SUPERSEDED")
                    .set(PostVersion::getAuditReason, "已有更新版本，本结果不再生效"));
            return;
        }
        if (event.status() == AuditStatus.APPROVED) {
            int approved = postVersionMapper.update(null, new LambdaUpdateWrapper<PostVersion>().eq(PostVersion::getId, version.getId())
                    .in(PostVersion::getStatus, OPEN_VERSION_STATUSES).set(PostVersion::getStatus, "APPROVED")
                    .set(PostVersion::getAuditReason, null));
            if (approved == 0) return;
            boolean firstPublic = post.getPublishedVersionNo() == null;
            int published = postMapper.update(null, new LambdaUpdateWrapper<Post>().eq(Post::getId, post.getId())
                    .eq(Post::getLatestVersionNo, event.versionNo()).eq(Post::getDeleted, 0).ne(Post::getStatus, "OFFLINE")
                    .set(Post::getPublishedVersionNo, event.versionNo()).set(Post::getStatus, "PUBLISHED")
                    .set(Post::getAuditReason, null));
            if (published == 0) return;
            if (firstPublic) {
                topicMapper.update(null, new LambdaUpdateWrapper<Topic>().eq(Topic::getId, post.getTopicId())
                        .setSql("post_count = GREATEST(0, post_count + 1)"));
                notifyReplyAuthor(post);
            }
            mentionService.publishApprovedMentions(version.getContent(), post.getAuthorId(), "FORUM_POST", post.getId(), version.getVersionNo());
            return;
        }
        String status = workflowStatus(event.status());
        if (status == null) return;
        postVersionMapper.update(null, new LambdaUpdateWrapper<PostVersion>().eq(PostVersion::getId, version.getId())
                .in(PostVersion::getStatus, OPEN_VERSION_STATUSES).set(PostVersion::getStatus, status)
                .set(PostVersion::getAuditReason, event.reason()));
        postMapper.update(null, new LambdaUpdateWrapper<Post>().eq(Post::getId, post.getId())
                .eq(Post::getLatestVersionNo, event.versionNo()).ne(Post::getStatus, "OFFLINE")
                .set(Post::getStatus, status).set(Post::getAuditReason, event.reason()));
    }

    private void notifyReplyAuthor(Post post) {
        Long recipientId = null;
        if (post.getReplyToId() != null) {
            Post replyTo = postMapper.selectById(post.getReplyToId());
            if (replyTo != null) recipientId = replyTo.getAuthorId();
        }
        if (recipientId == null) {
            Topic topic = topicMapper.selectById(post.getTopicId());
            if (topic != null) recipientId = topic.getAuthorId();
        }
        if (recipientId == null || recipientId.equals(post.getAuthorId())) return;
        eventPublisher.publishEvent(new UserNotificationEvent(recipientId, "INTERACTION", "收到新的回复",
                "你的" + (post.getReplyToId() == null ? "主题" : "跟帖") + "有了新的回复",
                "FORUM_POST", String.valueOf(post.getId()), "FORUM_REPLY:" + post.getId() + ":" + recipientId));
    }

    private String workflowStatus(AuditStatus status) {
        return switch (status) {
            case REJECTED -> "REJECTED";
            case MANUAL_REVIEW -> "MANUAL_REVIEW";
            case FAILED -> "FAILED";
            default -> null;
        };
    }
}
