package com.heima.modules.resource.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "会员资源领取结果")
public class ResourceClaimVO {
    @Schema(description = "资源ID") private Long resourceId;
    @Schema(description = "是否重复领取；重复领取不占额度且不返佣") private Boolean duplicated;
    @Schema(description = "本次实际产生的作者返佣，自己领取或重复领取为0") private BigDecimal commissionAmount;
    @Schema(description = "首次领取时间") private LocalDateTime claimedAt;
    @Schema(description = "领取后今日已用额度；重复领取时为当前已用额度") private Integer usedToday;
    @Schema(description = "领取后今日剩余额度") private Integer remainingToday;
}
