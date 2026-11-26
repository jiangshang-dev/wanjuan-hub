package com.heima.modules.search.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.VersionType;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HighlightField;
import co.elastic.clients.util.NamedValue;
import com.heima.exception.ServiceException;
import com.heima.modules.search.dto.SearchIndexDocument;
import com.heima.modules.search.service.SearchIndexRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ElasticsearchSearchIndexRepository implements SearchIndexRepository {
    private final ElasticsearchClient elasticsearchClient;
    private volatile boolean initialized;

    @Override
    public void save(SearchIndexDocument document, long eventVersion) {
        ensureIndex();
        try {
            elasticsearchClient.index(request -> request.index(INDEX_NAME)
                    .id(documentId(document.contentType(), document.contentId()))
                    .document(document).version(eventVersion).versionType(VersionType.External));
        } catch (ElasticsearchException ex) {
            if (ex.status() != 409) throw unavailable(ex);
        } catch (IOException ex) {
            throw unavailable(ex);
        }
    }

    @Override
    public void delete(String contentType, Long contentId, long eventVersion) {
        ensureIndex();
        try {
            elasticsearchClient.delete(request -> request.index(INDEX_NAME)
                    .id(documentId(contentType, contentId))
                    .version(eventVersion).versionType(VersionType.External));
        } catch (ElasticsearchException ex) {
            if (ex.status() != 404 && ex.status() != 409) throw unavailable(ex);
        } catch (IOException ex) {
            throw unavailable(ex);
        }
    }

    @Override
    public SearchPage search(String keyword, String contentType, int from, int size) {
        ensureIndex();
        try {
            SearchResponse<SearchIndexDocument> response = elasticsearchClient.search(request -> request
                            .index(INDEX_NAME).from(from).size(size)
                            .query(query -> query.bool(bool -> {
                                bool.must(must -> must.multiMatch(multi -> multi.query(keyword)
                                        .fields("title^3", "summary^2", "content").analyzer("ik_smart")));
                                if (contentType != null) {
                                    bool.filter(filter -> filter.term(term -> term.field("contentType").value(contentType)));
                                }
                                return bool;
                            }))
                            .highlight(highlight -> highlight.preTags("<mark>").postTags("</mark>")
                                    .fields(NamedValue.of("title", HighlightField.of(field -> field.numberOfFragments(0))))
                                    .fields(NamedValue.of("summary", HighlightField.of(field -> field.fragmentSize(240).numberOfFragments(1))))
                                    .fields(NamedValue.of("content", HighlightField.of(field -> field.fragmentSize(240).numberOfFragments(1))))),
                    SearchIndexDocument.class);
            long total = response.hits().total() == null ? 0 : response.hits().total().value();
            List<SearchHit> hits = response.hits().hits().stream()
                    .filter(hit -> hit.source() != null).map(this::toSearchHit).toList();
            return new SearchPage(total, hits);
        } catch (ElasticsearchException | IOException ex) {
            throw unavailable(ex);
        }
    }

    private SearchHit toSearchHit(Hit<SearchIndexDocument> hit) {
        return new SearchHit(hit.source(), hit.highlight());
    }

    private void ensureIndex() {
        if (initialized) return;
        synchronized (this) {
            if (initialized) return;
            try {
                if (!elasticsearchClient.indices().exists(request -> request.index(INDEX_NAME)).value()) {
                    try {
                        elasticsearchClient.indices().create(request -> request.index(INDEX_NAME)
                                .mappings(mapping -> mapping
                                        .properties("contentType", property -> property.keyword(keyword -> keyword))
                                        .properties("contentId", property -> property.long_(number -> number))
                                        .properties("title", property -> property.text(text -> text
                                                .analyzer("ik_max_word").searchAnalyzer("ik_smart")))
                                        .properties("summary", property -> property.text(text -> text
                                                .analyzer("ik_max_word").searchAnalyzer("ik_smart")))
                                        .properties("content", property -> property.text(text -> text
                                                .analyzer("ik_max_word").searchAnalyzer("ik_smart")))
                                        .properties("url", property -> property.keyword(keyword -> keyword.index(false)))));
                    } catch (ElasticsearchException ex) {
                        if (!elasticsearchClient.indices().exists(request -> request.index(INDEX_NAME)).value()) throw ex;
                    }
                }
                initialized = true;
            } catch (ElasticsearchException | IOException ex) {
                throw unavailable(ex);
            }
        }
    }

    public static String documentId(String contentType, Long contentId) {
        return contentType + ":" + contentId;
    }

    private ServiceException unavailable(Exception cause) {
        return new ServiceException(503, "搜索服务暂不可用，请稍后重试", cause);
    }
}
