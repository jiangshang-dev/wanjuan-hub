package com.heima.modules.search;

import com.heima.common.PageResult;
import com.heima.exception.ServiceException;
import com.heima.modules.search.dto.SearchDtos.SearchResultVO;
import com.heima.modules.search.dto.SearchIndexDocument;
import com.heima.modules.search.service.SearchIndexRepository;
import com.heima.modules.search.service.SearchIndexRepository.SearchHit;
import com.heima.modules.search.service.SearchIndexRepository.SearchPage;
import com.heima.modules.search.service.impl.SearchServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchServiceImplTest {
    @Mock private SearchIndexRepository indexRepository;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private JdbcTemplate jdbcTemplate;
    @InjectMocks private SearchServiceImpl searchService;

    @Test
    void elasticsearchHighlightsAreEscapedExceptMarkTags() {
        SearchIndexDocument document = new SearchIndexDocument(
                "BLOG", 7L, "原始标题", "原始摘要", "正文", "/blogs/7");
        Map<String, List<String>> highlights = Map.of(
                "title", List.of("<mark>标题</mark><script>alert(1)</script>"),
                "summary", List.of("安全<mark>中文</mark><img src=x onerror=alert(1)>"));
        when(indexRepository.search(anyString(), any(), anyInt(), anyInt()))
                .thenReturn(new SearchPage(1, List.of(new SearchHit(document, highlights))));

        PageResult<SearchResultVO> result = searchService.search("中文", "BLOG", 1, 20, null);

        assertEquals(1, result.getTotal());
        SearchResultVO record = result.getRecords().getFirst();
        assertEquals("<mark>标题</mark>&lt;script&gt;alert(1)&lt;/script&gt;", record.getTitle());
        assertTrue(record.getHighlight().contains("安全<mark>中文</mark>"));
        assertFalse(record.getHighlight().contains("<img"));
        assertTrue(record.getHighlight().contains("&lt;img"));
        verify(jdbcTemplate).update(contains("search_hot_keyword"), eq("中文"));
        verifyNoInteractions(redisTemplate);
    }

    @Test
    void elasticsearchFailureReturns503WithoutMysqlFallback() {
        ServiceException unavailable = new ServiceException(503, "搜索服务暂不可用，请稍后重试");
        when(indexRepository.search(anyString(), any(), anyInt(), anyInt())).thenThrow(unavailable);

        ServiceException actual = assertThrows(ServiceException.class,
                () -> searchService.search("中文", null, 1, 20, null));

        assertSame(unavailable, actual);
        assertEquals(503, actual.getCode());
        verify(jdbcTemplate, never()).update(anyString(), anyString());
        verifyNoInteractions(redisTemplate);
    }

    @Test
    void resourceSearchIsRejectedBeforeCallingElasticsearch() {
        ServiceException error = assertThrows(ServiceException.class,
                () -> searchService.search("中文", "RESOURCE", 1, 20, null));

        assertEquals("不支持的搜索类型", error.getMessage());
        verifyNoInteractions(indexRepository, jdbcTemplate, redisTemplate);
    }
}
