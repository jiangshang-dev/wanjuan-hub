package com.heima.modules.search;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.heima.modules.search.entity.SearchOutbox;
import com.heima.modules.search.mapper.SearchOutboxMapper;
import com.heima.modules.search.service.SearchMessageSender;
import com.heima.modules.search.service.impl.SearchOutboxServiceImpl;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchOutboxServiceImplTest {
    @Mock private SearchOutboxMapper outboxMapper;
    @Mock private SearchMessageSender messageSender;
    @InjectMocks private SearchOutboxServiceImpl outboxService;

    @BeforeAll
    static void initTableMetadata() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, SearchOutbox.class);
    }

    @Test
    void databaseIdBecomesMonotonicElasticsearchExternalVersion() {
        when(outboxMapper.insert(any(SearchOutbox.class))).thenAnswer(invocation -> {
            SearchOutbox outbox = invocation.getArgument(0);
            outbox.setId(27L);
            return 1;
        });

        SearchOutbox outbox = outboxService.record("blog", 8L);

        assertEquals(27L, outbox.getEventVersion());
        assertEquals("PENDING", outbox.getStatus());
        assertEquals("BLOG", outbox.getContentType());
        assertNotNull(outbox.getMessageId());
        assertTrue(outbox.getPayload().contains("\"eventVersion\":27"));
        assertTrue(outbox.getPayload().contains("\"contentType\":\"BLOG\""));
        verify(outboxMapper).update(isNull(), any());
    }
}
