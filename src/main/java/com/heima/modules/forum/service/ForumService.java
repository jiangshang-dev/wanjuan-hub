package com.heima.modules.forum.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.heima.common.PageResult;
import com.heima.exception.ServiceException;
import com.heima.modules.auth.entity.UserAccount;
import com.heima.modules.auth.mapper.UserAccountMapper;
import com.heima.modules.comment.service.ContentAuditService;
import com.heima.modules.file.service.FileService;
import com.heima.modules.forum.dto.ForumDtos.BoardRequest;
import com.heima.modules.forum.dto.ForumDtos.ModerationRequest;
import com.heima.modules.forum.dto.ForumDtos.ModeratorRequest;
import com.heima.modules.forum.dto.ForumDtos.PostQuery;
import com.heima.modules.forum.dto.ForumDtos.PostRequest;
import com.heima.modules.forum.dto.ForumDtos.Query;
import com.heima.modules.forum.dto.ForumDtos.StatusRequest;
import com.heima.modules.forum.dto.ForumDtos.TopicRequest;
import com.heima.modules.forum.entity.ForumEntities.Board;
import com.heima.modules.forum.entity.ForumEntities.Moderator;
import com.heima.modules.forum.entity.ForumEntities.Post;
import com.heima.modules.forum.entity.ForumEntities.PostVersion;
import com.heima.modules.forum.entity.ForumEntities.Section;
import com.heima.modules.forum.entity.ForumEntities.Topic;
import com.heima.modules.forum.entity.ForumEntities.TopicVersion;
import com.heima.modules.forum.mapper.ForumMappers.BoardMapper;
import com.heima.modules.forum.mapper.ForumMappers.ModeratorMapper;
import com.heima.modules.forum.mapper.ForumMappers.PostMapper;
import com.heima.modules.forum.mapper.ForumMappers.PostVersionMapper;
import com.heima.modules.forum.mapper.ForumMappers.SectionMapper;
import com.heima.modules.forum.mapper.ForumMappers.TopicMapper;
import com.heima.modules.forum.mapper.ForumMappers.TopicVersionMapper;
import com.heima.modules.forum.vo.ForumVos.BoardVO;
import com.heima.modules.forum.vo.ForumVos.ModeratorVO;
import com.heima.modules.forum.vo.ForumVos.PostVO;
import com.heima.modules.forum.vo.ForumVos.TopicVO;
import com.heima.modules.search.event.SearchIndexRefreshEvent;
import com.heima.modules.system.mapper.SysAdminMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ForumService {
    public static final String TOPIC_AUDIT_BIZ_TYPE = "FORUM_TOPIC";
    public static final String POST_AUDIT_BIZ_TYPE = "FORUM_POST";
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final SectionMapper sectionMapper;
    private final BoardMapper boardMapper;
    private final ModeratorMapper moderatorMapper;
    private final TopicMapper topicMapper;
    private final TopicVersionMapper topicVersionMapper;
    private final PostMapper postMapper;
    private final PostVersionMapper postVersionMapper;
    private final UserAccountMapper userMapper;
    private final SysAdminMapper adminMapper;
    private final FileService fileService;
    private final ContentAuditService auditService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public List<BoardVO> publicBoards() {
        List<Long> sectionIds = sectionMapper.selectList(new LambdaQueryWrapper<Section>().eq(Section::getStatus, 1))
                .stream().map(Section::getId).toList();
        if (sectionIds.isEmpty()) return List.of();
        return boardMapper.selectList(new LambdaQueryWrapper<Board>().eq(Board::getStatus, 1)
                        .in(Board::getSectionId, sectionIds).orderByAsc(Board::getSectionId)
                        .orderByAsc(Board::getSortOrder).orderByAsc(Board::getId).last("LIMIT 100"))
                .stream().map(this::boardVO).toList();
    }

    @Transactional(readOnly = true)
    public PageResult<TopicVO> publicTopics(Query query) {
        LambdaQueryWrapper<Topic> wrapper = publicTopicWrapper(query.getBoardId());
        applyKeyword(wrapper, query.getKeyword(), false);
        if ("ESSENCE".equals(query.getSort())) wrapper.eq(Topic::getEssence, 1);
        if ("HOT".equals(query.getSort())) {
            wrapper.orderByDesc(Topic::getTopped).orderByDesc(Topic::getViewCount)
                    .orderByDesc(Topic::getLikeCount).orderByDesc(Topic::getId);
        } else {
            wrapper.orderByDesc(Topic::getTopped).orderByDesc(Topic::getId);
        }
        Page<Topic> page = topicMapper.selectPage(page(query), wrapper);
        return topicResult(page, query, true);
    }

    @Transactional
    public TopicVO publicTopic(Long id) {
        Topic topic = topicMapper.selectOne(publicTopicWrapper(null).eq(Topic::getId, id));
        if (topic == null) throw new ServiceException(404, "主题不存在或未公开");
        TopicVO vo = topicVO(topic, topicVersion(id, topic.getPublishedVersionNo()), true);
        topicMapper.update(null, new LambdaUpdateWrapper<Topic>().eq(Topic::getId, id).setSql("view_count = view_count + 1"));
        return vo;
    }

    @Transactional(readOnly = true)
    public PageResult<PostVO> publicPosts(Long topicId, PostQuery query) {
        Topic topic = requiredTopic(topicId);
        if (!isPublic(topic)) throw new ServiceException(404, "主题不存在或未公开");
        boolean descending = "DESC".equals(query.getOrder());
        LambdaQueryWrapper<Post> wrapper = publicPostWrapper(topicId)
                .eq(Boolean.TRUE.equals(query.getAuthorOnly()), Post::getAuthorId, topic.getAuthorId())
                .orderByAsc(!descending, Post::getFloorNo).orderByAsc(!descending, Post::getId)
                .orderByDesc(descending, Post::getFloorNo).orderByDesc(descending, Post::getId);
        Page<Post> page = postMapper.selectPage(page(query), wrapper);
        return postResult(page, query, true);
    }

    @Transactional(readOnly = true)
    public PageResult<TopicVO> userTopics(Query query, Long userId) {
        LambdaQueryWrapper<Topic> wrapper = new LambdaQueryWrapper<Topic>().eq(Topic::getAuthorId, userId)
                .eq(Topic::getDeleted, 0).eq(query.getBoardId() != null, Topic::getBoardId, query.getBoardId())
                .eq(query.getStatus() != null && !query.getStatus().isBlank(), Topic::getStatus, query.getStatus())
                .orderByDesc(Topic::getId);
        applyKeyword(wrapper, query.getKeyword(), true);
        return topicResult(topicMapper.selectPage(page(query), wrapper), query, false);
    }

    @Transactional(readOnly = true)
    public TopicVO userTopic(Long id, Long userId) {
        Topic topic = ownedTopic(id, userId);
        return topicVO(topic, topicVersion(id, topic.getLatestVersionNo()), false);
    }

    @Transactional
    public Long createTopic(TopicRequest request, Long userId) {
        Board board = enabledBoard(request.getBoardId());
        requireUserPermission(board.getPostPermission(), "该板块不允许普通用户发帖");
        return createTopicInternal(request, userId);
    }

    @Transactional
    public void updateTopic(Long id, TopicRequest request, Long userId) {
        Topic topic = ownedTopic(id, userId);
        if ("OFFLINE".equals(topic.getStatus())) throw new ServiceException(409, "已下线主题不能编辑");
        if (!topic.getBoardId().equals(request.getBoardId())) throw new ServiceException(400, "用户编辑时不能变更板块");
        enabledBoard(topic.getBoardId());
        updateTopicInternal(topic, request, userId);
    }

    @Transactional
    public void deleteTopic(Long id, Long userId) {
        Topic topic = ownedTopic(id, userId);
        deleteTopicInternal(topic, true);
    }

    @Transactional(readOnly = true)
    public PageResult<PostVO> userPosts(PostQuery query, Long userId) {
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<Post>().eq(Post::getAuthorId, userId)
                .eq(Post::getDeleted, 0).eq(query.getTopicId() != null, Post::getTopicId, query.getTopicId())
                .eq(query.getBoardId() != null, Post::getBoardId, query.getBoardId())
                .eq(query.getStatus() != null && !query.getStatus().isBlank(), Post::getStatus, query.getStatus())
                .orderByDesc(Post::getId);
        return postResult(postMapper.selectPage(page(query), wrapper), query, false);
    }

    @Transactional(readOnly = true)
    public PostVO userPost(Long id, Long userId) {
        Post post = ownedPost(id, userId);
        return postVO(post, postVersion(id, post.getLatestVersionNo()), false);
    }

    @Transactional
    public Long createPost(Long topicId, PostRequest request, Long userId) {
        Topic topic = requiredTopic(topicId);
        if (!isPublic(topic)) throw new ServiceException(404, "主题不存在或未公开");
        if (topic.getLocked() == 1) throw new ServiceException(409, "主题已锁定，不能回复");
        Board board = enabledBoard(topic.getBoardId());
        requireUserPermission(board.getReplyPermission(), "该板块不允许普通用户回复");
        return createPostInternal(topic, request, userId, true);
    }

    @Transactional
    public void updatePost(Long id, PostRequest request, Long userId) {
        Post post = ownedPost(id, userId);
        Topic topic = requiredTopic(post.getTopicId());
        if ("OFFLINE".equals(post.getStatus()) || "OFFLINE".equals(topic.getStatus())) {
            throw new ServiceException(409, "已下线跟帖不能编辑");
        }
        if (topic.getLocked() == 1) throw new ServiceException(409, "主题已锁定，不能编辑跟帖");
        updatePostInternal(post, request, userId);
    }

    @Transactional
    public void deletePost(Long id, Long userId) {
        Post post = ownedPost(id, userId);
        deletePostInternal(post, true);
    }

    @Transactional(readOnly = true)
    public PageResult<BoardVO> adminBoards(Query query) {
        LambdaQueryWrapper<Board> wrapper = new LambdaQueryWrapper<Board>()
                .like(query.getKeyword() != null && !query.getKeyword().isBlank(), Board::getName, cleanKeyword(query.getKeyword()))
                .orderByAsc(Board::getSectionId).orderByAsc(Board::getSortOrder).orderByDesc(Board::getId);
        Page<Board> page = boardMapper.selectPage(page(query), wrapper);
        return PageResult.of(page.getRecords().stream().map(this::boardVO).toList(), page.getTotal(), pageNo(query), pageSize(query));
    }

    @Transactional
    public Long createBoard(BoardRequest request, Long adminId) {
        requireGlobalAdmin(adminId);
        validateBoard(request, null);
        Board board = new Board();
        BeanUtils.copyProperties(request, board);
        board.setTopicCount(0L);
        boardMapper.insert(board);
        return board.getId();
    }

    @Transactional
    public void updateBoard(Long id, BoardRequest request, Long adminId) {
        requireGlobalAdmin(adminId);
        Board board = requiredBoard(id);
        validateBoard(request, id);
        BeanUtils.copyProperties(request, board);
        boardMapper.updateById(board);
    }

    @Transactional
    public void deleteBoard(Long id, Long adminId) {
        requireGlobalAdmin(adminId);
        requiredBoard(id);
        if (boardMapper.selectCount(new LambdaQueryWrapper<Board>().eq(Board::getParentId, id)) > 0
                || topicMapper.selectCount(new LambdaQueryWrapper<Topic>().eq(Topic::getBoardId, id)) > 0
                || postMapper.selectCount(new LambdaQueryWrapper<Post>().eq(Post::getBoardId, id)) > 0) {
            throw new ServiceException(409, "板块下仍有子板块、主题或跟帖，不能删除");
        }
        moderatorMapper.delete(new LambdaQueryWrapper<Moderator>().eq(Moderator::getBoardId, id));
        boardMapper.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<ModeratorVO> moderators(Long boardId, Long adminId) {
        requireGlobalAdmin(adminId);
        return moderatorMapper.selectList(new LambdaQueryWrapper<Moderator>()
                        .eq(boardId != null, Moderator::getBoardId, boardId).orderByDesc(Moderator::getId).last("LIMIT 100"))
                .stream().map(this::moderatorVO).toList();
    }

    @Transactional
    public Long createModerator(ModeratorRequest request, Long operatorId) {
        requireGlobalAdmin(operatorId);
        requiredBoard(request.getBoardId());
        if (adminMapper.selectById(request.getAdminId()) == null) throw new ServiceException(400, "管理员不存在");
        Moderator moderator = new Moderator();
        BeanUtils.copyProperties(request, moderator);
        try {
            moderatorMapper.insert(moderator);
        } catch (DuplicateKeyException e) {
            throw new ServiceException(409, "该管理员已关联此板块");
        }
        return moderator.getId();
    }

    @Transactional
    public void updateModerator(Long id, ModeratorRequest request, Long operatorId) {
        requireGlobalAdmin(operatorId);
        Moderator moderator = moderatorMapper.selectById(id);
        if (moderator == null) throw new ServiceException(404, "版主关联不存在");
        requiredBoard(request.getBoardId());
        if (adminMapper.selectById(request.getAdminId()) == null) throw new ServiceException(400, "管理员不存在");
        BeanUtils.copyProperties(request, moderator);
        try {
            moderatorMapper.updateById(moderator);
        } catch (DuplicateKeyException e) {
            throw new ServiceException(409, "该管理员已关联此板块");
        }
    }

    @Transactional
    public void deleteModerator(Long id, Long operatorId) {
        requireGlobalAdmin(operatorId);
        if (moderatorMapper.deleteById(id) == 0) throw new ServiceException(404, "版主关联不存在");
    }

    @Transactional(readOnly = true)
    public PageResult<TopicVO> adminTopics(Query query, Long adminId) {
        LambdaQueryWrapper<Topic> wrapper = new LambdaQueryWrapper<Topic>().eq(Topic::getDeleted, 0)
                .eq(query.getStatus() != null && !query.getStatus().isBlank(), Topic::getStatus, query.getStatus())
                .orderByDesc(Topic::getId);
        scopeTopics(wrapper, query.getBoardId(), adminId);
        applyKeyword(wrapper, query.getKeyword(), true);
        return topicResult(topicMapper.selectPage(page(query), wrapper), query, false);
    }

    @Transactional(readOnly = true)
    public TopicVO adminTopic(Long id, Long adminId) {
        Topic topic = requiredTopic(id);
        requireBoardAccess(topic.getBoardId(), adminId);
        return topicVO(topic, topicVersion(id, topic.getLatestVersionNo()), false);
    }

    @Transactional
    public Long createTopicForAdmin(TopicRequest request, Long adminId) {
        if (request.getAuthorId() == null) throw new ServiceException(400, "后台创建主题必须指定作者");
        requireActiveUser(request.getAuthorId());
        Board board = enabledBoard(request.getBoardId());
        requireAdminPermission(board, board.getPostPermission(), adminId);
        return createTopicInternal(request, request.getAuthorId());
    }

    @Transactional
    public void updateTopicForAdmin(Long id, TopicRequest request, Long adminId) {
        Topic topic = requiredTopic(id);
        requireBoardAccess(topic.getBoardId(), adminId);
        if (!topic.getBoardId().equals(request.getBoardId())) throw new ServiceException(400, "请使用移动操作变更板块");
        updateTopicInternal(topic, request, topic.getAuthorId());
    }

    @Transactional
    public void deleteTopicForAdmin(Long id, Long adminId) {
        Topic topic = requiredTopic(id);
        requireBoardAccess(topic.getBoardId(), adminId);
        deleteTopicInternal(topic, false);
    }

    @Transactional
    public void topicStatus(Long id, StatusRequest request, Long adminId) {
        Topic topic = requiredTopic(id);
        requireBoardAccess(topic.getBoardId(), adminId);
        if ("PUBLISHED".equals(request.getStatus())) requireApprovedTopicVersion(topic);
        boolean wasPublic = isPublic(topic);
        topicMapper.update(null, new LambdaUpdateWrapper<Topic>().eq(Topic::getId, id).eq(Topic::getDeleted, 0)
                .set(Topic::getStatus, request.getStatus()).set(Topic::getAuditReason, request.getReason()));
        adjustTopicPublicCount(topic, wasPublic, "PUBLISHED".equals(request.getStatus()));
        eventPublisher.publishEvent(new SearchIndexRefreshEvent("FORUM_TOPIC", id));
    }

    @Transactional
    public void moderateTopic(Long id, ModerationRequest request, Long adminId) {
        Topic topic = requiredTopic(id);
        requireBoardAccess(topic.getBoardId(), adminId);
        Long targetBoardId = request.getBoardId() == null ? topic.getBoardId() : request.getBoardId();
        if (!targetBoardId.equals(topic.getBoardId())) {
            enabledBoard(targetBoardId);
            requireBoardAccess(targetBoardId, adminId);
        }
        boolean wasPublic = isPublic(topic);
        LambdaUpdateWrapper<Topic> update = new LambdaUpdateWrapper<Topic>().eq(Topic::getId, id).eq(Topic::getDeleted, 0)
                .set(request.getLocked() != null, Topic::getLocked, request.getLocked())
                .set(request.getTopped() != null, Topic::getTopped, request.getTopped())
                .set(request.getEssence() != null, Topic::getEssence, request.getEssence())
                .set(Topic::getBoardId, targetBoardId);
        topicMapper.update(null, update);
        if (!targetBoardId.equals(topic.getBoardId()) && wasPublic) {
            changeBoardTopicCount(topic.getBoardId(), -1);
            changeBoardTopicCount(targetBoardId, 1);
            postMapper.update(null, new LambdaUpdateWrapper<Post>().eq(Post::getTopicId, id).set(Post::getBoardId, targetBoardId));
        }
    }

    @Transactional(readOnly = true)
    public PageResult<PostVO> adminPosts(PostQuery query, Long adminId) {
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<Post>().eq(Post::getDeleted, 0)
                .eq(query.getTopicId() != null, Post::getTopicId, query.getTopicId())
                .eq(query.getStatus() != null && !query.getStatus().isBlank(), Post::getStatus, query.getStatus())
                .orderByDesc(Post::getId);
        scopePosts(wrapper, query.getBoardId(), adminId);
        return postResult(postMapper.selectPage(page(query), wrapper), query, false);
    }

    @Transactional(readOnly = true)
    public PostVO adminPost(Long id, Long adminId) {
        Post post = requiredPost(id);
        requireBoardAccess(post.getBoardId(), adminId);
        return postVO(post, postVersion(id, post.getLatestVersionNo()), false);
    }

    @Transactional
    public Long createPostForAdmin(PostRequest request, Long adminId) {
        if (request.getTopicId() == null || request.getAuthorId() == null) {
            throw new ServiceException(400, "后台创建跟帖必须指定主题和作者");
        }
        requireActiveUser(request.getAuthorId());
        Topic topic = requiredTopic(request.getTopicId());
        Board board = enabledBoard(topic.getBoardId());
        requireAdminPermission(board, board.getReplyPermission(), adminId);
        return createPostInternal(topic, request, request.getAuthorId(), false);
    }

    @Transactional
    public void updatePostForAdmin(Long id, PostRequest request, Long adminId) {
        Post post = requiredPost(id);
        requireBoardAccess(post.getBoardId(), adminId);
        if (request.getTopicId() != null && !post.getTopicId().equals(request.getTopicId())) {
            throw new ServiceException(400, "编辑跟帖时不能变更主题");
        }
        updatePostInternal(post, request, post.getAuthorId());
    }

    @Transactional
    public void deletePostForAdmin(Long id, Long adminId) {
        Post post = requiredPost(id);
        requireBoardAccess(post.getBoardId(), adminId);
        deletePostInternal(post, false);
    }

    @Transactional
    public void postStatus(Long id, StatusRequest request, Long adminId) {
        Post post = requiredPost(id);
        requireBoardAccess(post.getBoardId(), adminId);
        if ("PUBLISHED".equals(request.getStatus())) requireApprovedPostVersion(post);
        boolean wasPublic = isPublic(post);
        postMapper.update(null, new LambdaUpdateWrapper<Post>().eq(Post::getId, id).eq(Post::getDeleted, 0)
                .set(Post::getStatus, request.getStatus()).set(Post::getAuditReason, request.getReason()));
        adjustPostPublicCount(post, wasPublic, "PUBLISHED".equals(request.getStatus()));
    }

    private Long createTopicInternal(TopicRequest request, Long authorId) {
        List<Long> images = images(request.getImageFileIds());
        fileService.requireOwnedImages(images, authorId);
        Topic topic = new Topic();
        topic.setBoardId(request.getBoardId());
        topic.setAuthorId(authorId);
        topic.setStatus("PENDING");
        topic.setLatestVersionNo(1);
        topic.setLocked(0);
        topic.setTopped(0);
        topic.setEssence(0);
        topic.setPostCount(0L);
        topic.setCommentCount(0L);
        topic.setLikeCount(0L);
        topic.setFavoriteCount(0L);
        topic.setViewCount(0L);
        topic.setDeleted(0);
        topicMapper.insert(topic);
        TopicVersion version = new TopicVersion();
        version.setTopicId(topic.getId());
        version.setVersionNo(1);
        version.setTitle(request.getTitle());
        version.setContent(request.getContent());
        version.setContentFileIds(JSON.writeValueAsString(images));
        version.setStatus("PENDING");
        topicVersionMapper.insert(version);
        fileService.bindReferences(images, TOPIC_AUDIT_BIZ_TYPE, topic.getId(), 1);
        auditService.submit(TOPIC_AUDIT_BIZ_TYPE, topic.getId(), 1, authorId,
                request.getTitle() + "\n" + request.getContent(), images);
        return topic.getId();
    }

    private void updateTopicInternal(Topic topic, TopicRequest request, Long authorId) {
        List<Long> images = images(request.getImageFileIds());
        fileService.requireOwnedImages(images, authorId);
        int nextVersion = topic.getLatestVersionNo() + 1;
        TopicVersion version = new TopicVersion();
        version.setTopicId(topic.getId());
        version.setVersionNo(nextVersion);
        version.setTitle(request.getTitle());
        version.setContent(request.getContent());
        version.setContentFileIds(JSON.writeValueAsString(images));
        version.setStatus("PENDING");
        topicVersionMapper.insert(version);
        int updated = topicMapper.update(null, new LambdaUpdateWrapper<Topic>().eq(Topic::getId, topic.getId())
                .eq(Topic::getLatestVersionNo, topic.getLatestVersionNo()).eq(Topic::getDeleted, 0).ne(Topic::getStatus, "OFFLINE")
                .set(Topic::getLatestVersionNo, nextVersion).set(Topic::getStatus, "PENDING").set(Topic::getAuditReason, null));
        if (updated == 0) throw new ServiceException(409, "主题已被并发修改，请刷新后重试");
        fileService.bindReferences(images, TOPIC_AUDIT_BIZ_TYPE, topic.getId(), nextVersion);
        auditService.submit(TOPIC_AUDIT_BIZ_TYPE, topic.getId(), nextVersion, authorId,
                request.getTitle() + "\n" + request.getContent(), images);
    }

    private Long createPostInternal(Topic topic, PostRequest request, Long authorId, boolean frontUser) {
        List<Long> images = images(request.getImageFileIds());
        fileService.requireOwnedImages(images, authorId);
        Long rootId = null;
        if (request.getParentId() != null) {
            Post parent = requiredPost(request.getParentId());
            requireSameTopic(parent, topic.getId());
            if (frontUser && !isPublic(parent) && !parent.getAuthorId().equals(authorId)) {
                throw new ServiceException(404, "父跟帖不存在或未公开");
            }
            rootId = parent.getRootId() == null ? parent.getId() : parent.getRootId();
        } else if (request.getReplyToId() != null) {
            throw new ServiceException(400, "指定被回复跟帖时必须同时指定父跟帖");
        }
        if (request.getReplyToId() != null) {
            Post replyTo = requiredPost(request.getReplyToId());
            requireSameTopic(replyTo, topic.getId());
            Long replyRoot = replyTo.getRootId() == null ? replyTo.getId() : replyTo.getRootId();
            if (!Objects.equals(rootId, replyRoot)) throw new ServiceException(400, "被回复跟帖与父跟帖不属于同一楼中楼");
            if (frontUser && !isPublic(replyTo) && !replyTo.getAuthorId().equals(authorId)) {
                throw new ServiceException(404, "被回复跟帖不存在或未公开");
            }
        }
        Post post = new Post();
        post.setTopicId(topic.getId());
        post.setBoardId(topic.getBoardId());
        post.setAuthorId(authorId);
        post.setRootId(rootId);
        post.setParentId(request.getParentId());
        post.setReplyToId(request.getReplyToId() == null ? request.getParentId() : request.getReplyToId());
        post.setStatus("PENDING");
        post.setLatestVersionNo(1);
        post.setLikeCount(0L);
        post.setDeleted(0);
        insertWithFloor(post);
        PostVersion version = new PostVersion();
        version.setPostId(post.getId());
        version.setVersionNo(1);
        version.setContent(request.getContent());
        version.setContentFileIds(JSON.writeValueAsString(images));
        version.setStatus("PENDING");
        postVersionMapper.insert(version);
        fileService.bindReferences(images, POST_AUDIT_BIZ_TYPE, post.getId(), 1);
        auditService.submit(POST_AUDIT_BIZ_TYPE, post.getId(), 1, authorId, request.getContent(), images);
        return post.getId();
    }

    private void updatePostInternal(Post post, PostRequest request, Long authorId) {
        List<Long> images = images(request.getImageFileIds());
        fileService.requireOwnedImages(images, authorId);
        int nextVersion = post.getLatestVersionNo() + 1;
        PostVersion version = new PostVersion();
        version.setPostId(post.getId());
        version.setVersionNo(nextVersion);
        version.setContent(request.getContent());
        version.setContentFileIds(JSON.writeValueAsString(images));
        version.setStatus("PENDING");
        postVersionMapper.insert(version);
        int updated = postMapper.update(null, new LambdaUpdateWrapper<Post>().eq(Post::getId, post.getId())
                .eq(Post::getLatestVersionNo, post.getLatestVersionNo()).eq(Post::getDeleted, 0).ne(Post::getStatus, "OFFLINE")
                .set(Post::getLatestVersionNo, nextVersion).set(Post::getStatus, "PENDING").set(Post::getAuditReason, null));
        if (updated == 0) throw new ServiceException(409, "跟帖已被并发修改，请刷新后重试");
        fileService.bindReferences(images, POST_AUDIT_BIZ_TYPE, post.getId(), nextVersion);
        auditService.submit(POST_AUDIT_BIZ_TYPE, post.getId(), nextVersion, authorId, request.getContent(), images);
    }

    private void insertWithFloor(Post post) {
        for (int attempt = 0; attempt < 3; attempt++) {
            post.setFloorNo(nextFloor(post.getTopicId()));
            try {
                postMapper.insert(post);
                return;
            } catch (DuplicateKeyException e) {
                if (attempt == 2) throw new ServiceException(409, "楼层分配冲突，请重试");
                post.setId(null);
            }
        }
    }

    private int nextFloor(Long topicId) {
        List<Object> values = postMapper.selectObjs(new QueryWrapper<Post>()
                .select("COALESCE(MAX(floor_no), 0) + 1").eq("topic_id", topicId));
        if (values.isEmpty() || values.get(0) == null) throw new ServiceException(500, "无法分配楼层");
        return ((Number) values.get(0)).intValue();
    }

    private void deleteTopicInternal(Topic topic, boolean owned) {
        boolean wasPublic = isPublic(topic);
        LambdaUpdateWrapper<Topic> update = new LambdaUpdateWrapper<Topic>().eq(Topic::getId, topic.getId())
                .eq(Topic::getDeleted, 0).eq(owned, Topic::getAuthorId, topic.getAuthorId()).set(Topic::getDeleted, 1);
        if (topicMapper.update(null, update) == 0) throw new ServiceException(409, "主题状态已变化");
        if (wasPublic) changeBoardTopicCount(topic.getBoardId(), -1);
        eventPublisher.publishEvent(new SearchIndexRefreshEvent("FORUM_TOPIC", topic.getId()));
    }

    private void deletePostInternal(Post post, boolean owned) {
        boolean wasPublic = isPublic(post);
        LambdaUpdateWrapper<Post> update = new LambdaUpdateWrapper<Post>().eq(Post::getId, post.getId())
                .eq(Post::getDeleted, 0).eq(owned, Post::getAuthorId, post.getAuthorId()).set(Post::getDeleted, 1);
        if (postMapper.update(null, update) == 0) throw new ServiceException(409, "跟帖状态已变化");
        if (wasPublic) changeTopicPostCount(post.getTopicId(), -1);
    }

    private void adjustTopicPublicCount(Topic topic, boolean wasPublic, boolean published) {
        boolean nowPublic = published && topic.getPublishedVersionNo() != null;
        if (wasPublic && !nowPublic) changeBoardTopicCount(topic.getBoardId(), -1);
        if (!wasPublic && nowPublic) changeBoardTopicCount(topic.getBoardId(), 1);
    }

    private void adjustPostPublicCount(Post post, boolean wasPublic, boolean published) {
        boolean nowPublic = published && post.getPublishedVersionNo() != null;
        if (wasPublic && !nowPublic) changeTopicPostCount(post.getTopicId(), -1);
        if (!wasPublic && nowPublic) changeTopicPostCount(post.getTopicId(), 1);
    }

    private void requireApprovedTopicVersion(Topic topic) {
        if (topic.getPublishedVersionNo() == null) throw new ServiceException(409, "主题尚无审核通过的公开版本");
        if (!"APPROVED".equals(topicVersion(topic.getId(), topic.getPublishedVersionNo()).getStatus())) {
            throw new ServiceException(409, "公开版本未审核通过");
        }
    }

    private void requireApprovedPostVersion(Post post) {
        if (post.getPublishedVersionNo() == null) throw new ServiceException(409, "跟帖尚无审核通过的公开版本");
        if (!"APPROVED".equals(postVersion(post.getId(), post.getPublishedVersionNo()).getStatus())) {
            throw new ServiceException(409, "公开版本未审核通过");
        }
    }

    private void validateBoard(BoardRequest request, Long boardId) {
        Section section = sectionMapper.selectById(request.getSectionId());
        if (section == null) throw new ServiceException(400, "分区不存在");
        if (request.getParentId() == null) return;
        if (request.getParentId().equals(boardId)) throw new ServiceException(400, "父板块不能是自身");
        Board parent = requiredBoard(request.getParentId());
        if (!parent.getSectionId().equals(request.getSectionId())) throw new ServiceException(400, "父板块必须属于同一分区");
        Long cursor = parent.getParentId();
        while (cursor != null) {
            if (cursor.equals(boardId)) throw new ServiceException(400, "板块层级不能形成循环");
            Board ancestor = boardMapper.selectById(cursor);
            cursor = ancestor == null ? null : ancestor.getParentId();
        }
    }

    private void requireUserPermission(String permission, String message) {
        if (!"USER".equals(permission)) throw new ServiceException(403, message);
    }

    private void requireAdminPermission(Board board, String permission, Long adminId) {
        if ("ADMIN".equals(permission)) requireGlobalAdmin(adminId);
        else requireBoardAccess(board.getId(), adminId);
    }

    private void requireGlobalAdmin(Long adminId) {
        if (!isGlobalAdmin(adminId)) throw new ServiceException(403, "仅全局管理员可以管理板块和版主");
    }

    private boolean isGlobalAdmin(Long adminId) {
        return moderatorMapper.selectCount(new LambdaQueryWrapper<Moderator>().eq(Moderator::getAdminId, adminId)) == 0;
    }

    private void requireBoardAccess(Long boardId, Long adminId) {
        if (isGlobalAdmin(adminId)) return;
        Long allowed = moderatorMapper.selectCount(new LambdaQueryWrapper<Moderator>().eq(Moderator::getAdminId, adminId)
                .eq(Moderator::getBoardId, boardId).eq(Moderator::getStatus, 1));
        if (allowed == 0) throw new ServiceException(403, "版主无权管理其他板块内容");
    }

    private List<Long> scopedBoardIds(Long adminId) {
        return moderatorMapper.selectList(new LambdaQueryWrapper<Moderator>().eq(Moderator::getAdminId, adminId)
                .eq(Moderator::getStatus, 1)).stream().map(Moderator::getBoardId).toList();
    }

    private void scopeTopics(LambdaQueryWrapper<Topic> wrapper, Long requestedBoardId, Long adminId) {
        if (requestedBoardId != null) {
            requireBoardAccess(requestedBoardId, adminId);
            wrapper.eq(Topic::getBoardId, requestedBoardId);
        } else if (!isGlobalAdmin(adminId)) {
            List<Long> ids = scopedBoardIds(adminId);
            if (ids.isEmpty()) wrapper.eq(Topic::getBoardId, -1L);
            else wrapper.in(Topic::getBoardId, ids);
        }
    }

    private void scopePosts(LambdaQueryWrapper<Post> wrapper, Long requestedBoardId, Long adminId) {
        if (requestedBoardId != null) {
            requireBoardAccess(requestedBoardId, adminId);
            wrapper.eq(Post::getBoardId, requestedBoardId);
        } else if (!isGlobalAdmin(adminId)) {
            List<Long> ids = scopedBoardIds(adminId);
            if (ids.isEmpty()) wrapper.eq(Post::getBoardId, -1L);
            else wrapper.in(Post::getBoardId, ids);
        }
    }

    private void requireActiveUser(Long userId) {
        if (userMapper.selectCount(new LambdaQueryWrapper<UserAccount>().eq(UserAccount::getId, userId)
                .eq(UserAccount::getStatus, 1)) == 0) throw new ServiceException(400, "作者用户不存在或已停用");
    }

    private void requireSameTopic(Post post, Long topicId) {
        if (!post.getTopicId().equals(topicId)) throw new ServiceException(400, "父跟帖或被回复跟帖不属于当前主题");
    }

    private LambdaQueryWrapper<Topic> publicTopicWrapper(Long boardId) {
        return new LambdaQueryWrapper<Topic>().eq(boardId != null, Topic::getBoardId, boardId)
                .eq(Topic::getDeleted, 0).ne(Topic::getStatus, "OFFLINE").isNotNull(Topic::getPublishedVersionNo);
    }

    private LambdaQueryWrapper<Post> publicPostWrapper(Long topicId) {
        return new LambdaQueryWrapper<Post>().eq(Post::getTopicId, topicId).eq(Post::getDeleted, 0)
                .ne(Post::getStatus, "OFFLINE").isNotNull(Post::getPublishedVersionNo);
    }

    private void applyKeyword(LambdaQueryWrapper<Topic> wrapper, String keyword, boolean latest) {
        String cleaned = cleanKeyword(keyword);
        if (cleaned == null) return;
        String pointer = latest ? "latest_version_no" : "published_version_no";
        wrapper.apply("EXISTS (SELECT 1 FROM forum_topic_version v WHERE v.topic_id = forum_topic.id AND v.version_no = forum_topic."
                + pointer + " AND v.title LIKE {0})", "%" + cleaned + "%");
    }

    private Topic requiredTopic(Long id) {
        Topic topic = topicMapper.selectById(id);
        if (topic == null || topic.getDeleted() == 1) throw new ServiceException(404, "主题不存在");
        return topic;
    }

    private Post requiredPost(Long id) {
        Post post = postMapper.selectById(id);
        if (post == null || post.getDeleted() == 1) throw new ServiceException(404, "跟帖不存在");
        return post;
    }

    private Topic ownedTopic(Long id, Long userId) {
        Topic topic = requiredTopic(id);
        if (!topic.getAuthorId().equals(userId)) throw new ServiceException(403, "无权操作他人的主题");
        return topic;
    }

    private Post ownedPost(Long id, Long userId) {
        Post post = requiredPost(id);
        if (!post.getAuthorId().equals(userId)) throw new ServiceException(403, "无权操作他人的跟帖");
        return post;
    }

    private Board enabledBoard(Long id) {
        Board board = requiredBoard(id);
        if (board.getStatus() != 1) throw new ServiceException(409, "板块已停用");
        return board;
    }

    private Board requiredBoard(Long id) {
        Board board = boardMapper.selectById(id);
        if (board == null) throw new ServiceException(404, "板块不存在");
        return board;
    }

    private TopicVersion topicVersion(Long topicId, Integer versionNo) {
        TopicVersion version = topicVersionMapper.selectOne(new LambdaQueryWrapper<TopicVersion>()
                .eq(TopicVersion::getTopicId, topicId).eq(TopicVersion::getVersionNo, versionNo));
        if (version == null) throw new ServiceException(500, "主题版本指针异常");
        return version;
    }

    private PostVersion postVersion(Long postId, Integer versionNo) {
        PostVersion version = postVersionMapper.selectOne(new LambdaQueryWrapper<PostVersion>()
                .eq(PostVersion::getPostId, postId).eq(PostVersion::getVersionNo, versionNo));
        if (version == null) throw new ServiceException(500, "跟帖版本指针异常");
        return version;
    }

    private boolean isPublic(Topic topic) {
        return topic.getDeleted() == 0 && !"OFFLINE".equals(topic.getStatus()) && topic.getPublishedVersionNo() != null;
    }

    private boolean isPublic(Post post) {
        return post.getDeleted() == 0 && !"OFFLINE".equals(post.getStatus()) && post.getPublishedVersionNo() != null;
    }

    private void changeBoardTopicCount(Long boardId, int delta) {
        boardMapper.update(null, new LambdaUpdateWrapper<Board>().eq(Board::getId, boardId)
                .setSql("topic_count = GREATEST(0, topic_count + " + delta + ")"));
    }

    private void changeTopicPostCount(Long topicId, int delta) {
        topicMapper.update(null, new LambdaUpdateWrapper<Topic>().eq(Topic::getId, topicId)
                .setSql("post_count = GREATEST(0, post_count + " + delta + ")"));
    }

    private TopicVO topicVO(Topic topic, TopicVersion version, boolean publicView) {
        TopicVO vo = new TopicVO();
        BeanUtils.copyProperties(topic, vo);
        vo.setTitle(version.getTitle());
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

    private PostVO postVO(Post post, PostVersion version, boolean publicView) {
        PostVO vo = new PostVO();
        BeanUtils.copyProperties(post, vo);
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

    private BoardVO boardVO(Board board) {
        BoardVO vo = new BoardVO();
        BeanUtils.copyProperties(board, vo);
        return vo;
    }

    private ModeratorVO moderatorVO(Moderator moderator) {
        ModeratorVO vo = new ModeratorVO();
        BeanUtils.copyProperties(moderator, vo);
        return vo;
    }

    private PageResult<TopicVO> topicResult(Page<Topic> page, Query query, boolean publicView) {
        return PageResult.of(page.getRecords().stream()
                .map(topic -> topicVO(topic, topicVersion(topic.getId(), publicView ? topic.getPublishedVersionNo() : topic.getLatestVersionNo()), publicView))
                .toList(), page.getTotal(), pageNo(query), pageSize(query));
    }

    private PageResult<PostVO> postResult(Page<Post> page, Query query, boolean publicView) {
        return PageResult.of(page.getRecords().stream()
                .map(post -> postVO(post, postVersion(post.getId(), publicView ? post.getPublishedVersionNo() : post.getLatestVersionNo()), publicView))
                .toList(), page.getTotal(), pageNo(query), pageSize(query));
    }

    private List<Long> images(List<Long> source) { return source == null ? List.of() : source.stream().distinct().toList(); }
    private String cleanKeyword(String keyword) { return keyword == null || keyword.isBlank() ? null : keyword.trim(); }
    private long pageNo(Query query) { return Math.max(1, query.getPage()); }
    private long pageSize(Query query) { return Math.min(100, Math.max(1, query.getSize())); }
    private <T> Page<T> page(Query query) { return new Page<>(pageNo(query), pageSize(query)); }
}
