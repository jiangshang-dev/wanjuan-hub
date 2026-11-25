package com.heima.modules.operation.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.heima.common.PageResult;
import com.heima.exception.ServiceException;
import com.heima.modules.blog.entity.BlogArticle;
import com.heima.modules.blog.mapper.BlogArticleMapper;
import com.heima.modules.forum.entity.ForumEntities.Topic;
import com.heima.modules.forum.mapper.ForumMappers.TopicMapper;
import com.heima.modules.resource.entity.ResourceInfo;
import com.heima.modules.resource.mapper.ResourceInfoMapper;
import com.heima.modules.operation.dto.OperationDtos.AdvertisementRequest;
import com.heima.modules.operation.dto.OperationDtos.AnnouncementRequest;
import com.heima.modules.operation.dto.OperationDtos.ConfigRequest;
import com.heima.modules.operation.dto.OperationDtos.FriendLinkRequest;
import com.heima.modules.operation.dto.OperationDtos.HotKeywordRequest;
import com.heima.modules.operation.dto.OperationDtos.RecommendationRequest;
import com.heima.modules.operation.entity.OperationEntities.Advertisement;
import com.heima.modules.operation.entity.OperationEntities.Announcement;
import com.heima.modules.operation.entity.OperationEntities.FriendLink;
import com.heima.modules.operation.entity.OperationEntities.HotKeyword;
import com.heima.modules.operation.entity.OperationEntities.Recommendation;
import com.heima.modules.operation.entity.OperationEntities.SiteConfig;
import com.heima.modules.operation.mapper.OperationMappers.AdvertisementMapper;
import com.heima.modules.operation.mapper.OperationMappers.AnnouncementMapper;
import com.heima.modules.operation.mapper.OperationMappers.ConfigMapper;
import com.heima.modules.operation.mapper.OperationMappers.FriendLinkMapper;
import com.heima.modules.operation.mapper.OperationMappers.HotKeywordMapper;
import com.heima.modules.operation.mapper.OperationMappers.RecommendationMapper;
import com.heima.modules.operation.service.OperationService;
import com.heima.modules.operation.vo.OperationVOs.AdvertisementVO;
import com.heima.modules.operation.vo.OperationVOs.AnnouncementVO;
import com.heima.modules.operation.vo.OperationVOs.ConfigVO;
import com.heima.modules.operation.vo.OperationVOs.FriendLinkVO;
import com.heima.modules.operation.vo.OperationVOs.HotKeywordVO;
import com.heima.modules.operation.vo.OperationVOs.PublicConfigVO;
import com.heima.modules.operation.vo.OperationVOs.RecommendationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class OperationServiceImpl implements OperationService {
    private static final Set<String> CONTENT_TYPES = Set.of("BLOG", "RESOURCE", "FORUM_TOPIC");
    private static final List<String> SENSITIVE_KEY_FRAGMENTS = List.of("password", "passwd", "secret",
            "token", "credential", "private", "apikey", "api-key", "accesskey", "access-key",
            "encryptionkey", "encryption-key", "signingkey", "signing-key");

    private final AnnouncementMapper announcementMapper;
    private final RecommendationMapper recommendationMapper;
    private final AdvertisementMapper advertisementMapper;
    private final FriendLinkMapper friendLinkMapper;
    private final HotKeywordMapper hotKeywordMapper;
    private final ConfigMapper configMapper;
    private final BlogArticleMapper blogArticleMapper;
    private final ResourceInfoMapper resourceInfoMapper;
    private final TopicMapper topicMapper;

    @Override
    public PageResult<AnnouncementVO> announcementPage(long page, long size, String keyword, Integer status) {
        return page(announcementMapper, new LambdaQueryWrapper<Announcement>()
                .like(StringUtils.hasText(keyword), Announcement::getTitle, text(keyword))
                .eq(status != null, Announcement::getStatus, status)
                .orderByDesc(Announcement::getId), page, size, this::announcementVO);
    }

    @Override @Transactional
    public String createAnnouncement(AnnouncementRequest request) {
        validatePeriod(request.getPublishStart(), request.getPublishEnd());
        Announcement entity = new Announcement();
        BeanUtils.copyProperties(request, entity);
        return insert(announcementMapper, entity);
    }

    @Override @Transactional
    public void updateAnnouncement(Long id, AnnouncementRequest request) {
        validatePeriod(request.getPublishStart(), request.getPublishEnd());
        update(announcementMapper, id, request, "公告");
    }

    @Override @Transactional
    public void deleteAnnouncement(Long id) { delete(announcementMapper, id, "公告"); }

    @Override @Transactional
    public void announcementStatus(Long id, Integer status) {
        status(announcementMapper, id, status, "公告", Announcement::setStatus);
    }

    @Override
    public List<AnnouncementVO> publicAnnouncements(int limit) {
        LocalDateTime now = LocalDateTime.now();
        return announcementMapper.selectPublic(now, bounded(limit)).stream()
                .filter(item -> enabled(item.getStatus()) && available(item.getPublishStart(), item.getPublishEnd(), now))
                .limit(bounded(limit)).map(this::announcementVO).toList();
    }

    @Override
    public PageResult<RecommendationVO> recommendationPage(long page, long size, String keyword, String contentType, Integer status) {
        String type = contentType(contentType);
        return page(recommendationMapper, new LambdaQueryWrapper<Recommendation>()
                .like(StringUtils.hasText(keyword), Recommendation::getTitle, text(keyword))
                .eq(type != null, Recommendation::getContentType, type)
                .eq(status != null, Recommendation::getStatus, status)
                .orderByDesc(Recommendation::getId), page, size, this::recommendationVO);
    }

    @Override @Transactional
    public String createRecommendation(RecommendationRequest request) {
        Recommendation entity = new Recommendation();
        BeanUtils.copyProperties(request, entity);
        return insert(recommendationMapper, entity);
    }

    @Override @Transactional
    public void updateRecommendation(Long id, RecommendationRequest request) {
        update(recommendationMapper, id, request, "推荐位");
    }

    @Override @Transactional
    public void deleteRecommendation(Long id) { delete(recommendationMapper, id, "推荐位"); }

    @Override @Transactional
    public void recommendationStatus(Long id, Integer status) {
        status(recommendationMapper, id, status, "推荐位", Recommendation::setStatus);
    }

    @Override
    public List<RecommendationVO> publicRecommendations(int limit) {
        return recommendationMapper.selectPublic(bounded(limit)).stream()
                .filter(item -> enabled(item.getStatus()) && hasPublicTarget(item)).limit(bounded(limit))
                .map(this::recommendationVO).toList();
    }

    @Override
    public PageResult<AdvertisementVO> advertisementPage(long page, long size, String keyword, String position, Integer status) {
        return page(advertisementMapper, new LambdaQueryWrapper<Advertisement>()
                .like(StringUtils.hasText(keyword), Advertisement::getTitle, text(keyword))
                .eq(StringUtils.hasText(position), Advertisement::getPosition, text(position))
                .eq(status != null, Advertisement::getStatus, status)
                .orderByDesc(Advertisement::getId), page, size, this::advertisementVO);
    }

    @Override @Transactional
    public String createAdvertisement(AdvertisementRequest request) {
        validatePeriod(request.getStartTime(), request.getEndTime());
        Advertisement entity = new Advertisement();
        BeanUtils.copyProperties(request, entity);
        return insert(advertisementMapper, entity);
    }

    @Override @Transactional
    public void updateAdvertisement(Long id, AdvertisementRequest request) {
        validatePeriod(request.getStartTime(), request.getEndTime());
        update(advertisementMapper, id, request, "广告位");
    }

    @Override @Transactional
    public void deleteAdvertisement(Long id) { delete(advertisementMapper, id, "广告位"); }

    @Override @Transactional
    public void advertisementStatus(Long id, Integer status) {
        status(advertisementMapper, id, status, "广告位", Advertisement::setStatus);
    }

    @Override
    public List<AdvertisementVO> publicAdvertisements(String position, int limit) {
        LocalDateTime now = LocalDateTime.now();
        String normalizedPosition = StringUtils.hasText(position) ? position.trim() : null;
        return advertisementMapper.selectPublic(normalizedPosition, now, bounded(limit)).stream()
                .filter(item -> enabled(item.getStatus()) && available(item.getStartTime(), item.getEndTime(), now))
                .limit(bounded(limit)).map(this::advertisementVO).toList();
    }

    @Override
    public PageResult<FriendLinkVO> friendLinkPage(long page, long size, String keyword, Integer status) {
        return page(friendLinkMapper, new LambdaQueryWrapper<FriendLink>()
                .like(StringUtils.hasText(keyword), FriendLink::getName, text(keyword))
                .eq(status != null, FriendLink::getStatus, status)
                .orderByDesc(FriendLink::getId), page, size, this::friendLinkVO);
    }

    @Override @Transactional
    public String createFriendLink(FriendLinkRequest request) {
        FriendLink entity = new FriendLink();
        BeanUtils.copyProperties(request, entity);
        return insert(friendLinkMapper, entity);
    }

    @Override @Transactional
    public void updateFriendLink(Long id, FriendLinkRequest request) {
        update(friendLinkMapper, id, request, "友情链接");
    }

    @Override @Transactional
    public void deleteFriendLink(Long id) { delete(friendLinkMapper, id, "友情链接"); }

    @Override @Transactional
    public void friendLinkStatus(Long id, Integer status) {
        status(friendLinkMapper, id, status, "友情链接", FriendLink::setStatus);
    }

    @Override
    public List<FriendLinkVO> publicFriendLinks(int limit) {
        return friendLinkMapper.selectPublic(bounded(limit)).stream()
                .filter(item -> enabled(item.getStatus())).limit(bounded(limit)).map(this::friendLinkVO).toList();
    }

    @Override
    public PageResult<HotKeywordVO> hotKeywordPage(long page, long size, String keyword, Integer status) {
        return page(hotKeywordMapper, new LambdaQueryWrapper<HotKeyword>()
                .like(StringUtils.hasText(keyword), HotKeyword::getKeyword, text(keyword))
                .eq(status != null, HotKeyword::getStatus, status)
                .orderByDesc(HotKeyword::getId), page, size, this::hotKeywordVO);
    }

    @Override @Transactional
    public String createHotKeyword(HotKeywordRequest request) {
        HotKeyword entity = new HotKeyword();
        BeanUtils.copyProperties(request, entity);
        return insert(hotKeywordMapper, entity);
    }

    @Override @Transactional
    public void updateHotKeyword(Long id, HotKeywordRequest request) {
        update(hotKeywordMapper, id, request, "热门搜索");
    }

    @Override @Transactional
    public void deleteHotKeyword(Long id) { delete(hotKeywordMapper, id, "热门搜索"); }

    @Override @Transactional
    public void hotKeywordStatus(Long id, Integer status) {
        status(hotKeywordMapper, id, status, "热门搜索", HotKeyword::setStatus);
    }

    @Override
    public List<HotKeywordVO> publicHotKeywords(int limit) {
        return hotKeywordMapper.selectPublic(bounded(limit)).stream()
                .filter(item -> enabled(item.getStatus())).limit(bounded(limit)).map(this::hotKeywordVO).toList();
    }

    @Override
    public PageResult<ConfigVO> configPage(long page, long size, String keyword, Integer publicFlag, Integer status) {
        return page(configMapper, new LambdaQueryWrapper<SiteConfig>()
                .and(StringUtils.hasText(keyword), wrapper -> wrapper
                        .like(SiteConfig::getConfigKey, text(keyword)).or()
                        .like(SiteConfig::getDescription, text(keyword)))
                .eq(publicFlag != null, SiteConfig::getPublicFlag, publicFlag)
                .eq(status != null, SiteConfig::getStatus, status)
                .orderByDesc(SiteConfig::getId), page, size, this::configVO);
    }

    @Override @Transactional
    public String createConfig(ConfigRequest request) {
        validatePublicConfig(request.getConfigKey(), request.getPublicFlag());
        SiteConfig entity = new SiteConfig();
        BeanUtils.copyProperties(request, entity);
        return insert(configMapper, entity);
    }

    @Override @Transactional
    public void updateConfig(Long id, ConfigRequest request) {
        validatePublicConfig(request.getConfigKey(), request.getPublicFlag());
        update(configMapper, id, request, "网站参数");
    }

    @Override @Transactional
    public void deleteConfig(Long id) { delete(configMapper, id, "网站参数"); }

    @Override @Transactional
    public void configStatus(Long id, Integer status) {
        SiteConfig config = require(configMapper, id, "网站参数");
        validatePublicConfig(config.getConfigKey(), status == 1 ? config.getPublicFlag() : 0);
        config.setStatus(status);
        configMapper.updateById(config);
    }

    @Override
    public List<PublicConfigVO> publicConfigs(int limit) {
        return configMapper.selectPublic(bounded(limit)).stream()
                .filter(item -> enabled(item.getStatus()) && item.getPublicFlag() != null && item.getPublicFlag() == 1)
                .filter(item -> !isSensitiveKey(item.getConfigKey())).limit(bounded(limit))
                .map(item -> new PublicConfigVO(item.getConfigKey(), item.getConfigValue(), item.getDescription())).toList();
    }

    private boolean hasPublicTarget(Recommendation recommendation) {
        if (recommendation.getContentType() == null || recommendation.getContentId() == null) return false;
        return switch (recommendation.getContentType()) {
            case "BLOG" -> {
                BlogArticle target = blogArticleMapper.selectById(recommendation.getContentId());
                yield target != null && Integer.valueOf(0).equals(target.getDeleted())
                        && !"OFFLINE".equals(target.getStatus()) && target.getPublishedVersionNo() != null;
            }
            case "RESOURCE" -> {
                ResourceInfo target = resourceInfoMapper.selectById(recommendation.getContentId());
                yield target != null && Integer.valueOf(0).equals(target.getDeleted())
                        && Integer.valueOf(1).equals(target.getOnline()) && Integer.valueOf(0).equals(target.getInvalid())
                        && target.getPublishedVersionNo() != null;
            }
            case "FORUM_TOPIC" -> {
                Topic target = topicMapper.selectById(recommendation.getContentId());
                yield target != null && Integer.valueOf(0).equals(target.getDeleted())
                        && !"OFFLINE".equals(target.getStatus()) && target.getPublishedVersionNo() != null;
            }
            default -> false;
        };
    }

    static boolean available(LocalDateTime start, LocalDateTime end, LocalDateTime now) {
        return (start == null || !start.isAfter(now)) && (end == null || end.isAfter(now));
    }

    static boolean isSensitiveKey(String key) {
        if (key == null) return true;
        String normalized = key.toLowerCase(Locale.ROOT);
        return SENSITIVE_KEY_FRAGMENTS.stream().anyMatch(normalized::contains);
    }

    private static void validatePublicConfig(String key, Integer publicFlag) {
        if (publicFlag != null && publicFlag == 1 && isSensitiveKey(key)) {
            throw new ServiceException("敏感系统参数禁止公开");
        }
    }

    private static void validatePeriod(LocalDateTime start, LocalDateTime end) {
        if (start != null && end != null && !end.isAfter(start)) {
            throw new ServiceException("结束时间必须晚于开始时间");
        }
    }

    private static String contentType(String contentType) {
        if (!StringUtils.hasText(contentType)) return null;
        String type = contentType.trim().toUpperCase(Locale.ROOT);
        if (!CONTENT_TYPES.contains(type)) throw new ServiceException("不支持的推荐内容类型");
        return type;
    }

    private static boolean enabled(Integer status) { return status != null && status == 1; }
    private static String text(String value) { return value == null ? "" : value.trim(); }
    private static int bounded(int limit) { return Math.min(100, Math.max(1, limit)); }

    private <T, R> PageResult<R> page(BaseMapper<T> mapper, Wrapper<T> wrapper, long page, long size,
                                      Function<T, R> converter) {
        Page<T> result = mapper.selectPage(new Page<>(Math.max(1, page), Math.min(100, Math.max(1, size))), wrapper);
        return PageResult.of(result.getRecords().stream().map(converter).toList(), result.getTotal(),
                result.getCurrent(), result.getSize());
    }

    private static <T> String insert(BaseMapper<T> mapper, T entity) {
        mapper.insert(entity);
        return String.valueOf(((com.heima.common.BaseEntity) entity).getId());
    }

    private static <T> void update(BaseMapper<T> mapper, Long id, Object request, String name) {
        T entity = require(mapper, id, name);
        BeanUtils.copyProperties(request, entity);
        mapper.updateById(entity);
    }

    private static <T> void delete(BaseMapper<T> mapper, Long id, String name) {
        require(mapper, id, name);
        mapper.deleteById(id);
    }

    private static <T> void status(BaseMapper<T> mapper, Long id, Integer status, String name,
                                   BiConsumer<T, Integer> setter) {
        T entity = require(mapper, id, name);
        setter.accept(entity, status);
        mapper.updateById(entity);
    }

    private static <T> T require(BaseMapper<T> mapper, Long id, String name) {
        T entity = mapper.selectById(id);
        if (entity == null) throw new ServiceException(404, name + "不存在");
        return entity;
    }

    private AnnouncementVO announcementVO(Announcement item) {
        return new AnnouncementVO(String.valueOf(item.getId()), item.getTitle(), item.getContent(), item.getStatus(),
                item.getPublishStart(), item.getPublishEnd(), item.getSortOrder(), item.getCreateTime(), item.getUpdateTime());
    }

    private RecommendationVO recommendationVO(Recommendation item) {
        return new RecommendationVO(String.valueOf(item.getId()), item.getContentType(), String.valueOf(item.getContentId()),
                item.getTitle(), item.getReason(), item.getStatus(), item.getSortOrder(), item.getCreateTime(), item.getUpdateTime());
    }

    private AdvertisementVO advertisementVO(Advertisement item) {
        return new AdvertisementVO(String.valueOf(item.getId()), item.getTitle(), item.getImageUrl(), item.getTargetUrl(),
                item.getPosition(), item.getStatus(), item.getStartTime(), item.getEndTime(), item.getSortOrder(),
                item.getCreateTime(), item.getUpdateTime());
    }

    private FriendLinkVO friendLinkVO(FriendLink item) {
        return new FriendLinkVO(String.valueOf(item.getId()), item.getName(), item.getUrl(), item.getLogoUrl(),
                item.getStatus(), item.getSortOrder(), item.getCreateTime(), item.getUpdateTime());
    }

    private HotKeywordVO hotKeywordVO(HotKeyword item) {
        return new HotKeywordVO(String.valueOf(item.getId()), item.getKeyword(), item.getHeat(), item.getStatus(),
                item.getSortOrder(), item.getCreateTime(), item.getUpdateTime());
    }

    private ConfigVO configVO(SiteConfig item) {
        return new ConfigVO(String.valueOf(item.getId()), item.getConfigKey(), item.getConfigValue(), item.getDescription(),
                item.getPublicFlag(), item.getStatus(), item.getCreateTime(), item.getUpdateTime());
    }
}
