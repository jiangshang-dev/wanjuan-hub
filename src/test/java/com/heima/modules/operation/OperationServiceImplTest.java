package com.heima.modules.operation;

import com.heima.exception.ServiceException;
import com.heima.modules.blog.entity.BlogArticle;
import com.heima.modules.blog.mapper.BlogArticleMapper;
import com.heima.modules.forum.entity.ForumEntities.Topic;
import com.heima.modules.forum.mapper.ForumMappers.TopicMapper;
import com.heima.modules.resource.entity.ResourceInfo;
import com.heima.modules.resource.mapper.ResourceInfoMapper;
import com.heima.modules.operation.dto.OperationDtos.ConfigRequest;
import com.heima.modules.operation.entity.OperationEntities.Announcement;
import com.heima.modules.operation.entity.OperationEntities.Recommendation;
import com.heima.modules.operation.entity.OperationEntities.SiteConfig;
import com.heima.modules.operation.mapper.OperationMappers.AdvertisementMapper;
import com.heima.modules.operation.mapper.OperationMappers.AnnouncementMapper;
import com.heima.modules.operation.mapper.OperationMappers.ConfigMapper;
import com.heima.modules.operation.mapper.OperationMappers.FriendLinkMapper;
import com.heima.modules.operation.mapper.OperationMappers.HotKeywordMapper;
import com.heima.modules.operation.mapper.OperationMappers.RecommendationMapper;
import com.heima.modules.operation.service.impl.OperationServiceImpl;
import com.heima.modules.operation.vo.OperationVOs.AnnouncementVO;
import com.heima.modules.operation.vo.OperationVOs.PublicConfigVO;
import com.heima.modules.operation.vo.OperationVOs.RecommendationVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OperationServiceImplTest {
    @Mock private AnnouncementMapper announcementMapper;
    @Mock private RecommendationMapper recommendationMapper;
    @Mock private AdvertisementMapper advertisementMapper;
    @Mock private FriendLinkMapper friendLinkMapper;
    @Mock private HotKeywordMapper hotKeywordMapper;
    @Mock private ConfigMapper configMapper;
    @Mock private BlogArticleMapper blogArticleMapper;
    @Mock private ResourceInfoMapper resourceInfoMapper;
    @Mock private TopicMapper topicMapper;
    @InjectMocks private OperationServiceImpl operationService;

    @Test
    void publicAnnouncementsExcludeDisabledFutureAndExpiredItems() {
        LocalDateTime now = LocalDateTime.now();
        Announcement active = announcement(1L, 1, null, null);
        Announcement expired = announcement(2L, 1, null, now.minusSeconds(1));
        Announcement future = announcement(3L, 1, now.plusDays(1), null);
        Announcement disabled = announcement(4L, 0, null, null);
        when(announcementMapper.selectPublic(any(), eq(100))).thenReturn(List.of(active, expired, future, disabled));

        List<AnnouncementVO> result = operationService.publicAnnouncements(100);

        assertEquals(List.of("1"), result.stream().map(AnnouncementVO::id).toList());
    }

    @Test
    void publicConfigsApplyDefenseInDepthSensitiveKeyMasking() {
        when(configMapper.selectPublic(anyInt())).thenReturn(List.of(
                config(1L, "site.name", "万卷汇", 1, 1),
                config(2L, "security.jwt.secret", "must-not-leak", 1, 1),
                config(3L, "site.disabled", "hidden", 1, 0),
                config(4L, "site.internalToken", "hidden", 0, 1)));

        List<PublicConfigVO> result = operationService.publicConfigs(100);

        assertEquals(1, result.size());
        assertEquals("site.name", result.getFirst().configKey());
        assertEquals("万卷汇", result.getFirst().configValue());
    }

    @Test
    void publicRecommendationsRecheckThatTargetsRemainPublic() {
        when(recommendationMapper.selectPublic(100)).thenReturn(List.of(
                recommendation(1L, "BLOG", 11L), recommendation(2L, "BLOG", 12L),
                recommendation(3L, "RESOURCE", 13L), recommendation(4L, "RESOURCE", 14L),
                recommendation(5L, "FORUM_TOPIC", 15L), recommendation(6L, "FORUM_TOPIC", 16L),
                recommendation(7L, "BLOG", 17L)));
        when(blogArticleMapper.selectById(11L)).thenReturn(blog(0, "PUBLISHED", 1));
        when(blogArticleMapper.selectById(12L)).thenReturn(blog(1, "PUBLISHED", 1));
        when(blogArticleMapper.selectById(17L)).thenReturn(blog(0, "PUBLISHED", null));
        when(resourceInfoMapper.selectById(13L)).thenReturn(resource(0, 1, 0, 1));
        when(resourceInfoMapper.selectById(14L)).thenReturn(resource(0, 0, 0, 1));
        when(topicMapper.selectById(15L)).thenReturn(topic(0, "PUBLISHED", 1));
        when(topicMapper.selectById(16L)).thenReturn(topic(1, "PUBLISHED", 1));

        List<RecommendationVO> result = operationService.publicRecommendations(100);

        assertEquals(List.of("1", "3", "5"), result.stream().map(RecommendationVO::id).toList());
    }

    @Test
    void sensitiveConfigCannotBeMarkedPublicByAdministrator() {
        ConfigRequest request = new ConfigRequest();
        request.setConfigKey("security.api-token");
        request.setConfigValue("secret-value");
        request.setPublicFlag(1);
        request.setStatus(1);

        ServiceException error = assertThrows(ServiceException.class, () -> operationService.createConfig(request));

        assertEquals("敏感系统参数禁止公开", error.getMessage());
        verify(configMapper, never()).insert(org.mockito.ArgumentMatchers.<SiteConfig>any());
    }

    private static Recommendation recommendation(Long id, String contentType, Long contentId) {
        Recommendation item = new Recommendation();
        item.setId(id);
        item.setContentType(contentType);
        item.setContentId(contentId);
        item.setTitle("推荐" + id);
        item.setReason("原因");
        item.setStatus(1);
        item.setSortOrder(0);
        return item;
    }

    private static BlogArticle blog(int deleted, String status, Integer publishedVersionNo) {
        BlogArticle target = new BlogArticle();
        target.setDeleted(deleted);
        target.setStatus(status);
        target.setPublishedVersionNo(publishedVersionNo);
        return target;
    }

    private static ResourceInfo resource(int deleted, int online, int invalid, Integer publishedVersionNo) {
        ResourceInfo target = new ResourceInfo();
        target.setDeleted(deleted);
        target.setOnline(online);
        target.setInvalid(invalid);
        target.setPublishedVersionNo(publishedVersionNo);
        return target;
    }

    private static Topic topic(int deleted, String status, Integer publishedVersionNo) {
        Topic target = new Topic();
        target.setDeleted(deleted);
        target.setStatus(status);
        target.setPublishedVersionNo(publishedVersionNo);
        return target;
    }

    private static Announcement announcement(Long id, int status, LocalDateTime start, LocalDateTime end) {
        Announcement item = new Announcement();
        item.setId(id);
        item.setTitle("公告" + id);
        item.setContent("内容");
        item.setStatus(status);
        item.setPublishStart(start);
        item.setPublishEnd(end);
        item.setSortOrder(0);
        return item;
    }

    private static SiteConfig config(Long id, String key, String value, int publicFlag, int status) {
        SiteConfig item = new SiteConfig();
        item.setId(id);
        item.setConfigKey(key);
        item.setConfigValue(value);
        item.setDescription(key);
        item.setPublicFlag(publicFlag);
        item.setStatus(status);
        return item;
    }
}
