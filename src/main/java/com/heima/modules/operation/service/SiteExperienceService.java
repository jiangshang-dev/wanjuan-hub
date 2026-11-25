package com.heima.modules.operation.service;

import com.heima.common.PageResult;
import com.heima.modules.operation.dto.SiteExperienceDtos.BackgroundMusicRequest;
import com.heima.modules.operation.dto.SiteExperienceDtos.GrayDayRequest;
import com.heima.modules.operation.vo.SiteExperienceVOs.BackgroundMusicVO;
import com.heima.modules.operation.vo.SiteExperienceVOs.GrayDayVO;
import com.heima.modules.operation.vo.SiteExperienceVOs.GrayStatusVO;
import com.heima.modules.operation.vo.SiteExperienceVOs.PublicBackgroundMusicVO;

import java.time.LocalDate;

public interface SiteExperienceService {
    PageResult<GrayDayVO> grayDayPage(long page, long size, LocalDate startDate, LocalDate endDate, Integer status);
    String createGrayDay(GrayDayRequest request);
    void updateGrayDay(Long id, GrayDayRequest request);
    void grayDayStatus(Long id, Integer status);

    GrayStatusVO currentGrayDay();
    boolean isGrayDay();
    String grayReason();

    BackgroundMusicVO backgroundMusic();
    void saveBackgroundMusic(BackgroundMusicRequest request);
    PublicBackgroundMusicVO publicBackgroundMusic();
}
