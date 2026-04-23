package com.pontewire.gateway.component;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class HmacValidationFilter implements WebFilter {


    @Value("${pontewire.hmac.secret}")
    private String secret;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        if (path.startsWith("/actuator")) {
            return chain.filter(exchange);
        }
        String signature = exchange.getRequest().getHeaders()
                .getFirst("X-Signature-SHA256"); //  Stripe-Signature

        if (signature == null) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

//buffer body (stream we can read only 1 time)
        return DataBufferUtils.join(exchange.getRequest().getBody())
                .flatMap(dataBuffer -> {
                    byte[] rawBody = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(rawBody);
                    DataBufferUtils.release(dataBuffer);

                    if (!isValidSignature(rawBody, signature)) {
                        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                        return exchange.getResponse().setComplete();
                    }

                    //rewrite request with buffered body -> then controller can read
                    ServerHttpRequest mutatedRequest = new ServerHttpRequestDecorator(exchange.getRequest()) {
                        @Override
                        public Flux<DataBuffer> getBody() {
                            return Flux.just(exchange.getResponse()
                                    .bufferFactory().wrap(rawBody));
                        }
                    };

                    return chain.filter(exchange.mutate().request(mutatedRequest).build());
                });
    }
//HMAC validation
private boolean isValidSignature(byte[] body, String receivedSig) {
    try {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"
        ));
        byte[] expectedBytes = mac.doFinal(body);
        String expected = "sha256=" + HexFormat.of().formatHex(expectedBytes);

        // тимчасово — видалиш після фіксу
        log.info("SECRET    : '{}'", secret);
        log.info("BODY SIZE : {} bytes", body.length);
        log.info("EXPECTED  : {}", expected);
        log.info("RECEIVED  : {}", receivedSig);

        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                receivedSig.getBytes(StandardCharsets.UTF_8)
        );
    } catch (Exception e) {
        log.error("HMAC error", e);
        return false;
    }
}
}
