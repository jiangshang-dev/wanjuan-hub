package com.heima.modules.resource.service;

import com.heima.exception.ServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class ResourceCryptoService {
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;
    private final String configuredKey;
    private final SecureRandom random = new SecureRandom();

    public ResourceCryptoService(@Value("${WANJUAN_RESOURCE_CRYPTO_KEY:}") String configuredKey) {
        this.configuredKey = configuredKey;
    }

    public String encrypt(String plaintext) {
        if (plaintext == null) return null;
        try {
            byte[] iv = new byte[IV_BYTES];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] result = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, result, 0, iv.length);
            System.arraycopy(encrypted, 0, result, iv.length, encrypted.length);
            return Base64.getEncoder().encodeToString(result);
        } catch (GeneralSecurityException e) {
            throw new ServiceException(500, "资源敏感信息加密失败");
        }
    }

    public String decrypt(String ciphertext) {
        if (ciphertext == null) return null;
        try {
            byte[] data = Base64.getDecoder().decode(ciphertext);
            if (data.length <= IV_BYTES) throw new IllegalArgumentException();
            byte[] iv = new byte[IV_BYTES];
            byte[] encrypted = new byte[data.length - IV_BYTES];
            System.arraycopy(data, 0, iv, 0, IV_BYTES);
            System.arraycopy(data, IV_BYTES, encrypted, 0, encrypted.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new ServiceException(500, "资源敏感信息解密失败");
        }
    }

    private SecretKeySpec key() {
        if (configuredKey == null || configuredKey.isBlank()) {
            throw new ServiceException(503, "资源加密密钥未配置，请配置WANJUAN_RESOURCE_CRYPTO_KEY");
        }
        try {
            byte[] key = MessageDigest.getInstance("SHA-256").digest(configuredKey.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(key, "AES");
        } catch (GeneralSecurityException e) {
            throw new ServiceException(500, "资源加密组件不可用");
        }
    }
}
