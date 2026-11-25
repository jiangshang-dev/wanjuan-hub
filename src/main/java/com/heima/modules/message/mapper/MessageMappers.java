package com.heima.modules.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.heima.modules.message.entity.MessageEntities.Notification;
import com.heima.modules.message.entity.MessageEntities.Preference;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

public final class MessageMappers {
    private MessageMappers() {}

    @Mapper
    public interface NotificationMapper extends BaseMapper<Notification> {
        @Update("UPDATE message_notification SET read_flag=1,read_time=#{readTime} WHERE user_id=#{userId} AND read_flag=0")
        int readAll(@Param("userId") Long userId, @Param("readTime") LocalDateTime readTime);
    }

    @Mapper
    public interface PreferenceMapper extends BaseMapper<Preference> {
        @Insert("INSERT IGNORE INTO message_preference(user_id) VALUES(#{userId})")
        int insertIgnore(@Param("userId") Long userId);
    }
}
