package com.heima.modules.report.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.heima.common.PageResult;
import com.heima.exception.ServiceException;
import com.heima.modules.blog.entity.BlogArticle;
import com.heima.modules.blog.mapper.BlogArticleMapper;
import com.heima.modules.comment.entity.CommentEntities.Comment;
import com.heima.modules.comment.mapper.CommentMappers.CommentMapper;
import com.heima.modules.comment.service.ContentTargetService;
import com.heima.modules.forum.entity.ForumEntities.Board;
import com.heima.modules.forum.entity.ForumEntities.Post;
import com.heima.modules.forum.entity.ForumEntities.Topic;
import com.heima.modules.forum.mapper.ForumMappers.BoardMapper;
import com.heima.modules.forum.mapper.ForumMappers.PostMapper;
import com.heima.modules.forum.mapper.ForumMappers.TopicMapper;
import com.heima.modules.resource.dto.AdminResourceStatusDTO;
import com.heima.modules.resource.service.ResourceService;
import com.heima.modules.report.dto.ReportDtos.CreateRequest;
import com.heima.modules.report.dto.ReportDtos.HandleRequest;
import com.heima.modules.report.dto.ReportDtos.Query;
import com.heima.modules.report.entity.ReportEntities.Handle;
import com.heima.modules.report.entity.ReportEntities.Report;
import com.heima.modules.report.mapper.ReportMappers.HandleMapper;
import com.heima.modules.report.mapper.ReportMappers.ReportMapper;
import com.heima.modules.report.vo.ReportVO;
import com.heima.modules.message.event.UserNotificationEvent;
import com.heima.modules.search.event.SearchIndexRefreshEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {
    private final ReportMapper reportMapper;
    private final HandleMapper handleMapper;
    private final ContentTargetService targetService;
    private final BlogArticleMapper blogMapper;
    private final TopicMapper topicMapper;
    private final PostMapper postMapper;
    private final CommentMapper commentMapper;
    private final BoardMapper boardMapper;
    private final ResourceService resourceService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Long create(CreateRequest request, Long reporterId) {
        targetService.requireVisible(request.getTargetType(), request.getTargetId());
        Report report = new Report();
        report.setReporterId(reporterId);
        report.setTargetType(request.getTargetType());
        report.setTargetId(request.getTargetId());
        report.setReason(request.getReason());
        report.setDetail(request.getDetail());
        report.setStatus("PENDING");
        reportMapper.insert(report);
        return report.getId();
    }

    @Transactional(readOnly = true)
    public PageResult<ReportVO> userPage(Query query, Long reporterId) {
        LambdaQueryWrapper<Report> wrapper = new LambdaQueryWrapper<Report>().eq(Report::getReporterId, reporterId)
                .eq(query.getStatus() != null, Report::getStatus, query.getStatus())
                .eq(query.getTargetType() != null, Report::getTargetType, query.getTargetType()).orderByDesc(Report::getId);
        return result(reportMapper.selectPage(page(query), wrapper), query);
    }

    @Transactional(readOnly = true)
    public PageResult<ReportVO> adminPage(Query query) {
        LambdaQueryWrapper<Report> wrapper = new LambdaQueryWrapper<Report>()
                .eq(query.getStatus() != null, Report::getStatus, query.getStatus())
                .eq(query.getTargetType() != null, Report::getTargetType, query.getTargetType()).orderByDesc(Report::getId);
        return result(reportMapper.selectPage(page(query), wrapper), query);
    }

    @Transactional
    public void handle(Long id, HandleRequest request, Long handlerId) {
        Report report = reportMapper.selectById(id);
        if (report == null) throw new ServiceException(404, "举报不存在");
        String status = "REJECT".equals(request.getAction()) ? "REJECTED" : "PROCESSED";
        int updated = reportMapper.update(null, new LambdaUpdateWrapper<Report>().eq(Report::getId, id)
                .eq(Report::getStatus, "PENDING").set(Report::getStatus, status));
        if (updated == 0) throw new ServiceException(409, "举报已被处理，请刷新后重试");
        Handle handle = new Handle();
        handle.setReportId(id);
        handle.setHandlerId(handlerId);
        handle.setAction(request.getAction());
        handle.setNote(request.getNote());
        try {
            handleMapper.insert(handle);
        } catch (DuplicateKeyException e) {
            throw new ServiceException(409, "举报已存在处理记录");
        }
        if ("OFFLINE".equals(request.getAction())) offline(report.getTargetType(), report.getTargetId(), request.getNote());
        eventPublisher.publishEvent(new UserNotificationEvent(report.getReporterId(), "SYSTEM", "举报处理结果",
                "REJECTED".equals(status) ? "你提交的举报经核实未成立" : "你提交的举报已处理",
                "REPORT", String.valueOf(report.getId()), "REPORT_RESULT:" + report.getId()));
    }

    private void offline(String targetType, Long targetId, String note) {
        switch (targetType) {
            case "BLOG" -> {
                BlogArticle article = blogMapper.selectById(targetId);
                if (article == null || article.getDeleted() == 1) throw new ServiceException(404, "被举报文章不存在");
                blogMapper.update(null, new LambdaUpdateWrapper<BlogArticle>().eq(BlogArticle::getId, targetId)
                        .eq(BlogArticle::getDeleted, 0).set(BlogArticle::getStatus, "OFFLINE").set(BlogArticle::getAuditReason, note));
                eventPublisher.publishEvent(new SearchIndexRefreshEvent("BLOG", targetId));
            }
            case "FORUM" -> {
                Topic topic = topicMapper.selectById(targetId);
                if (topic == null || topic.getDeleted() == 1) throw new ServiceException(404, "被举报主题不存在");
                boolean wasPublic = isPublic(topic.getDeleted(), topic.getStatus(), topic.getPublishedVersionNo());
                topicMapper.update(null, new LambdaUpdateWrapper<Topic>().eq(Topic::getId, targetId).eq(Topic::getDeleted, 0)
                        .set(Topic::getStatus, "OFFLINE").set(Topic::getAuditReason, note));
                eventPublisher.publishEvent(new SearchIndexRefreshEvent("FORUM_TOPIC", targetId));
                if (wasPublic) boardMapper.update(null, new LambdaUpdateWrapper<Board>().eq(Board::getId, topic.getBoardId())
                        .setSql("topic_count = GREATEST(0, topic_count - 1)"));
            }
            case "FORUM_POST" -> {
                Post post = postMapper.selectById(targetId);
                if (post == null || post.getDeleted() == 1) throw new ServiceException(404, "被举报跟帖不存在");
                boolean wasPublic = isPublic(post.getDeleted(), post.getStatus(), post.getPublishedVersionNo());
                postMapper.update(null, new LambdaUpdateWrapper<Post>().eq(Post::getId, targetId).eq(Post::getDeleted, 0)
                        .set(Post::getStatus, "OFFLINE").set(Post::getAuditReason, note));
                if (wasPublic) topicMapper.update(null, new LambdaUpdateWrapper<Topic>().eq(Topic::getId, post.getTopicId())
                        .setSql("post_count = GREATEST(0, post_count - 1)"));
            }
            case "COMMENT" -> {
                Comment comment = commentMapper.selectById(targetId);
                if (comment == null || comment.getDeleted() == 1) throw new ServiceException(404, "被举报评论不存在");
                boolean wasPublic = isPublic(comment.getDeleted(), comment.getStatus(), comment.getPublishedVersionNo());
                commentMapper.update(null, new LambdaUpdateWrapper<Comment>().eq(Comment::getId, targetId).eq(Comment::getDeleted, 0)
                        .set(Comment::getStatus, "OFFLINE").set(Comment::getAuditReason, note));
                if (wasPublic) {
                    targetService.changeCommentCount(comment.getTargetType(), comment.getTargetId(), -1);
                    if (comment.getParentId() != null) {
                        commentMapper.update(null, new LambdaUpdateWrapper<Comment>().eq(Comment::getId, comment.getParentId())
                                .setSql("reply_count = GREATEST(0, reply_count - 1)"));
                    }
                }
            }
            case "RESOURCE" -> {
                AdminResourceStatusDTO status = new AdminResourceStatusDTO();
                status.setStatus("OFFLINE");
                status.setReason(note);
                resourceService.adminStatus(targetId, status);
            }
            default -> throw new ServiceException(400, "不支持的举报目标类型");
        }
    }

    private boolean isPublic(Integer deleted, String status, Integer publishedVersionNo) {
        return deleted == 0 && !"OFFLINE".equals(status) && publishedVersionNo != null;
    }

    private PageResult<ReportVO> result(Page<Report> page, Query query) {
        List<Long> ids = page.getRecords().stream().map(Report::getId).toList();
        Map<Long, Handle> handles = ids.isEmpty() ? Map.of() : handleMapper.selectList(new LambdaQueryWrapper<Handle>()
                        .in(Handle::getReportId, ids)).stream().collect(Collectors.toMap(Handle::getReportId, Function.identity()));
        List<ReportVO> records = page.getRecords().stream().map(report -> {
            ReportVO vo = new ReportVO();
            BeanUtils.copyProperties(report, vo);
            Handle handle = handles.get(report.getId());
            if (handle != null) {
                vo.setHandlerId(handle.getHandlerId());
                vo.setAction(handle.getAction());
                vo.setHandleNote(handle.getNote());
                vo.setHandleTime(handle.getCreateTime());
            }
            return vo;
        }).toList();
        return PageResult.of(records, page.getTotal(), pageNo(query), pageSize(query));
    }

    private long pageNo(Query query) { return Math.max(1, query.getPage()); }
    private long pageSize(Query query) { return Math.min(100, Math.max(1, query.getSize())); }
    private <T> Page<T> page(Query query) { return new Page<>(pageNo(query), pageSize(query)); }
}
