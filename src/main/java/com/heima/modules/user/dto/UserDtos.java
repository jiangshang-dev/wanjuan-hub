package com.heima.modules.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

public final class UserDtos {
    private UserDtos() {}
    @Schema(description="资料修改请求") public record ProfileRequest(
            @Schema(description="昵称，审核通过后公开") @NotBlank @Size(max=50) String nickname,
            @Schema(description="旧版头像URL兼容字段；新头像请使用avatarFileId") @Size(max=500) String avatar,
            @Schema(description="通过图片上传接口获得的头像文件ID") Long avatarFileId,
            @Schema(description="是否移除头像") boolean removeAvatar,
            @Schema(description="个性签名，审核通过后公开") @Size(max=255) String signature,
            @Schema(description="生日") @Past LocalDate birthday,
            @Schema(description="性别：0未知/1男/2女") @Min(0) @Max(2) Integer gender) {}
    @Schema(description="内容赞踩接收开关") public record InteractionSettingsRequest(
            @Schema(description="是否允许他人点赞自己的博客、帖子、回复和评论") @NotNull Boolean allowLike,
            @Schema(description="是否允许他人点踩自己的博客、帖子、回复和评论") @NotNull Boolean allowDislike) {}
    @Schema(description="修改密码请求") public record PasswordRequest(@Schema(description="旧密码") @NotBlank String oldPassword,@Schema(description="新密码") @NotBlank @Size(min=8,max=72) String newPassword) {}
    @Schema(description="绑定邮箱请求") public record BindEmailRequest(@Schema(description="邮箱") @NotBlank @Email String email,@Schema(description="邮箱验证码") @NotBlank String emailCode) {}
    @Schema(description="绑定手机请求") public record BindPhoneRequest(@Schema(description="手机号") @NotBlank @Pattern(regexp="^1[3-9]\\d{9}$") String phone,@Schema(description="短信验证码") @NotBlank String smsCode) {}
}
