package com.heima.modules.operation;

import com.heima.exception.ServiceException;
import com.heima.modules.operation.dto.SiteExperienceDtos.BackgroundMusicRequest;
import com.heima.modules.operation.entity.SiteExperienceEntities.BackgroundMusic;
import com.heima.modules.operation.entity.SiteExperienceEntities.GrayDay;
import com.heima.modules.operation.mapper.SiteExperienceMappers.BackgroundMusicMapper;
import com.heima.modules.operation.mapper.SiteExperienceMappers.GrayDayMapper;
import com.heima.modules.operation.service.impl.SiteExperienceServiceImpl;
import com.heima.modules.operation.vo.SiteExperienceVOs.GrayStatusVO;
import com.heima.modules.operation.vo.SiteExperienceVOs.PublicBackgroundMusicVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SiteExperienceServiceImplTest {
    @Mock private GrayDayMapper grayDayMapper;
    @Mock private BackgroundMusicMapper backgroundMusicMapper;
    @InjectMocks private SiteExperienceServiceImpl siteExperienceService;

    @Test
    void grayDayMethodsExposeEnabledCurrentDayAndReason() {
        GrayDay grayDay = new GrayDay();
        grayDay.setReason("国家公祭日");
        when(grayDayMapper.selectOne(any())).thenReturn(grayDay);

        GrayStatusVO status = siteExperienceService.currentGrayDay();

        assertTrue(status.gray());
        assertEquals("国家公祭日", status.reason());
        assertTrue(siteExperienceService.isGrayDay());
        assertEquals("国家公祭日", siteExperienceService.grayReason());
    }

    @Test
    void nonGrayDayHasNullReason() {
        when(grayDayMapper.selectOne(any())).thenReturn(null);

        assertFalse(siteExperienceService.currentGrayDay().gray());
        assertNull(siteExperienceService.currentGrayDay().reason());
        assertFalse(siteExperienceService.isGrayDay());
        assertNull(siteExperienceService.grayReason());
    }

    @Test
    void publicMusicContainsOnlyPlayerConfiguration() {
        BackgroundMusic music = new BackgroundMusic();
        music.setEnabled(1);
        music.setAudioUrl("https://rustfs.example.com/public/bgm.mp3");
        music.setPlayMode("MANUAL");
        music.setPlayerVisible(0);
        when(backgroundMusicMapper.selectById(1L)).thenReturn(music);

        PublicBackgroundMusicVO result = siteExperienceService.publicBackgroundMusic();

        assertTrue(result.enabled());
        assertEquals("https://rustfs.example.com/public/bgm.mp3", result.audioUrl());
        assertEquals("MANUAL", result.playMode());
        assertFalse(result.playerVisible());
    }

    @Test
    void enabledMusicRequiresAudioUrl() {
        BackgroundMusicRequest request = new BackgroundMusicRequest();
        request.setEnabled(true);
        request.setAudioUrl(" ");
        request.setPlayMode("AUTO");
        request.setPlayerVisible(true);

        ServiceException error = assertThrows(ServiceException.class,
                () -> siteExperienceService.saveBackgroundMusic(request));

        assertEquals("开启背景音乐时必须配置音频地址", error.getMessage());
        verify(backgroundMusicMapper, never()).selectById(anyLong());
    }
}
