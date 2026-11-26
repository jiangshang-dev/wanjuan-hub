package com.heima.modules.operation.service;

import com.heima.common.PageResult;
import com.heima.modules.operation.dto.FeedbackDtos.CreateRequest;
import com.heima.modules.operation.dto.FeedbackDtos.ReplyRequest;
import com.heima.modules.operation.vo.FeedbackVO;

public interface FeedbackService {
    String create(Long userId, CreateRequest request);
    PageResult<FeedbackVO> userPage(Long userId, String status, long page, long size);
    FeedbackVO userDetail(Long userId, Long id);

    PageResult<FeedbackVO> adminPage(String status, Long userId, String keyword, long page, long size);
    void reply(Long id, ReplyRequest request, Long adminId);
    void close(Long id);
}
