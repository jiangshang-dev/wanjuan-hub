package com.heima.infrastructure.aliyun.audit;

import com.aliyun.green20220302.Client;
import com.aliyun.green20220302.models.ImageModerationRequest;
import com.aliyun.green20220302.models.ImageModerationResponseBody;
import com.aliyun.green20220302.models.TextModerationRequest;
import com.aliyun.green20220302.models.TextModerationResponseBody;
import com.aliyun.teaopenapi.models.Config;
import com.heima.config.AliyunGreenProperties;
import com.heima.exception.ServiceException;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;

@Component
public class AliyunContentAuditGateway implements ContentAuditGateway {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private final AliyunGreenProperties properties;
    private volatile Client client;

    public AliyunContentAuditGateway(AliyunGreenProperties properties) {
        this.properties = properties;
    }

    @Override
    public AuditGatewayResult reviewText(String content) {
        if (!enabled()) {
            return new AuditGatewayResult(GatewayDecision.REVIEW, "自动审核未启用，请人工审核", null);
        }
        try {
            TextModerationRequest request = new TextModerationRequest();
            request.setService("comment_detection");
            request.setServiceParameters(JSON.writeValueAsString(Map.of("content", content)));
            TextModerationResponseBody body = client().textModeration(request).getBody();
            if (body == null || body.getCode() == null || body.getCode() != 200 || body.getData() == null) {
                throw new ServiceException(502, "阿里云文本审核返回异常");
            }
            String labels = body.getData().getLabels();
            String reason = body.getData().getReason();
            GatewayDecision decision = labels == null || labels.isBlank() ? GatewayDecision.PASS : GatewayDecision.REJECT;
            return new AuditGatewayResult(decision, decision == GatewayDecision.PASS ? "通过" : reason, body.getRequestId());
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException(502, "阿里云文本审核调用失败", e);
        }
    }

    @Override
    public AuditGatewayResult reviewImage(String imageUrl) {
        if (!enabled()) {
            return new AuditGatewayResult(GatewayDecision.REVIEW, "自动审核未启用，请人工审核", null);
        }
        try {
            ImageModerationRequest request = new ImageModerationRequest();
            request.setService("baselineCheck");
            request.setServiceParameters(JSON.writeValueAsString(Map.of("imageUrl", imageUrl)));
            ImageModerationResponseBody body = client().imageModeration(request).getBody();
            if (body == null || body.getCode() == null || body.getCode() != 200 || body.getData() == null) {
                throw new ServiceException(502, "阿里云图片审核返回异常");
            }
            String risk = body.getData().getRiskLevel() == null ? "" : body.getData().getRiskLevel().toLowerCase();
            List<ImageModerationResponseBody.ImageModerationResponseBodyDataResult> results = body.getData().getResult();
            boolean noLabel = results == null || results.isEmpty()
                    || results.stream().allMatch(item -> "nonLabel".equalsIgnoreCase(item.getLabel()));
            GatewayDecision decision;
            if (("none".equals(risk) || risk.isBlank()) && noLabel) {
                decision = GatewayDecision.PASS;
            } else if ("high".equals(risk)) {
                decision = GatewayDecision.REJECT;
            } else {
                decision = GatewayDecision.REVIEW;
            }
            String reason = decision == GatewayDecision.PASS ? "通过" : describe(results, risk);
            return new AuditGatewayResult(decision, reason, body.getRequestId());
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException(502, "阿里云图片审核调用失败", e);
        }
    }

    private String describe(List<ImageModerationResponseBody.ImageModerationResponseBodyDataResult> results, String risk) {
        if (results == null || results.isEmpty()) {
            return "图片风险等级：" + risk;
        }
        String text = results.stream().map(item -> item.getDescription() == null ? item.getLabel() : item.getDescription())
                .filter(value -> value != null && !value.isBlank()).distinct().reduce((a, b) -> a + "；" + b).orElse("图片需人工审核");
        return text.length() > 480 ? text.substring(0, 480) : text;
    }

    private boolean enabled() {
        return properties.isEnabled() && properties.getAccessKeyId() != null && !properties.getAccessKeyId().isBlank()
                && properties.getAccessKeySecret() != null && !properties.getAccessKeySecret().isBlank();
    }

    private Client client() throws Exception {
        if (client == null) {
            synchronized (this) {
                if (client == null) {
                    client = new Client(new Config().setAccessKeyId(properties.getAccessKeyId())
                            .setAccessKeySecret(properties.getAccessKeySecret()).setEndpoint(properties.getEndpoint()));
                }
            }
        }
        return client;
    }
}
