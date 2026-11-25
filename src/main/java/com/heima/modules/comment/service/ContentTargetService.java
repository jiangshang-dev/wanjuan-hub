package com.heima.modules.comment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.heima.exception.ServiceException;
import com.heima.modules.blog.entity.BlogArticle;
import com.heima.modules.blog.mapper.BlogArticleMapper;
import com.heima.modules.comment.entity.CommentEntities.Comment;
import com.heima.modules.comment.mapper.CommentMappers.CommentMapper;
import com.heima.modules.forum.entity.ForumEntities.Post;
import com.heima.modules.forum.entity.ForumEntities.Topic;
import com.heima.modules.forum.mapper.ForumMappers.PostMapper;
import com.heima.modules.forum.mapper.ForumMappers.TopicMapper;
import com.heima.modules.resource.entity.ResourceInfo;
import com.heima.modules.resource.mapper.ResourceInfoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ContentTargetService {
    private final BlogArticleMapper blogMapper;
    private final TopicMapper topicMapper;
    private final PostMapper postMapper;
    private final CommentMapper commentMapper;
    private final ResourceInfoMapper resourceMapper;

    public void requireVisible(String targetType, Long targetId) {
        boolean visible = switch (targetType) {
            case "BLOG" -> blogMapper.selectCount(new LambdaQueryWrapper<BlogArticle>().eq(BlogArticle::getId, targetId)
                    .eq(BlogArticle::getDeleted, 0).ne(BlogArticle::getStatus, "OFFLINE").isNotNull(BlogArticle::getPublishedVersionNo)) > 0;
            case "FORUM" -> topicMapper.selectCount(new LambdaQueryWrapper<Topic>().eq(Topic::getId, targetId)
                    .eq(Topic::getDeleted, 0).ne(Topic::getStatus, "OFFLINE").isNotNull(Topic::getPublishedVersionNo)) > 0;
            case "FORUM_POST" -> postMapper.selectCount(new LambdaQueryWrapper<Post>().eq(Post::getId, targetId)
                    .eq(Post::getDeleted, 0).ne(Post::getStatus, "OFFLINE").isNotNull(Post::getPublishedVersionNo)) > 0;
            case "COMMENT" -> commentMapper.selectCount(new LambdaQueryWrapper<Comment>().eq(Comment::getId, targetId)
                    .eq(Comment::getDeleted, 0).ne(Comment::getStatus, "OFFLINE").isNotNull(Comment::getPublishedVersionNo)) > 0;
            case "RESOURCE" -> resourceMapper.selectCount(new LambdaQueryWrapper<ResourceInfo>().eq(ResourceInfo::getId, targetId)
                    .eq(ResourceInfo::getDeleted, 0).eq(ResourceInfo::getOnline, 1).eq(ResourceInfo::getInvalid, 0)
                    .eq(ResourceInfo::getStatus, "PUBLISHED").isNotNull(ResourceInfo::getPublishedVersionNo)) > 0;
            default -> throw new ServiceException(400, "不支持的目标类型");
        };
        if (!visible) throw new ServiceException(404, "目标不存在或未公开");
    }

    public void changeCommentCount(String targetType, Long targetId, int delta) {
        String expression = "GREATEST(0, comment_count + " + delta + ")";
        if ("BLOG".equals(targetType)) {
            blogMapper.update(null, new LambdaUpdateWrapper<BlogArticle>().eq(BlogArticle::getId, targetId).setSql("comment_count = " + expression));
        } else if ("FORUM".equals(targetType)) {
            topicMapper.update(null, new LambdaUpdateWrapper<Topic>().eq(Topic::getId, targetId).setSql("comment_count = " + expression));
        } else if ("RESOURCE".equals(targetType)) {
            resourceMapper.update(null, new LambdaUpdateWrapper<ResourceInfo>().eq(ResourceInfo::getId, targetId).setSql("comment_count = " + expression));
        }
    }
}
