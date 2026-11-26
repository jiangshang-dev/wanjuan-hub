package com.heima.modules.search.service.impl;

import com.heima.common.PageResult;
import com.heima.exception.ServiceException;
import com.heima.modules.search.dto.SearchDtos.SearchResultVO;
import com.heima.modules.search.dto.SearchIndexDocument;
import com.heima.modules.search.service.SearchIndexRepository;
import com.heima.modules.search.service.SearchIndexRepository.SearchHit;
import com.heima.modules.search.service.SearchIndexRepository.SearchPage;
import com.heima.modules.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {
    private static final String HISTORY_PREFIX = "wanjuan:search:history:";
    private static final Set<String> PUBLIC_TYPES = Set.of("BLOG", "FORUM_TOPIC");
    private static final int MAX_RESULT_WINDOW = 10_000;
    private final SearchIndexRepository indexRepository;
    private final StringRedisTemplate redisTemplate;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public PageResult<SearchResultVO> search(String keyword, String type, long page, long size, Long userId) {
        String normalized = normalizeKeyword(keyword);
        String normalizedType = normalizeType(type);
        long current = Math.max(1, page);
        long pageSize = Math.min(100, Math.max(1, size));
        if (normalized.isEmpty()) return PageResult.of(List.of(), 0, current, pageSize);
        long offset = (current - 1) * pageSize;
        if (offset < 0 || offset + pageSize > MAX_RESULT_WINDOW) {
            throw new ServiceException(400, "搜索分页不能超过10000条结果");
        }
        SearchPage result = indexRepository.search(normalized, normalizedType, (int) offset, (int) pageSize);
        recordKeyword(normalized, userId);
        List<SearchResultVO> records = result.hits().stream().map(this::toResult).toList();
        return PageResult.of(records, result.total(), current, pageSize);
    }

    @Override
    public List<String> hotKeywords(int limit) {
        return jdbcTemplate.queryForList("SELECT keyword FROM search_hot_keyword WHERE status=1 " +
                "ORDER BY sort_order ASC,heat DESC,id DESC LIMIT " + Math.min(100, Math.max(1, limit)), String.class);
    }

    @Override
    public List<String> history(Long userId, int limit) {
        if (userId == null) return List.of();
        try {
            Set<String> values = redisTemplate.opsForZSet().reverseRange(HISTORY_PREFIX + userId,
                    0, Math.min(100, Math.max(1, limit)) - 1L);
            return values == null ? List.of() : List.copyOf(values);
        } catch (DataAccessException ex) {
            log.warn("Search history is temporarily unavailable, userId={}", userId);
            return List.of();
        }
    }

    private SearchResultVO toResult(SearchHit hit) {
        SearchIndexDocument document = hit.document();
        Map<String, List<String>> highlights = hit.highlights();
        String title = firstHighlight(highlights, "title");
        String summary = firstHighlight(highlights, "summary");
        if (summary == null) summary = firstHighlight(highlights, "content");
        if (summary == null) summary = firstHighlight(highlights, "title");
        return new SearchResultVO(document.contentType(), document.contentId(),
                title == null ? HtmlUtils.htmlEscape(document.title()) : safeEsHighlight(title),
                safeEsHighlight(summary == null ? snippet(document) : summary), document.url());
    }

    private void recordKeyword(String keyword, Long userId) {
        try {
            jdbcTemplate.update("INSERT INTO search_hot_keyword(keyword,heat) VALUES(?,1) " +
                    "ON DUPLICATE KEY UPDATE heat=heat+1", keyword);
        } catch (DataAccessException ex) {
            log.warn("Failed to record hot search keyword");
        }
        if (userId == null) return;
        try {
            String key = HISTORY_PREFIX + userId;
            long now = System.currentTimeMillis();
            redisTemplate.opsForZSet().add(key, keyword, now);
            redisTemplate.opsForZSet().removeRange(key, 0, -21);
            redisTemplate.expire(key, Duration.ofDays(30));
        } catch (DataAccessException ex) {
            log.warn("Failed to record search history, userId={}", userId);
        }
    }

    private static String firstHighlight(Map<String, List<String>> highlights, String field) {
        if (highlights == null) return null;
        List<String> values = highlights.get(field);
        return values == null || values.isEmpty() ? null : values.getFirst();
    }

    static String safeEsHighlight(String text) {
        if (text == null) return "";
        return HtmlUtils.htmlEscape(text)
                .replace("&lt;mark&gt;", "<mark>")
                .replace("&lt;/mark&gt;", "</mark>");
    }

    private static String snippet(SearchIndexDocument document) {
        String text = document.summary() == null || document.summary().isBlank()
                ? document.content() : document.summary();
        if (text == null) return "";
        return text.length() <= 240 ? text : text.substring(0, 240);
    }

    private static String normalizeKeyword(String keyword) {
        String value = keyword == null ? "" : keyword.trim();
        if (value.length() > 100) throw new ServiceException("搜索关键词不能超过100个字符");
        return value;
    }

    private static String normalizeType(String type) {
        if (type == null || type.isBlank()) return null;
        String value = type.trim().toUpperCase(Locale.ROOT);
        if (!PUBLIC_TYPES.contains(value)) throw new ServiceException("不支持的搜索类型");
        return value;
    }
}
