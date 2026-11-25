package com.heima.modules.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.heima.exception.ServiceException;
import com.heima.infrastructure.sms.SmsSender;
import com.heima.modules.auth.dto.AuthDtos.MeResponse;
import com.heima.modules.auth.entity.UserAccount;
import com.heima.modules.auth.entity.UserProfile;
import com.heima.modules.auth.mapper.UserAccountMapper;
import com.heima.modules.auth.mapper.UserProfileMapper;
import com.heima.modules.auth.service.AuthService;
import com.heima.modules.auth.service.VerificationCodeService;
import com.heima.modules.audit.service.AuditService;
import com.heima.modules.file.service.FileService;
import com.heima.modules.user.dto.UserDtos.*;
import com.heima.modules.user.listener.UserProfileAuditListener;
import com.heima.security.service.RefreshTokenService;
import com.heima.utils.UserUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserAccountMapper userMapper;
    private final UserProfileMapper profileMapper;
    private final VerificationCodeService codeService;
    private final SmsSender smsSender;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshService;
    private final AuthService authService;
    private final AuditService auditService;
    private final FileService fileService;

    public MeResponse profile() { return authService.me(); }

    @Transactional
    public MeResponse updateProfile(ProfileRequest request) {
        Long id = UserUtil.requireFrontUser().getId();
        UserProfile profile = profileMapper.selectById(id);
        String nickname = request.nickname().trim();
        String signature = request.signature() == null ? null : request.signature().trim();
        Integer gender = request.gender() == null ? 0 : request.gender();
        LocalDateTime now = LocalDateTime.now();
        Long avatarFileId = request.removeAvatar() ? null
                : request.avatarFileId() == null ? profile.getAvatarFileId() : request.avatarFileId();
        if (request.removeAvatar() && request.avatarFileId() != null) {
            throw new ServiceException(400, "新头像与移除头像不能同时提交");
        }
        if (request.avatar() != null && !request.avatar().isBlank()
                && request.avatarFileId() == null && !Objects.equals(request.avatar(), profile.getAvatar())) {
            throw new ServiceException(400, "头像请先通过图片上传接口上传");
        }
        boolean moderatedContentChanged = !Objects.equals(nickname, profile.getNickname())
                || !Objects.equals(signature, profile.getSignature())
                || !Objects.equals(avatarFileId, profile.getAvatarFileId())
                || (request.removeAvatar() && (profile.getAvatarFileId() != null || profile.getAvatar() != null));
        if (!moderatedContentChanged) {
            profileMapper.update(null, new LambdaUpdateWrapper<UserProfile>()
                    .eq(UserProfile::getUserId, id)
                    .set(UserProfile::getBirthday, request.birthday())
                    .set(UserProfile::getGender, gender)
                    .set(UserProfile::getUpdateTime, now));
            return authService.me();
        }

        if (request.avatarFileId() != null && !request.removeAvatar()) {
            fileService.requireOwnedImages(List.of(request.avatarFileId()), id);
        }
        int version = (profile.getProfileVersion() == null ? 0 : profile.getProfileVersion()) + 1;
        int updated = profileMapper.update(null, new LambdaUpdateWrapper<UserProfile>()
                .eq(UserProfile::getUserId, id)
                .eq(UserProfile::getProfileVersion, profile.getProfileVersion())
                .set(UserProfile::getPendingNickname, nickname)
                .set(UserProfile::getPendingSignature, signature)
                .set(UserProfile::getPendingAvatarFileId, avatarFileId)
                .set(UserProfile::getPendingRemoveAvatar, request.removeAvatar())
                .set(UserProfile::getProfileAuditStatus, "PENDING")
                .set(UserProfile::getProfileAuditReason, null)
                .set(UserProfile::getBirthday, request.birthday())
                .set(UserProfile::getGender, gender)
                .set(UserProfile::getUpdateTime, now)
                .setSql("profile_version = profile_version + 1"));
        if (updated != 1) throw new ServiceException(409, "资料正在更新，请刷新后重试");
        if (request.avatarFileId() != null && !request.removeAvatar()) {
            fileService.bindReferences(List.of(request.avatarFileId()), UserProfileAuditListener.BIZ_TYPE, id, version);
        }
        auditService.submit(UserProfileAuditListener.BIZ_TYPE, id, version, id,
                nickname + "\n" + (signature == null ? "" : signature),
                avatarFileId == null || Objects.equals(avatarFileId, profile.getAvatarFileId())
                        ? List.of() : List.of(avatarFileId));
        return authService.me();
    }

    @Transactional
    public void changePassword(PasswordRequest request) {
        Long id=UserUtil.requireFrontUser().getId(); UserAccount user=userMapper.selectById(id);
        if (!passwordEncoder.matches(request.oldPassword(),user.getPassword())) throw new ServiceException(400,"旧密码错误");
        if (passwordEncoder.matches(request.newPassword(),user.getPassword())) throw new ServiceException(400,"新密码不能与旧密码相同");
        user.setPassword(passwordEncoder.encode(request.newPassword())); userMapper.updateById(user); refreshService.revokeAll("USER",id);
    }

    @Transactional
    public void bindEmail(BindEmailRequest request) {
        Long id=UserUtil.requireFrontUser().getId(); String email=request.email().toLowerCase(Locale.ROOT);
        if (userMapper.selectCount(new LambdaQueryWrapper<UserAccount>().eq(UserAccount::getEmail,email).ne(UserAccount::getId,id))>0) throw new ServiceException(409,"邮箱已被绑定");
        codeService.consumeEmail("BIND",email,request.emailCode()); UserAccount user=userMapper.selectById(id); user.setEmail(email); userMapper.updateById(user);
    }

    @Transactional
    public void bindPhone(BindPhoneRequest request) {
        if (!smsSender.isAvailable()) throw new ServiceException(503,"短信服务未配置");
        Long id=UserUtil.requireFrontUser().getId();
        if (userMapper.selectCount(new LambdaQueryWrapper<UserAccount>().eq(UserAccount::getPhone,request.phone()).ne(UserAccount::getId,id))>0) throw new ServiceException(409,"手机号已被绑定");
        codeService.consumeSms("BIND",request.phone(),request.smsCode()); UserAccount user=userMapper.selectById(id); user.setPhone(request.phone()); userMapper.updateById(user);
    }
}
