package com.heima.modules.comment;

import com.heima.exception.ServiceException;
import com.heima.modules.comment.dto.CommentDtos.CreateRequest;
import com.heima.modules.comment.entity.CommentEntities.Comment;
import com.heima.modules.comment.mapper.CommentMappers.CommentMapper;
import com.heima.modules.comment.mapper.CommentMappers.VersionMapper;
import com.heima.modules.comment.service.CommentService;
import com.heima.modules.comment.service.ContentAuditService;
import com.heima.modules.comment.service.ContentTargetService;
import com.heima.modules.file.service.FileService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {
    @Mock private CommentMapper commentMapper;
    @Mock private VersionMapper versionMapper;
    @Mock private ContentTargetService targetService;
    @Mock private ContentAuditService auditService;
    @Mock private FileService fileService;
    @InjectMocks private CommentService commentService;

    @Test
    void parentCommentMustBelongToSameTarget() {
        CreateRequest request = new CreateRequest();
        request.setTargetType("BLOG");
        request.setTargetId(10L);
        request.setParentId(99L);
        request.setContent("reply");
        Comment parent = new Comment();
        parent.setId(99L);
        parent.setTargetType("FORUM");
        parent.setTargetId(10L);
        parent.setAuthorId(2L);
        parent.setDeleted(0);
        when(commentMapper.selectById(99L)).thenReturn(parent);

        ServiceException error = assertThrows(ServiceException.class, () -> commentService.create(request, 1L));

        assertEquals(400, error.getCode());
        verify(commentMapper, never()).insert(any(Comment.class));
        verify(auditService, never()).submit(any(), any(), any(), any(), any(), any());
    }
}
