package com.heima.modules.resource;

import com.heima.exception.ServiceException;
import com.heima.modules.resource.entity.ResourceDiskType;
import com.heima.modules.resource.service.ResourceCryptoService;
import com.heima.modules.resource.service.ResourceUrlValidator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ResourceCryptoServiceTest {
    private final ResourceUrlValidator urlValidator = new ResourceUrlValidator();

    @Test
    void urlAndExtractionCodeAreEncryptedReversibly() {
        ResourceCryptoService crypto = new ResourceCryptoService("test-only-resource-key");
        String url = "https://pan.baidu.com/s/abc";
        String first = crypto.encrypt(url);
        String second = crypto.encrypt(url);

        assertNotEquals(url, first);
        assertNotEquals(first, second);
        assertEquals(url, crypto.decrypt(first));
        assertEquals("1234", crypto.decrypt(crypto.encrypt("1234")));
    }

    @Test
    void missingKeyFailsManagementOrAccessClearly() {
        ResourceCryptoService crypto = new ResourceCryptoService("");
        ServiceException error = assertThrows(ServiceException.class, () -> crypto.encrypt("https://pan.baidu.com/s/abc"));
        assertEquals(503, error.getCode());
    }

    @Test
    void whitelistRequiresHttpsAndExactDomainOrSubdomain() {
        ResourceDiskType disk = new ResourceDiskType();
        disk.setStatus(1);
        disk.setAllowedHosts("pan.baidu.com");
        assertEquals("https://pan.baidu.com/s/abc", urlValidator.validate("https://pan.baidu.com/s/abc", disk));
        assertEquals("https://sub.pan.baidu.com/s/abc", urlValidator.validate("https://sub.pan.baidu.com/s/abc", disk));
        assertThrows(ServiceException.class, () -> urlValidator.validate("http://pan.baidu.com/s/abc", disk));
        assertThrows(ServiceException.class, () -> urlValidator.validate("https://pan.baidu.com.evil.test/s/abc", disk));
        assertThrows(ServiceException.class, () -> urlValidator.validate("https://evil.test/s/abc", disk));
    }
}
