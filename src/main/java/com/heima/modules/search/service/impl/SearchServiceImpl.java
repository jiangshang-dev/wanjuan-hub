package com.heima.modules.search.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.heima.common.PageResult;
import com.heima.exception.ServiceException;
import com.heima.modules.search.dto.SearchDtos.SearchResultVO;
import com.heima.modules.search.entity.SearchDocument;
import com.heima.modules.search.mapper.SearchDocumentMapper;
import com.heima.modules.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {
    private static final String HISTORY_PREFIX = "wanjuan:search:history:";
    private static final Set<String> PUBLIC_TYPES = Set.of("BLOG", "RESOURCE", "FORUM_TOPIC");
    private final SearchDocumentMapper documentMapper;
    private final StringRedisTemplate redisTemplate;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public PageResult<SearchResultVO> search(String keyword, String type, long page, long size, Long userId) {
        String normalized = normalizeKeyword(keyword);
        String normalizedType = normalizeType(type);
        long current = Math.max(1, page);
        long pageSize = Math.min(100, Math.max(1, size));
        if (normalized.isEmpty()) return PageResult.of(List.of(), 0, current, pageSize);

        Page<SearchDocument> result = documentMapper.search(new Page<>(current, pageSize), normalized, normalizedType);
        recordKeyword(normalized, userId);
        List<SearchResultVO> records = result.getRecords().stream()
                .map(document -> new SearchResultVO(document.getContentType(), document.getContentId(),
                        HtmlUtils.htmlEscape(document.getTitle()),
                        safeHighlight(snippet(document), normalized), document.getUrl()))
                .toList();
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
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

    private void recordKeyword(String keyword, Long userId) {
        try {
            documentMapper.incrementHotKeyword(keyword);
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

    public static String safeHighlight(String text, String keyword) {
        if (text == null) return "";
        Matcher matcher = Pattern.compile(Pattern.quote(keyword), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE)
                .matcher(text);
        StringBuilder result = new StringBuilder();
        int cursor = 0;
        while (matcher.find()) {
            result.append(HtmlUtils.htmlEscape(text.substring(cursor, matcher.start())));
            result.append("<mark>").append(HtmlUtils.htmlEscape(matcher.group())).append("</mark>");
            cursor = matcher.end();
        }
        result.append(HtmlUtils.htmlEscape(text.substring(cursor)));
        return result.toString();
    }

    private static String snippet(SearchDocument document) {
        String text = document.getSummary() == null || document.getSummary().isBlank()
                ? document.getContent() : document.getSummary();
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
        String value = type.trim().toUpperCase();
        if (!PUBLIC_TYPES.contains(value)) throw new ServiceException("不支持的搜索类型");
        return value;
    }
}
