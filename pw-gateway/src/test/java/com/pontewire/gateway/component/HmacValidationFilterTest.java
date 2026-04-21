package com.pontewire.gateway.component;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.HexFormat;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class HmacValidationFilterTest {

    private HmacValidationFilter filter;
    private static final String SECRET = "test-secret";
    @BeforeEach
    void setUp() {
        filter = new HmacValidationFilter();
        ReflectionTestUtils.setField(filter, "secret", SECRET);
    }


    private String generateMacSignature(byte[] body) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(UTF_8), "HmacSHA256"));
        return "sha256=" + HexFormat.of().formatHex(mac.doFinal(body));
    }



    @Test
    void validSignature_shouldReturnTrue() throws Exception {
        byte[] body = """
                {"source":"stripe","data":{"id":"evt_123"}}
                """.getBytes(UTF_8);

        String validSig = generateMacSignature(body);

        boolean result = invokeIsValidSignature(filter, body, validSig);

        assertThat(result).isTrue();
    }

    @Test
    void wrongSecret_shouldReturnFalse() throws Exception {
        byte[] body = "{}".getBytes(UTF_8);
        String sigWithWrongSecret = "sha256=aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";

        boolean result = invokeIsValidSignature(filter, body, sigWithWrongSecret);

        assertThat(result).isFalse();
    }

    @Test
    void tamperedBody_shouldReturnFalse() throws Exception {
        byte[] originalBody = """
                {"source":"stripe","data":{"amount":100}}
                """.getBytes(UTF_8);
        byte[] tamperedBody = """
                {"source":"stripe","data":{"amount":99999}}
                """.getBytes(UTF_8);

        String sigOfOriginal = generateMacSignature(originalBody);


        boolean result = invokeIsValidSignature(filter, tamperedBody, sigOfOriginal);

        assertThat(result).isFalse();
    }

    @Test
    void emptyBody_shouldStillWork() throws Exception {
        byte[] body = new byte[0];

        String validSig = generateMacSignature(body);

        boolean result = invokeIsValidSignature(filter, body, validSig);

        assertThat(result).isTrue();
    }

    private boolean invokeIsValidSignature(HmacValidationFilter f, byte[] body, String sig)
            throws Exception {
        var method = HmacValidationFilter.class
                .getDeclaredMethod("isValidSignature", byte[].class, String.class);
        method.setAccessible(true);
        return (boolean) method.invoke(f, body, sig);
    }
}