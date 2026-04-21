package com.pontewire.gateway.controller;

import com.pontewire.gateway.component.HmacValidationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.HexFormat;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@WebFluxTest(controllers = PonteIngressController.class)
@Import(HmacValidationFilter.class)
class HmacValidationFilterWebTest {

    @Autowired
    private WebTestClient webClient;

    @MockitoBean
    private KafkaTemplate<String, String> kafkaTemplate;

    @Value("${pontewire.hmac.secret}")
    private String secret;

    private static final String STRIPE_URI = "/api/v1/bridge/stripe";
    private static final String GITHUB_URI = "/api/v1/bridge/github";

    private String signBody(String body) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(UTF_8), "HmacSHA256"));
        return "sha256=" + HexFormat.of().formatHex(mac.doFinal(body.getBytes(UTF_8)));
    }

    @Test
    void requestWithValidSignature_shouldReturn202() throws Exception {
        String body = """
                {"source":"stripe","data":{"id":"evt_123"}}
                """;

        webClient.post()
                .uri(STRIPE_URI)
                .header("X-Signature-SHA256", signBody(body))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus().isAccepted();
    }

    @Test
    void requestWithoutSignature_shouldReturn401() {
        webClient.post()
                .uri(STRIPE_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"source":"stripe","data":{}}
                        """)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void requestWithWrongSignature_shouldReturn401() {
        webClient.post()
                .uri(STRIPE_URI)
                .header("X-Signature-SHA256", "sha256=wrongsignature")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"source":"stripe","data":{}}
                        """)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void validRequest_shouldCallKafkaSend() throws Exception {
        String body = """
                {"source":"github","data":{"ref":"main"}}
                """;

        webClient.post()
                .uri(GITHUB_URI)
                .header("X-Signature-SHA256", signBody(body))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus().isAccepted();

        verify(kafkaTemplate, times(1)).send(eq("pw.incoming"), eq("github"), any());
    }
}