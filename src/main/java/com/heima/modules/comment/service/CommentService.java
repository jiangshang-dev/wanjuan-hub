package com.heima.modules.comment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.heima.common.PageResult;
import com.heima.exception.ServiceException;
import com.heima.modules.comment.dto.CommentDtos.CreateRequest;
import com.heima.modules.comment.dto.CommentDtos.Query;
import com.heima.modules.comment.dto.CommentDtos.StatusRequest;
import com.heima.modules.comment.entity.CommentEntities.Comment;
import com.heima.modules.comment.entity.CommentEntities.Version;
import com.heima.modules.comment.mapper.CommentMappers.CommentMapper;
import com.heima.modules.comment.mapper.CommentMappers.VersionMapper;
import com.heima.modules.comment.vo.CommentVO;
import com.heima.modules.file.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentService {
    public static final String AUDIT_BIZ_TYPE = "COMMENT";
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private final CommentMapper commentMapper;
    private final VersionMapper versionMapper;
    private final ContentTargetService targetService;
    private final ContentAuditService auditService;
    private final FileService fileService;

    @Transactional
    public Long create(CreateRequest request, Long userId) {
        targetService.requireVisible(request.getTargetType(), request.getTargetId());
        List<Long> images = images(request.getImageFileIds());
        fileService.requireOwnedImages(images, userId);
        Comment parent = null;
        Long rootId = null;
        if (request.getParentId() != null) {
            parent = required(request.getParentId());
            requireSameTarget(parent, request);
            if (!isPublic(parent) && !parent.getAuthorId().equals(userId)) throw new ServiceException(404, "父评论不存在或未公开");
            rootId = parent.getRootId() == null ? parent.getId() : parent.getRootId();
        } else if (request.getReplyToId() != null) {
            throw new ServiceException(400, "指定被回复评论时必须同时指定父评论");
        }
        if (request.getReplyToId() != null) {
            Comment replyTo = required(request.getReplyToId());
            requireSameTarget(replyTo, request);
            Long replyRoot = replyTo.getRootId() == null ? replyTo.getId() : replyTo.getRootId();
            if (!rootId.equals(replyRoot)) throw new ServiceException(400, "被回复评论与父评论不属于同一根评论");
            if (!isPublic(replyTo) && !replyTo.getAuthorId().equals(userId)) throw new ServiceException(404, "被回复评论不存在或未公开");
        }
        Comment comment = new Comment();
        comment.setTargetType(request.getTargetType());
        comment.setTargetId(request.getTargetId());
        comment.setAuthorId(userId);
        comment.setRootId(rootId);
        comment.setParentId(request.getParentId());
        comment.setReplyToId(request.getReplyToId() == null ? request.getParentId() : request.getReplyToId());
        comment.setStatus("PENDING");
        comment.setLatestVersionNo(1);
        comment.setReplyCount(0L);
        comment.setLikeCount(0L);
        comment.setDeleted(0);
        commentMapper.insert(comment);
        Version version = version(comment.getId(), 1, request.getContent(), images);
        versionMapper.insert(version);
        fileService.bindReferences(images, AUDIT_BIZ_TYPE, comment.getId(), 1);
        auditService.submit(AUDIT_BIZ_TYPE, comment.getId(), 1, userId, request.getContent(), images);
        return comment.getId();
    }

    @Transactional(readOnly = true)
    public PageResult<CommentVO> publicRoots(Query query) {
        requireTargetQuery(query);
        targetService.requireVisible(query.getTargetType(), query.getTargetId());
        LambdaQueryWrapper<Comment> wrapper = publicWrapper(query.getTargetType(), query.getTargetId())
                .isNull(Comment::getRootId).orderByDesc(Comment::getId);
        Page<Comment> page = commentMapper.selectPage(new Page<>(pageNo(query), pageSize(query)), wrapper);
        return result(page, c -> toVO(c, version(c.getId(), c.getPublishedVersionNo()), true), query);
    }

    @Transactional(readOnly = true)
    public PageResult<CommentVO> publicReplies(Long rootId, Query query) {
        Comment root = required(rootId);
        if (!isPublic(root) || root.getRootId() != null) throw new ServiceException(404, "根评论不存在或未公开");
        if (query.getTargetType() != null && !root.getTargetType().equals(query.getTargetType())
                || query.getTargetId() != null && !root.getTargetId().equals(query.getTargetId())) {
            throw new ServiceException(400, "查询目标与根评论不一致");
        }
        targetService.requireVisible(root.getTargetType(), root.getTargetId());
        Page<Comment> page = commentMapper.selectPage(new Page<>(pageNo(query), pageSize(query)),
                publicWrapper(root.getTargetType(), root.getTargetId()).eq(Comment::getRootId, rootId).orderByAsc(Comment::getId));
        return result(page, c -> toVO(c, version(c.getId(), c.getPublishedVersionNo()), true), query);
    }

    @Transactional(readOnly = true)
    public PageResult<CommentVO> userPage(Query query, Long userId) {
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<Comment>().eq(Comment::getAuthorId, userId)
                .eq(Comment::getDeleted, 0).eq(query.getStatus() != null, Comment::getStatus, query.getStatus()).orderByDesc(Comment::getId);
        Page<Comment> page = commentMapper.selectPage(new Page<>(pageNo(query), pageSize(query)), wrapper);
        return result(page, c -> toVO(c, version(c.getId(), c.getLatestVersionNo()), false), query);
    }

    @Transactional
    public void delete(Long id, Long userId) {
        Comment comment = required(id);
        if (!comment.getAuthorId().equals(userId)) throw new ServiceException(403, "无权删除他人的评论");
        boolean wasPublic = isPublic(comment);
        int updated = commentMapper.update(null, new LambdaUpdateWrapper<Comment>().eq(Comment::getId, id)
                .eq(Comment::getAuthorId, userId).eq(Comment::getDeleted, 0).set(Comment::getDeleted, 1));
        if (updated == 0) throw new ServiceException(409, "评论状态已变化");
        if (wasPublic) changePublicCounts(comment, -1);
    }

    @Transactional(readOnly = true)
    public PageResult<CommentVO> adminPage(Query query) {
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<Comment>().eq(Comment::getDeleted, 0)
                .eq(query.getTargetType() != null, Comment::getTargetType, query.getTargetType())
                .eq(query.getTargetId() != null, Comment::getTargetId, query.getTargetId())
                .eq(query.getStatus() != null, Comment::getStatus, query.getStatus()).orderByDesc(Comment::getId);
        Page<Comment> page = commentMapper.selectPage(new Page<>(pageNo(query), pageSize(query)), wrapper);
        return result(page, c -> toVO(c, version(c.getId(), c.getLatestVersionNo()), false), query);
    }

    @Transactional
    public void adminStatus(Long id, StatusRequest request) {
        Comment comment = required(id);
        if ("PUBLISHED".equals(request.getStatus())) {
            if (comment.getPublishedVersionNo() == null) throw new ServiceException(409, "评论尚无审核通过的公开版本");
            Version published = version(id, comment.getPublishedVersionNo());
            if (!"APPROVED".equals(published.getStatus())) throw new ServiceException(409, "公开版本未审核通过");
        }
        boolean wasPublic = isPublic(comment);
        commentMapper.update(null, new LambdaUpdateWrapper<Comment>().eq(Comment::getId, id).eq(Comment::getDeleted, 0)
                .set(Comment::getStatus, request.getStatus()).set(Comment::getAuditReason, request.getReason()));
        if (wasPublic && "OFFLINE".equals(request.getStatus())) changePublicCounts(comment, -1);
        if (!wasPublic && "PUBLISHED".equals(request.getStatus())) changePublicCounts(comment, 1);
    }

    private void changePublicCounts(Comment comment, int delta) {
        targetService.changeCommentCount(comment.getTargetType(), comment.getTargetId(), delta);
        if (comment.getParentId() != null) {
            commentMapper.update(null, new LambdaUpdateWrapper<Comment>().eq(Comment::getId, comment.getParentId())
                    .setSql("reply_count = GREATEST(0, reply_count + " + delta + ")"));
        }
    }

    private Comment required(Long id) {
        Comment comment = commentMapper.selectById(id);
        if (comment == null || comment.getDeleted() == 1) throw new ServiceException(404, "评论不存在");
        return comment;
    }

    private void requireSameTarget(Comment comment, CreateRequest request) {
        if (!comment.getTargetType().equals(request.getTargetType()) || !comment.getTargetId().equals(request.getTargetId())) {
            throw new ServiceException(400, "父评论或被回复评论不属于当前目标");
        }
    }

    private boolean isPublic(Comment comment) {
        return comment.getDeleted() == 0 && !"OFFLINE".equals(comment.getStatus()) && comment.getPublishedVersionNo() != null;
    }

    private LambdaQueryWrapper<Comment> publicWrapper(String targetType, Long targetId) {
        return new LambdaQueryWrapper<Comment>().eq(Comment::getTargetType, targetType).eq(Comment::getTargetId, targetId)
                .eq(Comment::getDeleted, 0).ne(Comment::getStatus, "OFFLINE").isNotNull(Comment::getPublishedVersionNo);
    }

    private Version version(Long commentId, Integer versionNo) {
        Version version = versionMapper.selectOne(new LambdaQueryWrapper<Version>().eq(Version::getCommentId, commentId)
                .eq(Version::getVersionNo, versionNo));
        if (version == null) throw new ServiceException(500, "评论版本指针异常");
        return version;
    }

    private Version version(Long commentId, int versionNo, String content, List<Long> images) {
        Version version = new Version();
        version.setCommentId(commentId);
        version.setVersionNo(versionNo);
        version.setContent(content);
        version.setContentFileIds(JSON.writeValueAsString(images));
        version.setStatus("PENDING");
        return version;
    }

    private CommentVO toVO(Comment comment, Version version, boolean publicView) {
        CommentVO vo = new CommentVO();
        BeanUtils.copyProperties(comment, vo);
        vo.setContent(version.getContent());
        vo.setVersionNo(version.getVersionNo());
        vo.setImageFileIds(JSON.readValue(version.getContentFileIds(), new TypeReference<List<Long>>() {}));
        if (publicView) {
            vo.setStatus(null);
            vo.setAuditReason(null);
            vo.setLatestVersionNo(null);
        }
        return vo;
    }

    private void requireTargetQuery(Query query) {
        if (query.getTargetType() == null || query.getTargetId() == null) throw new ServiceException(400, "评论目标不能为空");
        if (!List.of("BLOG", "FORUM", "RESOURCE").contains(query.getTargetType())) throw new ServiceException(400, "不支持的目标类型");
    }

    private List<Long> images(List<Long> source) { return source == null ? List.of() : source.stream().distinct().toList(); }
    private long pageNo(Query query) { return Math.max(1, query.getPage()); }
    private long pageSize(Query query) { return Math.min(100, Math.max(1, query.getSize())); }
    private PageResult<CommentVO> result(Page<Comment> page, java.util.function.Function<Comment, CommentVO> mapper, Query query) {
        return PageResult.of(page.getRecords().stream().map(mapper).toList(), page.getTotal(), pageNo(query), pageSize(query));
    }
}
