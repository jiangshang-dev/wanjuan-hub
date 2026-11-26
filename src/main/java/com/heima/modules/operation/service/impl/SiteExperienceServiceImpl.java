package com.heima.modules.operation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.heima.common.PageResult;
import com.heima.exception.ServiceException;
import com.heima.modules.operation.dto.SiteExperienceDtos.BackgroundMusicRequest;
import com.heima.modules.operation.dto.SiteExperienceDtos.GrayDayRequest;
import com.heima.modules.operation.entity.SiteExperienceEntities.BackgroundMusic;
import com.heima.modules.operation.entity.SiteExperienceEntities.GrayDay;
import com.heima.modules.operation.mapper.SiteExperienceMappers.BackgroundMusicMapper;
import com.heima.modules.operation.mapper.SiteExperienceMappers.GrayDayMapper;
import com.heima.modules.operation.service.SiteExperienceService;
import com.heima.modules.operation.vo.SiteExperienceVOs.BackgroundMusicVO;
import com.heima.modules.operation.vo.SiteExperienceVOs.GrayDayVO;
import com.heima.modules.operation.vo.SiteExperienceVOs.GrayStatusVO;
import com.heima.modules.operation.vo.SiteExperienceVOs.PublicBackgroundMusicVO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class SiteExperienceServiceImpl implements SiteExperienceService {
    private static final long BACKGROUND_MUSIC_ID = 1L;

    private final GrayDayMapper grayDayMapper;
    private final BackgroundMusicMapper backgroundMusicMapper;

    @Override @Transactional(readOnly = true)
    public PageResult<GrayDayVO> grayDayPage(long page, long size, LocalDate startDate, LocalDate endDate, Integer status) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new ServiceException("结束日期不能早于开始日期");
        }
        Page<GrayDay> result = grayDayMapper.selectPage(new Page<>(pageNumber(page), pageSize(size)),
                new LambdaQueryWrapper<GrayDay>()
                        .ge(startDate != null, GrayDay::getGrayDate, startDate)
                        .le(endDate != null, GrayDay::getGrayDate, endDate)
                        .eq(status != null, GrayDay::getStatus, status)
                        .orderByAsc(GrayDay::getGrayDate).orderByDesc(GrayDay::getId));
        return PageResult.of(result.getRecords().stream().map(this::grayDayVO).toList(),
                result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override @Transactional
    public String createGrayDay(GrayDayRequest request) {
        ensureDateAvailable(request.getGrayDate(), null);
        GrayDay entity = new GrayDay();
        entity.setGrayDate(request.getGrayDate());
        entity.setReason(request.getReason().trim());
        entity.setStatus(request.getStatus());
        try {
            grayDayMapper.insert(entity);
        } catch (DuplicateKeyException ex) {
            throw new ServiceException(409, "该日期的灰色日配置已存在");
        }
        return String.valueOf(entity.getId());
    }

    @Override @Transactional
    public void updateGrayDay(Long id, GrayDayRequest request) {
        GrayDay entity = requireGrayDay(id);
        ensureDateAvailable(request.getGrayDate(), id);
        entity.setGrayDate(request.getGrayDate());
        entity.setReason(request.getReason().trim());
        entity.setStatus(request.getStatus());
        try {
            grayDayMapper.updateById(entity);
        } catch (DuplicateKeyException ex) {
            throw new ServiceException(409, "该日期的灰色日配置已存在");
        }
    }

    @Override @Transactional
    public void grayDayStatus(Long id, Integer status) {
        GrayDay entity = requireGrayDay(id);
        entity.setStatus(status);
        grayDayMapper.updateById(entity);
    }

    @Override @Transactional(readOnly = true)
    public GrayStatusVO currentGrayDay() {
        GrayDay grayDay = grayDayMapper.selectOne(new LambdaQueryWrapper<GrayDay>()
                .eq(GrayDay::getGrayDate, LocalDate.now()).eq(GrayDay::getStatus, 1).last("LIMIT 1"));
        return grayDay == null ? new GrayStatusVO(false, null) : new GrayStatusVO(true, grayDay.getReason());
    }

    @Override @Transactional(readOnly = true)
    public boolean isGrayDay() {
        return currentGrayDay().gray();
    }

    @Override @Transactional(readOnly = true)
    public String grayReason() {
        return currentGrayDay().reason();
    }

    @Override @Transactional(readOnly = true)
    public BackgroundMusicVO backgroundMusic() {
        BackgroundMusic entity = requireBackgroundMusic();
        return new BackgroundMusicVO(enabled(entity.getEnabled()), entity.getAudioUrl(), entity.getPlayMode(),
                enabled(entity.getPlayerVisible()), entity.getUpdateTime());
    }

    @Override @Transactional
    public void saveBackgroundMusic(BackgroundMusicRequest request) {
        boolean enabled = Boolean.TRUE.equals(request.getEnabled());
        String audioUrl = request.getAudioUrl() == null ? "" : request.getAudioUrl().trim();
        if (enabled && !StringUtils.hasText(audioUrl)) throw new ServiceException("开启背景音乐时必须配置音频地址");
        BackgroundMusic entity = requireBackgroundMusic();
        entity.setEnabled(enabled ? 1 : 0);
        entity.setAudioUrl(audioUrl);
        entity.setPlayMode(request.getPlayMode());
        entity.setPlayerVisible(Boolean.TRUE.equals(request.getPlayerVisible()) ? 1 : 0);
        backgroundMusicMapper.updateById(entity);
    }

    @Override @Transactional(readOnly = true)
    public PublicBackgroundMusicVO publicBackgroundMusic() {
        BackgroundMusic entity = requireBackgroundMusic();
        return new PublicBackgroundMusicVO(enabled(entity.getEnabled()), entity.getAudioUrl(), entity.getPlayMode(),
                enabled(entity.getPlayerVisible()));
    }

    private void ensureDateAvailable(LocalDate grayDate, Long excludedId) {
        Long count = grayDayMapper.selectCount(new LambdaQueryWrapper<GrayDay>()
                .eq(GrayDay::getGrayDate, grayDate)
                .ne(excludedId != null, GrayDay::getId, excludedId));
        if (count != null && count > 0) throw new ServiceException(409, "该日期的灰色日配置已存在");
    }

    private GrayDay requireGrayDay(Long id) {
        GrayDay entity = grayDayMapper.selectById(id);
        if (entity == null) throw new ServiceException(404, "灰色日配置不存在");
        return entity;
    }

    private BackgroundMusic requireBackgroundMusic() {
        BackgroundMusic entity = backgroundMusicMapper.selectById(BACKGROUND_MUSIC_ID);
        if (entity == null) throw new ServiceException(404, "背景音乐配置不存在");
        return entity;
    }

    private GrayDayVO grayDayVO(GrayDay entity) {
        return new GrayDayVO(String.valueOf(entity.getId()), entity.getGrayDate(), entity.getReason(), entity.getStatus(),
                entity.getCreateTime(), entity.getUpdateTime());
    }

    private static boolean enabled(Integer value) { return Integer.valueOf(1).equals(value); }
    private static long pageNumber(long page) { return Math.max(1, page); }
    private static long pageSize(long size) { return Math.min(100, Math.max(1, size)); }
}
