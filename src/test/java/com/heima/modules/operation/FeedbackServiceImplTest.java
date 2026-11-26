package com.heima.modules.operation;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.heima.exception.ServiceException;
import com.heima.modules.message.event.UserNotificationEvent;
import com.heima.modules.operation.dto.FeedbackDtos.CreateRequest;
import com.heima.modules.operation.dto.FeedbackDtos.ReplyRequest;
import com.heima.modules.operation.entity.FeedbackEntity;
import com.heima.modules.operation.mapper.FeedbackMapper;
import com.heima.modules.operation.service.impl.FeedbackServiceImpl;
import com.heima.modules.operation.vo.FeedbackVO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeedbackServiceImplTest {
    @Mock private FeedbackMapper feedbackMapper;
    @Mock private ApplicationEventPublisher eventPublisher;
    @InjectMocks private FeedbackServiceImpl feedbackService;

    @BeforeAll
    static void initTableMetadata() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), FeedbackEntity.class);
    }

    @Test
    void createUsesCurrentUserAndPendingStatus() {
        CreateRequest request = new CreateRequest();
        request.setContent("  希望增加深色模式  ");
        doAnswer(invocation -> {
            invocation.getArgument(0, FeedbackEntity.class).setId(8L);
            return 1;
        }).when(feedbackMapper).insert(any(FeedbackEntity.class));

        String id = feedbackService.create(42L, request);

        ArgumentCaptor<FeedbackEntity> captor = ArgumentCaptor.forClass(FeedbackEntity.class);
        verify(feedbackMapper).insert(captor.capture());
        assertEquals("8", id);
        assertEquals(42L, captor.getValue().getUserId());
        assertEquals("希望增加深色模式", captor.getValue().getContent());
        assertEquals("PENDING", captor.getValue().getStatus());
    }

    @Test
    @SuppressWarnings("unchecked")
    void userDetailAlwaysQueriesByIdAndOwner() {
        FeedbackEntity owned = feedback(9L, 42L, "REPLIED");
        when(feedbackMapper.selectOne(any())).thenReturn(owned);

        FeedbackVO result = feedbackService.userDetail(42L, 9L);

        assertEquals("9", result.id());
        ArgumentCaptor<Wrapper<FeedbackEntity>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(feedbackMapper).selectOne(captor.capture());
        assertTrue(captor.getValue().getSqlSegment().contains("user_id"));
    }

    @Test
    void userDetailReturnsNotFoundForMissingOrForeignFeedback() {
        when(feedbackMapper.selectOne(any())).thenReturn(null);

        ServiceException error = assertThrows(ServiceException.class, () -> feedbackService.userDetail(42L, 9L));

        assertEquals(404, error.getCode());
    }

    @Test
    void replyUpdatesFeedbackAndNotifiesOriginalUser() {
        when(feedbackMapper.selectById(9L)).thenReturn(feedback(9L, 42L, "PENDING"));
        when(feedbackMapper.update(any(), any())).thenReturn(1);
        ReplyRequest request = new ReplyRequest();
        request.setReply("  已纳入后续版本计划  ");

        feedbackService.reply(9L, request, 7L);

        ArgumentCaptor<UserNotificationEvent> captor = ArgumentCaptor.forClass(UserNotificationEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        UserNotificationEvent event = captor.getValue();
        assertEquals(42L, event.userId());
        assertEquals("SYSTEM", event.type());
        assertEquals("OPERATION_FEEDBACK", event.bizType());
        assertEquals("9", event.bizId());
        assertEquals("已纳入后续版本计划", event.content());
    }

    @Test
    void closedFeedbackCannotBeReplied() {
        when(feedbackMapper.selectById(9L)).thenReturn(feedback(9L, 42L, "CLOSED"));
        ReplyRequest request = new ReplyRequest();
        request.setReply("回复");

        ServiceException error = assertThrows(ServiceException.class, () -> feedbackService.reply(9L, request, 7L));

        assertEquals(409, error.getCode());
        verify(feedbackMapper, never()).update(any(), any());
        verifyNoInteractions(eventPublisher);
    }

    private static FeedbackEntity feedback(Long id, Long userId, String status) {
        FeedbackEntity entity = new FeedbackEntity();
        entity.setId(id);
        entity.setUserId(userId);
        entity.setContent("建议");
        entity.setStatus(status);
        return entity;
    }
}
