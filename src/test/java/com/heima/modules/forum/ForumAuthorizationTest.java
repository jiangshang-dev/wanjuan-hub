package com.heima.modules.forum;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.heima.exception.ServiceException;
import com.heima.modules.auth.mapper.UserAccountMapper;
import com.heima.modules.comment.service.ContentAuditService;
import com.heima.modules.file.service.FileService;
import com.heima.modules.forum.entity.ForumEntities.Moderator;
import com.heima.modules.forum.entity.ForumEntities.Topic;
import com.heima.modules.forum.mapper.ForumMappers.BoardMapper;
import com.heima.modules.forum.mapper.ForumMappers.ModeratorMapper;
import com.heima.modules.forum.mapper.ForumMappers.PostMapper;
import com.heima.modules.forum.mapper.ForumMappers.PostVersionMapper;
import com.heima.modules.forum.mapper.ForumMappers.SectionMapper;
import com.heima.modules.forum.mapper.ForumMappers.TopicMapper;
import com.heima.modules.forum.mapper.ForumMappers.TopicVersionMapper;
import com.heima.modules.forum.service.ForumService;
import com.heima.modules.system.mapper.SysAdminMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ForumAuthorizationTest {
    @Mock private SectionMapper sectionMapper;
    @Mock private BoardMapper boardMapper;
    @Mock private ModeratorMapper moderatorMapper;
    @Mock private TopicMapper topicMapper;
    @Mock private TopicVersionMapper topicVersionMapper;
    @Mock private PostMapper postMapper;
    @Mock private PostVersionMapper postVersionMapper;
    @Mock private UserAccountMapper userMapper;
    @Mock private SysAdminMapper adminMapper;
    @Mock private FileService fileService;
    @Mock private ContentAuditService auditService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @InjectMocks private ForumService forumService;

    @BeforeAll
    static void initTableMetadata() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Topic.class);
        TableInfoHelper.initTableInfo(assistant, Moderator.class);
    }

    @Test
    void authorCannotReadAnotherUsersWorkflowData() {
        Topic topic = topic(1L, 7L);
        when(topicMapper.selectById(9L)).thenReturn(topic);

        ServiceException error = assertThrows(ServiceException.class, () -> forumService.userTopic(9L, 2L));

        assertEquals(403, error.getCode());
        verify(topicVersionMapper, never()).selectOne(any());
    }

    @Test
    void moderatorCannotReadTopicOutsideAssignedBoard() {
        Topic topic = topic(2L, 1L);
        when(topicMapper.selectById(9L)).thenReturn(topic);
        when(moderatorMapper.selectCount(any())).thenReturn(1L, 0L);

        ServiceException error = assertThrows(ServiceException.class, () -> forumService.adminTopic(9L, 8L));

        assertEquals(403, error.getCode());
        verify(topicVersionMapper, never()).selectOne(any());
    }

    private Topic topic(Long boardId, Long authorId) {
        Topic topic = new Topic();
        topic.setId(9L);
        topic.setBoardId(boardId);
        topic.setAuthorId(authorId);
        topic.setDeleted(0);
        topic.setLatestVersionNo(1);
        return topic;
    }
}
