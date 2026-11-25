package com.heima.modules.resource.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.heima.common.PageResult;
import com.heima.exception.ServiceException;
import com.heima.modules.resource.dto.ResourceInvalidReportDTO;
import com.heima.modules.resource.dto.ResourceInvalidReportHandleDTO;
import com.heima.modules.resource.dto.ResourceReportQueryDTO;
import com.heima.modules.resource.entity.ResourceInfo;
import com.heima.modules.resource.entity.ResourceInvalidReport;
import com.heima.modules.resource.entity.ResourceLink;
import com.heima.modules.resource.enums.ResourceStatus;
import com.heima.modules.resource.mapper.ResourceInfoMapper;
import com.heima.modules.resource.mapper.ResourceInvalidReportMapper;
import com.heima.modules.resource.mapper.ResourceLinkMapper;
import com.heima.modules.resource.service.ResourceReportService;
import com.heima.modules.resource.vo.ResourceInvalidReportVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ResourceReportServiceImpl implements ResourceReportService {
    private final ResourceInvalidReportMapper reportMapper;
    private final ResourceInfoMapper resourceMapper;
    private final ResourceLinkMapper linkMapper;

    @Override
    @Transactional
    public Long create(Long resourceId, ResourceInvalidReportDTO request, Long reporterId) {
        ResourceInfo resource = resourceMapper.selectOne(new LambdaQueryWrapper<ResourceInfo>().eq(ResourceInfo::getId, resourceId)
                .eq(ResourceInfo::getDeleted, 0).eq(ResourceInfo::getOnline, 1).eq(ResourceInfo::getInvalid, 0));
        if (resource == null) throw new ServiceException(404, "资源不存在或未公开");
        if (request.getLinkId() != null) {
            ResourceLink link = linkMapper.selectOne(new LambdaQueryWrapper<ResourceLink>().eq(ResourceLink::getId, request.getLinkId())
                    .eq(ResourceLink::getResourceId, resourceId).eq(ResourceLink::getVersionNo, resource.getPublishedVersionNo()));
            if (link == null) throw new ServiceException(400, "链接不属于该资源公开版本");
        }
        ResourceInvalidReport report = new ResourceInvalidReport();
        report.setResourceId(resourceId);
        report.setLinkId(request.getLinkId());
        report.setReporterId(reporterId);
        report.setReason(request.getReason().trim());
        report.setStatus("PENDING");
        reportMapper.insert(report);
        return report.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<ResourceInvalidReportVO> page(ResourceReportQueryDTO query) {
        long pageNo = Math.max(1, query.getPage());
        long pageSize = Math.min(100, Math.max(1, query.getSize()));
        Page<ResourceInvalidReport> page = reportMapper.selectPage(new Page<>(pageNo, pageSize), new LambdaQueryWrapper<ResourceInvalidReport>()
                .eq(query.getStatus() != null && !query.getStatus().isBlank(), ResourceInvalidReport::getStatus, query.getStatus())
                .eq(query.getResourceId() != null, ResourceInvalidReport::getResourceId, query.getResourceId()).orderByDesc(ResourceInvalidReport::getId));
        return PageResult.of(page.getRecords().stream().map(item -> {
            ResourceInvalidReportVO vo = new ResourceInvalidReportVO();
            BeanUtils.copyProperties(item, vo);
            return vo;
        }).toList(), page.getTotal(), pageNo, pageSize);
    }

    @Override
    @Transactional
    public void handle(Long id, ResourceInvalidReportHandleDTO request, Long handlerId) {
        ResourceInvalidReport report = reportMapper.selectById(id);
        if (report == null) throw new ServiceException(404, "举报不存在");
        int updated = reportMapper.update(null, new LambdaUpdateWrapper<ResourceInvalidReport>().eq(ResourceInvalidReport::getId, id)
                .eq(ResourceInvalidReport::getStatus, "PENDING").set(ResourceInvalidReport::getStatus, request.getStatus())
                .set(ResourceInvalidReport::getHandlerId, handlerId).set(ResourceInvalidReport::getHandleNote, request.getHandleNote())
                .set(ResourceInvalidReport::getHandledAt, LocalDateTime.now()));
        if (updated == 0) throw new ServiceException(409, "举报已被处理");
        if (!"CONFIRMED".equals(request.getStatus())) return;
        if (report.getLinkId() != null) {
            linkMapper.update(null, new LambdaUpdateWrapper<ResourceLink>().eq(ResourceLink::getId, report.getLinkId())
                    .eq(ResourceLink::getResourceId, report.getResourceId()).set(ResourceLink::getStatus, "DISABLED"));
        }
        resourceMapper.update(null, new LambdaUpdateWrapper<ResourceInfo>().eq(ResourceInfo::getId, report.getResourceId())
                .eq(ResourceInfo::getDeleted, 0).set(ResourceInfo::getOnline, 0).set(ResourceInfo::getInvalid, 1)
                .set(ResourceInfo::getStatus, ResourceStatus.INVALID.name()).set(ResourceInfo::getAuditReason, "资源失效举报已确认"));
    }
}
