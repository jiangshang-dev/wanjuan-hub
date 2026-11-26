package com.heima.modules.search;

import com.heima.exception.ServiceException;
import com.heima.infrastructure.rabbitmq.SearchRabbitConstants;
import com.heima.modules.search.event.SearchIndexMessage;
import com.heima.modules.search.listener.SearchMessageListener;
import com.heima.modules.search.service.SearchIndexService;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class SearchMessageListenerTest {
    private static final String PAYLOAD = "{\"messageId\":\"m1\",\"contentType\":\"BLOG\",\"contentId\":8,\"eventVersion\":19}";
    @Mock private SearchIndexService searchIndexService;
    @Mock private RabbitTemplate rabbitTemplate;
    @Mock private Channel channel;
    @InjectMocks private SearchMessageListener listener;

    @Test
    void successfulConsumptionUsesManualAck() throws Exception {
        Message message = message();

        listener.onMessage(PAYLOAD, message, channel, 11L);

        verify(searchIndexService).consume(any(SearchIndexMessage.class));
        verify(channel).basicAck(11L, false);
        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    void firstFailureIsRepublishedToRetryQueueAndAcknowledged() throws Exception {
        Message message = message();
        doThrow(new ServiceException(503, "ES unavailable")).when(searchIndexService).consume(any());

        listener.onMessage(PAYLOAD, message, channel, 12L);

        verify(rabbitTemplate).send(eq(SearchRabbitConstants.RETRY_EXCHANGE),
                eq(SearchRabbitConstants.RETRY_ROUTING_KEY), same(message));
        verify(channel).basicAck(12L, false);
        org.junit.jupiter.api.Assertions.assertEquals(1,
                ((Number) message.getMessageProperties().getHeader("x-retry-count")).intValue());
    }

    @Test
    void thirdFailureIsRepublishedToDeadLetterQueue() throws Exception {
        Message message = message();
        message.getMessageProperties().setHeader("x-retry-count", 2);
        doThrow(new ServiceException(503, "ES unavailable")).when(searchIndexService).consume(any());

        listener.onMessage(PAYLOAD, message, channel, 13L);

        verify(rabbitTemplate).send(eq(SearchRabbitConstants.DEAD_EXCHANGE),
                eq(SearchRabbitConstants.DEAD_ROUTING_KEY), same(message));
        verify(channel).basicAck(13L, false);
        org.junit.jupiter.api.Assertions.assertEquals(3,
                ((Number) message.getMessageProperties().getHeader("x-retry-count")).intValue());
    }

    private Message message() {
        return new Message(PAYLOAD.getBytes(), new MessageProperties());
    }
}
