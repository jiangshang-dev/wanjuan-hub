package com.heima.modules.interaction.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.heima.common.PageResult;
import com.heima.exception.ServiceException;
import com.heima.modules.auth.entity.UserAccount;
import com.heima.modules.auth.mapper.UserAccountMapper;
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
import com.heima.modules.interaction.vo.InteractionVos.TargetVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
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
    private final ContentTargetService targetService;
    private final BlogArticleMapper blogMapper;
    private final TopicMapper topicMapper;
    private final PostMapper postMapper;
    private final CommentMapper commentMapper;

    @Transactional
    public void like(TargetRequest request, Long userId) {
        targetService.requireVisible(request.getTargetType(), request.getTargetId());
        Like like = new Like();
        like.setUserId(userId);
        like.setTargetType(request.getTargetType());
        like.setTargetId(request.getTargetId());
        try {
            likeMapper.insert(like);
        } catch (DuplicateKeyException e) {
            throw new ServiceException(409, "不能重复点赞");
        }
        changeTargetCount(request.getTargetType(), request.getTargetId(), true, 1);
    }

    @Transactional
    public void unlike(TargetRequest request, Long userId) {
        int deleted = likeMapper.delete(likeWrapper(userId, request));
        if (deleted == 0) throw new ServiceException(404, "尚未点赞该目标");
        changeTargetCount(request.getTargetType(), request.getTargetId(), true, -1);
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

    private LambdaQueryWrapper<Like> likeWrapper(Long userId, TargetRequest request) {
        return new LambdaQueryWrapper<Like>().eq(Like::getUserId, userId)
                .eq(Like::getTargetType, request.getTargetType()).eq(Like::getTargetId, request.getTargetId());
    }

    private LambdaQueryWrapper<Favorite> favoriteWrapper(Long userId, TargetRequest request) {
        return new LambdaQueryWrapper<Favorite>().eq(Favorite::getUserId, userId)
                .eq(Favorite::getTargetType, request.getTargetType()).eq(Favorite::getTargetId, request.getTargetId());
    }

    private void changeTargetCount(String targetType, Long targetId, boolean like, int delta) {
        String column = like ? "like_count" : "favorite_count";
        String sql = column + " = GREATEST(0, " + column + " + " + delta + ")";
        switch (targetType) {
            case "BLOG" -> blogMapper.update(null, new LambdaUpdateWrapper<BlogArticle>().eq(BlogArticle::getId, targetId).setSql(sql));
            case "FORUM" -> topicMapper.update(null, new LambdaUpdateWrapper<Topic>().eq(Topic::getId, targetId).setSql(sql));
            case "FORUM_POST" -> {
                if (like) postMapper.update(null, new LambdaUpdateWrapper<Post>().eq(Post::getId, targetId).setSql(sql));
            }
            case "COMMENT" -> {
                if (like) commentMapper.update(null, new LambdaUpdateWrapper<Comment>().eq(Comment::getId, targetId).setSql(sql));
            }
            case "RESOURCE" -> { }
            default -> throw new ServiceException(400, "不支持的目标类型");
        }
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
