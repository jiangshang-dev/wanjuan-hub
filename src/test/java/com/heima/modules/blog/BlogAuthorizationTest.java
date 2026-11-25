package com.heima.modules.blog;

import com.heima.exception.ServiceException;
import com.heima.modules.blog.converter.BlogConverter;
import com.heima.modules.blog.entity.BlogArticle;
import com.heima.modules.blog.mapper.BlogArticleMapper;
import com.heima.modules.blog.mapper.BlogArticleTagMapper;
import com.heima.modules.blog.mapper.BlogArticleVersionMapper;
import com.heima.modules.blog.mapper.BlogCategoryMapper;
import com.heima.modules.blog.mapper.BlogTagMapper;
import com.heima.modules.blog.service.impl.BlogServiceImpl;
import com.heima.modules.audit.service.AuditService;
import com.heima.modules.file.service.FileService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BlogAuthorizationTest {
    @Mock private BlogArticleMapper articleMapper;
    @Mock private BlogArticleVersionMapper versionMapper;
    @Mock private BlogCategoryMapper categoryMapper;
    @Mock private BlogTagMapper tagMapper;
    @Mock private BlogArticleTagMapper articleTagMapper;
    @Mock private BlogConverter converter;
    @Mock private FileService fileService;
    @Mock private AuditService auditService;
    @InjectMocks private BlogServiceImpl blogService;

    @Test
    void authorCannotReadAnotherUsersWorkflowData() {
        BlogArticle article = new BlogArticle();
        article.setId(9L);
        article.setAuthorId(1L);
        article.setDeleted(0);
        when(articleMapper.selectById(9L)).thenReturn(article);

        ServiceException error = assertThrows(ServiceException.class, () -> blogService.userDetail(9L, 2L));

        assertEquals(403, error.getCode());
    }
}
