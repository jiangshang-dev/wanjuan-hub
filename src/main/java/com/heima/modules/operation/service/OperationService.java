package com.heima.modules.operation.service;

import com.heima.common.PageResult;
import com.heima.modules.operation.dto.OperationDtos.AdvertisementRequest;
import com.heima.modules.operation.dto.OperationDtos.AnnouncementRequest;
import com.heima.modules.operation.dto.OperationDtos.ConfigRequest;
import com.heima.modules.operation.dto.OperationDtos.FriendLinkRequest;
import com.heima.modules.operation.dto.OperationDtos.HotKeywordRequest;
import com.heima.modules.operation.dto.OperationDtos.RecommendationRequest;
import com.heima.modules.operation.vo.OperationVOs.AdvertisementVO;
import com.heima.modules.operation.vo.OperationVOs.AnnouncementVO;
import com.heima.modules.operation.vo.OperationVOs.ConfigVO;
import com.heima.modules.operation.vo.OperationVOs.FriendLinkVO;
import com.heima.modules.operation.vo.OperationVOs.HotKeywordVO;
import com.heima.modules.operation.vo.OperationVOs.PublicConfigVO;
import com.heima.modules.operation.vo.OperationVOs.RecommendationVO;

import java.util.List;

public interface OperationService {
    PageResult<AnnouncementVO> announcementPage(long page, long size, String keyword, Integer status);
    String createAnnouncement(AnnouncementRequest request);
    void updateAnnouncement(Long id, AnnouncementRequest request);
    void deleteAnnouncement(Long id);
    void announcementStatus(Long id, Integer status);
    List<AnnouncementVO> publicAnnouncements(int limit);

    PageResult<RecommendationVO> recommendationPage(long page, long size, String keyword, String contentType, Integer status);
    String createRecommendation(RecommendationRequest request);
    void updateRecommendation(Long id, RecommendationRequest request);
    void deleteRecommendation(Long id);
    void recommendationStatus(Long id, Integer status);
    List<RecommendationVO> publicRecommendations(int limit);

    PageResult<AdvertisementVO> advertisementPage(long page, long size, String keyword, String position, Integer status);
    String createAdvertisement(AdvertisementRequest request);
    void updateAdvertisement(Long id, AdvertisementRequest request);
    void deleteAdvertisement(Long id);
    void advertisementStatus(Long id, Integer status);
    List<AdvertisementVO> publicAdvertisements(String position, int limit);

    PageResult<FriendLinkVO> friendLinkPage(long page, long size, String keyword, Integer status);
    String createFriendLink(FriendLinkRequest request);
    void updateFriendLink(Long id, FriendLinkRequest request);
    void deleteFriendLink(Long id);
    void friendLinkStatus(Long id, Integer status);
    List<FriendLinkVO> publicFriendLinks(int limit);

    PageResult<HotKeywordVO> hotKeywordPage(long page, long size, String keyword, Integer status);
    String createHotKeyword(HotKeywordRequest request);
    void updateHotKeyword(Long id, HotKeywordRequest request);
    void deleteHotKeyword(Long id);
    void hotKeywordStatus(Long id, Integer status);
    List<HotKeywordVO> publicHotKeywords(int limit);

    PageResult<ConfigVO> configPage(long page, long size, String keyword, Integer publicFlag, Integer status);
    String createConfig(ConfigRequest request);
    void updateConfig(Long id, ConfigRequest request);
    void deleteConfig(Long id);
    void configStatus(Long id, Integer status);
    List<PublicConfigVO> publicConfigs(int limit);
}
