package com.heima.security.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.heima.exception.ServiceException;
import com.heima.modules.auth.entity.RefreshTokenEntity;
import com.heima.modules.auth.entity.UserAccount;
import com.heima.modules.auth.mapper.RefreshTokenMapper;
import com.heima.modules.auth.mapper.UserAccountMapper;
import com.heima.modules.system.entity.SysAdmin;
import com.heima.modules.system.mapper.SysAdminMapper;
import com.heima.security.token.JwtService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    private final RefreshTokenMapper mapper;
    private final UserAccountMapper userMapper;
    private final SysAdminMapper adminMapper;
    private final JwtService jwt;

    @Transactional
    public IssuedTokens issue(String type,Long id,int version) { return issue(type,id,version,UUID.randomUUID().toString()); }

    private IssuedTokens issue(String type,Long id,int version,String family) {
        JwtService.GeneratedRefresh generated=jwt.refresh(type,id,version);
        RefreshTokenEntity entity=new RefreshTokenEntity();
        entity.setTokenId(generated.tokenId()); entity.setUserType(type); entity.setUserId(id); entity.setFamilyId(family);
        entity.setTokenHash(jwt.hash(generated.token())); entity.setExpiresAt(generated.expiresAt()); entity.setCreateTime(LocalDateTime.now());
        mapper.insert(entity);
        return new IssuedTokens(jwt.access(type,id,version),generated.token(),family,generated.tokenId());
    }

    @Transactional
    public IssuedTokens rotate(String raw,String expectedType) {
        Claims claims=jwt.parse(raw,JwtService.REFRESH);
        String type=claims.get("typ",String.class);
        if (!expectedType.equals(type)) throw new ServiceException(401,"令牌身份不匹配");
        RefreshTokenEntity old=mapper.selectOne(new LambdaQueryWrapper<RefreshTokenEntity>().eq(RefreshTokenEntity::getTokenId,claims.getId()));
        if (old==null || !MessageDigest.isEqual(old.getTokenHash().getBytes(StandardCharsets.UTF_8),jwt.hash(raw).getBytes(StandardCharsets.UTF_8))) throw new ServiceException(401,"刷新令牌无效");
        if (old.getRevokedAt()!=null) {
            mapper.revokeFamily(old.getFamilyId(),LocalDateTime.now());
            throw new ServiceException(401,"检测到刷新令牌重用，请重新登录");
        }
        if (old.getExpiresAt().isBefore(LocalDateTime.now()) || !type.equals(old.getUserType()) || !claims.getSubject().equals(String.valueOf(old.getUserId()))) throw new ServiceException(401,"刷新令牌已失效");
        int version=checkAccount(type,old.getUserId(),claims.get("ver",Integer.class));
        JwtService.GeneratedRefresh generated=jwt.refresh(type,old.getUserId(),version);
        if (mapper.rotate(old.getTokenId(),generated.tokenId(),LocalDateTime.now())!=1) {
            mapper.revokeFamily(old.getFamilyId(),LocalDateTime.now()); throw new ServiceException(401,"刷新令牌已使用");
        }
        RefreshTokenEntity next=new RefreshTokenEntity();
        next.setTokenId(generated.tokenId()); next.setUserType(type); next.setUserId(old.getUserId()); next.setFamilyId(old.getFamilyId());
        next.setTokenHash(jwt.hash(generated.token())); next.setExpiresAt(generated.expiresAt()); next.setCreateTime(LocalDateTime.now());
        mapper.insert(next);
        return new IssuedTokens(jwt.access(type,old.getUserId(),version),generated.token(),old.getFamilyId(),generated.tokenId());
    }

    @Transactional
    public void logout(String raw,String expectedType) {
        Claims claims=jwt.parse(raw,JwtService.REFRESH);
        String type=claims.get("typ",String.class); Long id=Long.valueOf(claims.getSubject());
        if (!expectedType.equals(type)) throw new ServiceException(401,"令牌身份不匹配");
        RefreshTokenEntity entity=mapper.selectOne(new LambdaQueryWrapper<RefreshTokenEntity>().eq(RefreshTokenEntity::getTokenId,claims.getId()));
        if (entity==null || !MessageDigest.isEqual(entity.getTokenHash().getBytes(StandardCharsets.UTF_8),jwt.hash(raw).getBytes(StandardCharsets.UTF_8))) throw new ServiceException(401,"刷新令牌无效");
        revokeAll(type,id);
    }

    public void revokeAll(String type,Long id) {
        mapper.revokeAll(type,id,LocalDateTime.now());
        if ("USER".equals(type)) userMapper.incrementTokenVersion(id); else adminMapper.incrementTokenVersion(id);
    }

    public int checkAccount(String type,Long id,int version) {
        if ("USER".equals(type)) {
            UserAccount user=userMapper.selectById(id);
            if (user==null || user.getStatus()!=1 || user.getTokenVersion()!=version) throw new ServiceException(401,"登录状态已失效");
            return user.getTokenVersion();
        }
        SysAdmin admin=adminMapper.selectById(id);
        if (admin==null || admin.getStatus()!=1 || admin.getTokenVersion()!=version) throw new ServiceException(401,"登录状态已失效");
        return admin.getTokenVersion();
    }

    public record IssuedTokens(String accessToken,String refreshToken,String familyId,String tokenId) {}
}
