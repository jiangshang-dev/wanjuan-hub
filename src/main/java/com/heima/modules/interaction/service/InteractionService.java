package com.heima.modules.interaction.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.heima.common.PageResult;
import com.heima.exception.ServiceException;
import com.heima.modules.auth.entity.UserAccount;
import com.heima.modules.auth.entity.UserProfile;
import com.heima.modules.auth.mapper.UserAccountMapper;
import com.heima.modules.auth.mapper.UserProfileMapper;
import com.heima.modules.blog.entity.BlogArticle;
import com.heima.modules.blog.mapper.BlogArticleMapper;
import com.heima.modules.comment.entity.CommentEntities.Comment;
import com.heima.modules.comment.mapper.CommentMappers.CommentMapper;
import com.heima.modules.comment.service.ContentTargetService;
import com.heima.modules.forum.entity.ForumEntities.Post;
import com.heima.modules.forum.entity.ForumEntities.Topic;
import com.heima.modules.forum.mapper.ForumMappers.PostMapper;
import com.heima.modules.forum.mapper.ForumMappers.TopicMapper;
import com.heima.modules.interaction.dto.InteractionDtos.Query;
import com.heima.modules.interaction.dto.InteractionDtos.TargetRequest;
import com.heima.modules.interaction.entity.InteractionEntities.Favorite;
import com.heima.modules.interaction.entity.InteractionEntities.Follow;
import com.heima.modules.interaction.entity.InteractionEntities.Like;
import com.heima.modules.interaction.mapper.InteractionMappers.FavoriteMapper;
import com.heima.modules.interaction.mapper.InteractionMappers.FollowMapper;
import com.heima.modules.interaction.mapper.InteractionMappers.LikeMapper;
import com.heima.modules.interaction.vo.InteractionVos.FollowVO;
import com.heima.modules.interaction.vo.InteractionVos.ReactionVO;
import com.heima.modules.interaction.vo.InteractionVos.TargetVO;
import com.heima.modules.message.event.UserNotificationEvent;
import com.heima.modules.operation.service.SiteExperienceService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InteractionService {
    private final LikeMapper likeMapper;
    private final FavoriteMapper favoriteMapper;
    private final FollowMapper followMapper;
    private final UserAccountMapper userMapper;
    private final UserProfileMapper profileMapper;
    private final ContentTargetService targetService;
    private final BlogArticleMapper blogMapper;
    private final TopicMapper topicMapper;
    private final PostMapper postMapper;
    private final CommentMapper commentMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final SiteExperienceService siteExperienceService;

    @Transactional
    public void like(TargetRequest request, Long userId) {
        request.setReactionType("LIKE");
        reactInternal(request, userId, false);
    }

    @Transactional
    public void unlike(TargetRequest request, Long userId) {
        request.setReactionType("LIKE");
        if (!cancelInternal(request, userId)) throw new ServiceException(404, "尚未点赞该目标");
    }

    @Transactional
    public ReactionVO react(TargetRequest request, Long userId) {
        return reactInternal(request, userId, true);
    }

    @Transactional
    public ReactionVO unreact(TargetRequest request, Long userId) {
        cancelInternal(request, userId);
        return reactionStatus(request.getTargetType(), request.getTargetId(), userId);
    }

    @Transactional(readOnly = true)
    public ReactionVO reactionStatus(String targetType, Long targetId, Long userId) {
        targetService.requireVisible(targetType, targetId);
        ReactionVO vo = emptyReaction(targetType, targetId);
        fillCounts(vo);
        if (userId != null) {
            Like existing = likeMapper.selectOne(reactionWrapper(userId, targetType, targetId).last("LIMIT 1"));
            vo.setUserReaction(existing == null ? "NONE" : existing.getReactionType());
        } else {
            vo.setUserReaction("NONE");
        }
        return vo;
    }

    private ReactionVO reactInternal(TargetRequest request, Long userId, boolean idempotent) {
        String reactionType = reactionType(request);
        requireReactionTarget(request.getTargetType());
        targetService.requireVisible(request.getTargetType(), request.getTargetId());
        requireReactionWritable();
        Long authorId = targetService.targetAuthorId(request.getTargetType(), request.getTargetId());
        requireAuthorAllows(authorId, userId, reactionType);

        Like existing = lockReaction(userId, request.getTargetType(), request.getTargetId());
        if (existing != null && reactionType.equals(existing.getReactionType())) {
            if (idempotent) return reactionStatus(request.getTargetType(), request.getTargetId(), userId);
            throw new ServiceException(409, "LIKE".equals(reactionType) ? "不能重复点赞" : "不能重复点踩");
        }
        if (existing == null) {
            Like reaction = new Like();
            reaction.setUserId(userId);
            reaction.setTargetType(request.getTargetType());
            reaction.setTargetId(request.getTargetId());
            reaction.setReactionType(reactionType);
            try {
                likeMapper.insert(reaction);
            } catch (DuplicateKeyException e) {
                throw new ServiceException(409, "赞踩操作冲突，请刷新后重试");
            }
            changeReactionCount(request.getTargetType(), request.getTargetId(), reactionType, 1);
        } else {
            String previous = existing.getReactionType();
            int updated = likeMapper.update(null, new LambdaUpdateWrapper<Like>()
                    .eq(Like::getId, existing.getId()).eq(Like::getReactionType, previous)
                    .set(Like::getReactionType, reactionType));
            if (updated != 1) throw new ServiceException(409, "赞踩状态已变化，请刷新后重试");
            changeReactionCount(request.getTargetType(), request.getTargetId(), previous, -1);
            changeReactionCount(request.getTargetType(), request.getTargetId(), reactionType, 1);
        }
        notifyTarget(request.getTargetType(), request.getTargetId(), userId,
                "LIKE".equals(reactionType) ? "点赞" : "点踩", reactionType);
        return reactionStatus(request.getTargetType(), request.getTargetId(), userId);
    }

    private boolean cancelInternal(TargetRequest request, Long userId) {
        String reactionType = reactionType(request);
        requireReactionTarget(request.getTargetType());
        targetService.requireVisible(request.getTargetType(), request.getTargetId());
        requireReactionWritable();
        Like existing = lockReaction(userId, request.getTargetType(), request.getTargetId());
        if (existing == null || !reactionType.equals(existing.getReactionType())) return false;
        int deleted = likeMapper.delete(new LambdaQueryWrapper<Like>().eq(Like::getId, existing.getId())
                .eq(Like::getReactionType, reactionType));
        if (deleted == 0) throw new ServiceException(409, "赞踩状态已变化，请刷新后重试");
        changeReactionCount(request.getTargetType(), request.getTargetId(), reactionType, -1);
        return true;
    }

    @Transactional
    public void favorite(TargetRequest request, Long userId) {
        targetService.requireVisible(request.getTargetType(), request.getTargetId());
        Favorite favorite = new Favorite();
        favorite.setUserId(userId);
        favorite.setTargetType(request.getTargetType());
        favorite.setTargetId(request.getTargetId());
        try {
            favoriteMapper.insert(favorite);
        } catch (DuplicateKeyException e) {
            throw new ServiceException(409, "不能重复收藏");
        }
        changeTargetCount(request.getTargetType(), request.getTargetId(), false, 1);
        notifyTarget(request.getTargetType(), request.getTargetId(), userId, "收藏", "FAVORITE");
    }

    @Transactional
    public void unfavorite(TargetRequest request, Long userId) {
        int deleted = favoriteMapper.delete(favoriteWrapper(userId, request));
        if (deleted == 0) throw new ServiceException(404, "尚未收藏该目标");
        changeTargetCount(request.getTargetType(), request.getTargetId(), false, -1);
    }

    @Transactional(readOnly = true)
    public PageResult<TargetVO> likes(Query query, Long userId) {
        LambdaQueryWrapper<Like> wrapper = new LambdaQueryWrapper<Like>().eq(Like::getUserId, userId)
                .eq(Like::getReactionType, "LIKE")
                .eq(query.getTargetType() != null, Like::getTargetType, query.getTargetType()).orderByDesc(Like::getId);
        Page<Like> page = likeMapper.selectPage(page(query), wrapper);
        return PageResult.of(page.getRecords().stream().map(this::targetVO).toList(), page.getTotal(), pageNo(query), pageSize(query));
    }

    @Transactional(readOnly = true)
    public PageResult<TargetVO> favorites(Query query, Long userId) {
        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<Favorite>().eq(Favorite::getUserId, userId)
                .eq(query.getTargetType() != null, Favorite::getTargetType, query.getTargetType()).orderByDesc(Favorite::getId);
        Page<Favorite> page = favoriteMapper.selectPage(page(query), wrapper);
        return PageResult.of(page.getRecords().stream().map(this::targetVO).toList(), page.getTotal(), pageNo(query), pageSize(query));
    }

    @Transactional
    public void follow(Long followeeId, Long followerId) {
        if (followeeId.equals(followerId)) throw new ServiceException(400, "不能关注自己");
        if (userMapper.selectCount(new LambdaQueryWrapper<UserAccount>().eq(UserAccount::getId, followeeId)
                .eq(UserAccount::getStatus, 1)) == 0) throw new ServiceException(404, "被关注用户不存在或已停用");
        Follow follow = new Follow();
        follow.setFollowerId(followerId);
        follow.setFolloweeId(followeeId);
        try {
            followMapper.insert(follow);
        } catch (DuplicateKeyException e) {
            throw new ServiceException(409, "不能重复关注");
        }
        eventPublisher.publishEvent(new UserNotificationEvent(followeeId, "INTERACTION", "你有了新的关注者",
                "有用户关注了你", "USER", String.valueOf(followeeId), "FOLLOW:" + follow.getId()));
    }

    @Transactional
    public void unfollow(Long followeeId, Long followerId) {
        if (followMapper.delete(new LambdaQueryWrapper<Follow>().eq(Follow::getFollowerId, followerId)
                .eq(Follow::getFolloweeId, followeeId)) == 0) throw new ServiceException(404, "尚未关注该用户");
    }

    @Transactional(readOnly = true)
    public PageResult<FollowVO> following(Query query, Long userId) {
        Page<Follow> page = followMapper.selectPage(page(query), new LambdaQueryWrapper<Follow>()
                .eq(Follow::getFollowerId, userId).orderByDesc(Follow::getId));
        return followResult(page, query);
    }

    @Transactional(readOnly = true)
    public PageResult<FollowVO> followers(Query query, Long userId) {
        Page<Follow> page = followMapper.selectPage(page(query), new LambdaQueryWrapper<Follow>()
                .eq(Follow::getFolloweeId, userId).orderByDesc(Follow::getId));
        return followResult(page, query);
    }

    private LambdaQueryWrapper<Like> reactionWrapper(Long userId, String targetType, Long targetId) {
        return new LambdaQueryWrapper<Like>().eq(Like::getUserId, userId)
                .eq(Like::getTargetType, targetType).eq(Like::getTargetId, targetId);
    }

    private Like lockReaction(Long userId, String targetType, Long targetId) {
        return likeMapper.selectOne(reactionWrapper(userId, targetType, targetId).last("FOR UPDATE"));
    }

    private LambdaQueryWrapper<Favorite> favoriteWrapper(Long userId, TargetRequest request) {
        return new LambdaQueryWrapper<Favorite>().eq(Favorite::getUserId, userId)
                .eq(Favorite::getTargetType, request.getTargetType()).eq(Favorite::getTargetId, request.getTargetId());
    }

    private void changeReactionCount(String targetType, Long targetId, String reactionType, int delta) {
        String column = "LIKE".equals(reactionType) ? "like_count" : "dislike_count";
        String sql = column + " = GREATEST(0, " + column + " + " + delta + ")";
        switch (targetType) {
            case "BLOG" -> requireCountUpdated(blogMapper.update(null, new LambdaUpdateWrapper<BlogArticle>()
                    .eq(BlogArticle::getId, targetId).setSql(sql)));
            case "FORUM" -> requireCountUpdated(topicMapper.update(null, new LambdaUpdateWrapper<Topic>()
                    .eq(Topic::getId, targetId).setSql(sql)));
            case "FORUM_POST" -> requireCountUpdated(postMapper.update(null, new LambdaUpdateWrapper<Post>()
                    .eq(Post::getId, targetId).setSql(sql)));
            case "COMMENT" -> requireCountUpdated(commentMapper.update(null, new LambdaUpdateWrapper<Comment>()
                    .eq(Comment::getId, targetId).setSql(sql)));
            default -> throw new ServiceException(400, "不支持的赞踩目标类型");
        }
    }

    private void changeTargetCount(String targetType, Long targetId, boolean like, int delta) {
        String column = like ? "like_count" : "favorite_count";
        String sql = column + " = GREATEST(0, " + column + " + " + delta + ")";
        switch (targetType) {
            case "BLOG" -> blogMapper.update(null, new LambdaUpdateWrapper<BlogArticle>().eq(BlogArticle::getId, targetId).setSql(sql));
            case "FORUM" -> topicMapper.update(null, new LambdaUpdateWrapper<Topic>().eq(Topic::getId, targetId).setSql(sql));
            case "RESOURCE" -> { }
            default -> throw new ServiceException(400, "不支持的收藏目标类型");
        }
    }

    private void requireCountUpdated(int updated) {
        if (updated != 1) throw new ServiceException(409, "互动目标已变化，请刷新后重试");
    }

    private ReactionVO emptyReaction(String targetType, Long targetId) {
        ReactionVO vo = new ReactionVO();
        vo.setTargetType(targetType);
        vo.setTargetId(targetId);
        vo.setGrayDay(siteExperienceService.isGrayDay());
        vo.setGrayReason(siteExperienceService.grayReason());
        Long authorId = targetService.targetAuthorId(targetType, targetId);
        UserProfile profile = authorId == null ? null : profileMapper.selectById(authorId);
        vo.setLikeAllowed(profile == null || profile.getAllowLike() == null || profile.getAllowLike());
        vo.setDislikeAllowed(profile == null || profile.getAllowDislike() == null || profile.getAllowDislike());
        return vo;
    }

    private void fillCounts(ReactionVO vo) {
        Long likeCount;
        Long dislikeCount;
        switch (vo.getTargetType()) {
            case "BLOG" -> {
                BlogArticle target = blogMapper.selectById(vo.getTargetId());
                likeCount = target == null ? 0L : target.getLikeCount();
                dislikeCount = target == null ? 0L : target.getDislikeCount();
            }
            case "FORUM" -> {
                Topic target = topicMapper.selectById(vo.getTargetId());
                likeCount = target == null ? 0L : target.getLikeCount();
                dislikeCount = target == null ? 0L : target.getDislikeCount();
            }
            case "FORUM_POST" -> {
                Post target = postMapper.selectById(vo.getTargetId());
                likeCount = target == null ? 0L : target.getLikeCount();
                dislikeCount = target == null ? 0L : target.getDislikeCount();
            }
            case "COMMENT" -> {
                Comment target = commentMapper.selectById(vo.getTargetId());
                likeCount = target == null ? 0L : target.getLikeCount();
                dislikeCount = target == null ? 0L : target.getDislikeCount();
            }
            default -> throw new ServiceException(400, "不支持的赞踩目标类型");
        }
        vo.setLikeCount(likeCount == null ? 0L : likeCount);
        vo.setDislikeCount(dislikeCount == null ? 0L : dislikeCount);
    }

    private void requireReactionWritable() {
        if (siteExperienceService.isGrayDay()) {
            String reason = siteExperienceService.grayReason();
            throw new ServiceException(409, reason == null || reason.isBlank() ? "特殊纪念日暂停点赞和点踩" : reason);
        }
    }

    private void requireAuthorAllows(Long authorId, Long actorId, String reactionType) {
        if (authorId == null || authorId.equals(actorId)) return;
        UserProfile profile = profileMapper.selectById(authorId);
        if (profile == null) return;
        boolean allowed = "LIKE".equals(reactionType)
                ? profile.getAllowLike() == null || profile.getAllowLike()
                : profile.getAllowDislike() == null || profile.getAllowDislike();
        if (!allowed) throw new ServiceException(403, "作者已关闭" + ("LIKE".equals(reactionType) ? "点赞" : "点踩"));
    }

    private void requireReactionTarget(String targetType) {
        if (!java.util.Set.of("BLOG", "FORUM", "FORUM_POST", "COMMENT").contains(targetType)) {
            throw new ServiceException(400, "不支持的赞踩目标类型");
        }
    }

    private String reactionType(TargetRequest request) {
        return request.getReactionType() == null ? "LIKE" : request.getReactionType();
    }

    private TargetVO targetVO(Like like) {
        TargetVO vo = new TargetVO();
        BeanUtils.copyProperties(like, vo);
        return vo;
    }

    private TargetVO targetVO(Favorite favorite) {
        TargetVO vo = new TargetVO();
        BeanUtils.copyProperties(favorite, vo);
        return vo;
    }

    private void notifyTarget(String targetType, Long targetId, Long actorId, String action, String eventType) {
        Long recipientId = targetService.targetAuthorId(targetType, targetId);
        if (recipientId == null || recipientId.equals(actorId)) return;
        eventPublisher.publishEvent(new UserNotificationEvent(recipientId, "INTERACTION", "你的内容被" + action,
                "有用户" + action + "了你的内容", targetType, String.valueOf(targetId),
                eventType + ":" + targetId + ":" + recipientId + ":" + actorId));
    }

    private PageResult<FollowVO> followResult(Page<Follow> page, Query query) {
        return PageResult.of(page.getRecords().stream().map(item -> {
            FollowVO vo = new FollowVO();
            BeanUtils.copyProperties(item, vo);
            return vo;
        }).toList(), page.getTotal(), pageNo(query), pageSize(query));
    }

    private long pageNo(Query query) { return Math.max(1, query.getPage()); }
    private long pageSize(Query query) { return Math.min(100, Math.max(1, query.getSize())); }
    private <T> Page<T> page(Query query) { return new Page<>(pageNo(query), pageSize(query)); }
}
