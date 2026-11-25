package com.heima.modules.message.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.heima.modules.auth.entity.UserProfile;
import com.heima.modules.auth.mapper.UserProfileMapper;
import com.heima.modules.message.event.UserNotificationEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class MentionService {
    private static final Pattern MENTION = Pattern.compile("(?<![\\w@])@([\\p{L}\\p{N}_-]{1,50})");
    private final UserProfileMapper profileMapper;
    private final ApplicationEventPublisher eventPublisher;

    public void publishApprovedMentions(String content, Long authorId, String bizType, Long bizId, Integer versionNo) {
        if (content == null || content.isBlank() || bizId == null) return;
        Set<String> nicknames = new LinkedHashSet<>();
        Matcher matcher = MENTION.matcher(content);
        while (matcher.find()) nicknames.add(matcher.group(1));
        if (nicknames.isEmpty()) return;
        profileMapper.selectList(new LambdaQueryWrapper<UserProfile>()
                        .in(UserProfile::getNickname, nicknames).last("LIMIT 100"))
                .stream().filter(profile -> !profile.getUserId().equals(authorId))
                .forEach(profile -> eventPublisher.publishEvent(new UserNotificationEvent(
                        profile.getUserId(), "INTERACTION", "有人提及了你", "你在新发布的内容中被提及",
                        bizType, String.valueOf(bizId), "MENTION:" + bizType + ":" + bizId + ":" + versionNo + ":" + profile.getUserId())));
    }
}
