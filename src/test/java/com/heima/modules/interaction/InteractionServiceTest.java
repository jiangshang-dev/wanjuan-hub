package com.heima.modules.interaction;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.heima.exception.ServiceException;
import com.heima.modules.auth.entity.UserProfile;
import com.heima.modules.auth.mapper.UserAccountMapper;
import com.heima.modules.auth.mapper.UserProfileMapper;
import com.heima.modules.blog.entity.BlogArticle;
import com.heima.modules.blog.mapper.BlogArticleMapper;
import com.heima.modules.comment.mapper.CommentMappers.CommentMapper;
import com.heima.modules.comment.service.ContentTargetService;
import com.heima.modules.forum.mapper.ForumMappers.PostMapper;
import com.heima.modules.forum.mapper.ForumMappers.TopicMapper;
import com.heima.modules.interaction.dto.InteractionDtos.TargetRequest;
import com.heima.modules.interaction.entity.InteractionEntities.Like;
import com.heima.modules.interaction.mapper.InteractionMappers.FavoriteMapper;
import com.heima.modules.interaction.mapper.InteractionMappers.FollowMapper;
import com.heima.modules.interaction.mapper.InteractionMappers.LikeMapper;
import com.heima.modules.interaction.service.InteractionService;
import com.heima.modules.message.event.UserNotificationEvent;
import com.heima.modules.operation.service.SiteExperienceService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InteractionServiceTest {
    @Mock private LikeMapper likeMapper;
    @Mock private FavoriteMapper favoriteMapper;
    @Mock private FollowMapper followMapper;
    @Mock private UserAccountMapper userMapper;
    @Mock private UserProfileMapper profileMapper;
    @Mock private ContentTargetService targetService;
    @Mock private BlogArticleMapper blogMapper;
    @Mock private TopicMapper topicMapper;
    @Mock private PostMapper postMapper;
    @Mock private CommentMapper commentMapper;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private SiteExperienceService siteExperienceService;
    @InjectMocks private InteractionService interactionService;

    @BeforeAll
    static void initTableMetadata() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Like.class);
        TableInfoHelper.initTableInfo(assistant, BlogArticle.class);
    }

    @Test
    void duplicateLikeDoesNotIncrementTargetCounter() {
        TargetRequest request = new TargetRequest();
        request.setTargetType("BLOG");
        request.setTargetId(10L);
        when(likeMapper.insert(any(Like.class))).thenThrow(new DuplicateKeyException("duplicate"));

        ServiceException error = assertThrows(ServiceException.class, () -> interactionService.like(request, 1L));

        assertEquals(409, error.getCode());
        verify(blogMapper, never()).update(isNull(), any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void likeNotifiesTargetAuthorAfterSuccessfulInsert() {
        TargetRequest request = new TargetRequest();
        request.setTargetType("BLOG");
        request.setTargetId(10L);
        when(targetService.targetAuthorId("BLOG", 10L)).thenReturn(2L);
        when(blogMapper.update(isNull(), any())).thenReturn(1);

        interactionService.like(request, 1L);

        verify(blogMapper).update(isNull(), any());
        verify(eventPublisher).publishEvent(any(UserNotificationEvent.class));
    }

    @Test
    void likeDoesNotNotifyAuthorAboutOwnContent() {
        TargetRequest request = new TargetRequest();
        request.setTargetType("BLOG");
        request.setTargetId(10L);
        when(targetService.targetAuthorId("BLOG", 10L)).thenReturn(1L);
        when(blogMapper.update(isNull(), any())).thenReturn(1);

        interactionService.like(request, 1L);

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void switchingReactionUpdatesBothCounters() {
        TargetRequest request = new TargetRequest();
        request.setTargetType("BLOG");
        request.setTargetId(10L);
        request.setReactionType("DISLIKE");
        Like existing = new Like();
        existing.setId(99L);
        existing.setReactionType("LIKE");
        when(likeMapper.selectOne(any())).thenReturn(existing).thenReturn(null);
        when(likeMapper.update(isNull(), any())).thenReturn(1);
        when(blogMapper.update(isNull(), any())).thenReturn(1);
        when(targetService.targetAuthorId("BLOG", 10L)).thenReturn(2L);

        interactionService.react(request, 1L);

        verify(likeMapper).update(isNull(), any());
        verify(blogMapper, times(2)).update(isNull(), any());
        verify(eventPublisher).publishEvent(any(UserNotificationEvent.class));
    }

    @Test
    void cancelReactionDeletesRowAndDecrementsCounter() {
        TargetRequest request = new TargetRequest();
        request.setTargetType("BLOG");
        request.setTargetId(10L);
        Like existing = new Like();
        existing.setId(99L);
        existing.setReactionType("LIKE");
        when(likeMapper.selectOne(any())).thenReturn(existing).thenReturn(null);
        when(likeMapper.delete(any())).thenReturn(1);
        when(blogMapper.update(isNull(), any())).thenReturn(1);

        interactionService.unreact(request, 1L);

        verify(likeMapper).delete(any());
        verify(blogMapper).update(isNull(), any());
    }

    @Test
    void grayDayAlsoBlocksCancellation() {
        TargetRequest request = new TargetRequest();
        request.setTargetType("BLOG");
        request.setTargetId(10L);
        when(siteExperienceService.isGrayDay()).thenReturn(true);
        when(siteExperienceService.grayReason()).thenReturn("特殊纪念日");

        ServiceException error = assertThrows(ServiceException.class, () -> interactionService.unreact(request, 1L));

        assertEquals(409, error.getCode());
        verify(likeMapper, never()).delete(any());
    }

    @Test
    void authorCanDisableDislikesFromOtherUsers() {
        TargetRequest request = new TargetRequest();
        request.setTargetType("BLOG");
        request.setTargetId(10L);
        request.setReactionType("DISLIKE");
        UserProfile profile = new UserProfile();
        profile.setAllowDislike(false);
        when(targetService.targetAuthorId("BLOG", 10L)).thenReturn(2L);
        when(profileMapper.selectById(2L)).thenReturn(profile);

        ServiceException error = assertThrows(ServiceException.class, () -> interactionService.react(request, 1L));

        assertEquals(403, error.getCode());
        verify(likeMapper, never()).insert(any(Like.class));
    }
}
