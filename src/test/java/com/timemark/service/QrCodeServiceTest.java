package com.timemark.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class QrCodeServiceTest {

    private QrCodeService qrCodeService;

    @BeforeEach
    void setUp() {
        qrCodeService = new QrCodeService();
        ReflectionTestUtils.setField(qrCodeService, "secret", "test-secret-key");
    }

    @Test
    void generatedTodayTokenIsValid() {
        String token = qrCodeService.generateDailyToken();
        assertThat(qrCodeService.isValid(token)).isTrue();
    }

    @Test
    void tamperedTokenIsRejected() {
        String token = qrCodeService.generateDailyToken();
        String tampered = token.substring(0, token.length() - 1) + "0";
        assertThat(qrCodeService.isValid(tampered)).isFalse();
    }

    @Test
    void tokenSignedWithDifferentSecretIsRejected() {
        String token = qrCodeService.generateDailyToken();

        QrCodeService differentSecretService = new QrCodeService();
        ReflectionTestUtils.setField(differentSecretService, "secret", "a-completely-different-secret");

        assertThat(differentSecretService.isValid(token)).isFalse();
    }

    @Test
    void malformedTokenIsRejected() {
        assertThat(qrCodeService.isValid("not-a-real-token")).isFalse();
        assertThat(qrCodeService.isValid("")).isFalse();
        assertThat(qrCodeService.isValid(null)).isFalse();
    }

    @Test
    void base64PngStartsWithDataUrlPrefix() {
        String token = qrCodeService.generateDailyToken();
        String image = qrCodeService.toBase64Png(token);
        assertThat(image).startsWith("data:image/png;base64,");
    }
}
