package com.devedu.learningplatform.infrastructure.security;

import com.devedu.learningplatform.application.exception.InvalidTokenException;
import com.devedu.learningplatform.application.port.out.AccessToken;
import com.devedu.learningplatform.application.port.out.TokenProvider;
import com.devedu.learningplatform.application.security.AuthenticatedUser;
import com.devedu.learningplatform.domain.model.User;
import com.devedu.learningplatform.domain.model.UserRole;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class JwtTokenProvider implements TokenProvider {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final Base64.Encoder BASE64_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder BASE64_DECODER = Base64.getUrlDecoder();

    private final ObjectMapper objectMapper;
    private final byte[] secret;
    private final Duration expiration;
    private final Clock clock;

    @Autowired
    public JwtTokenProvider(
            ObjectMapper objectMapper,
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.expiration}") Duration expiration
    ) {
        this(objectMapper, secret, expiration, Clock.systemUTC());
    }

    JwtTokenProvider(ObjectMapper objectMapper, String secret, Duration expiration, Clock clock) {
        this.objectMapper = objectMapper;
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.expiration = expiration;
        this.clock = clock;

        if (this.secret.length < 32) {
            throw new IllegalArgumentException("JWT secret must contain at least 32 UTF-8 bytes");
        }
        if (expiration.isZero() || expiration.isNegative()) {
            throw new IllegalArgumentException("JWT expiration must be positive");
        }
    }

    @Override
    public AccessToken issue(User user) {
        var issuedAt = Instant.now(clock);
        var expiresAt = issuedAt.plus(expiration);

        Map<String, Object> header = Map.of("alg", "HS256", "typ", "JWT");
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sub", user.id().toString());
        payload.put("email", user.email());
        payload.put("role", user.role().name());
        payload.put("iat", issuedAt.getEpochSecond());
        payload.put("exp", expiresAt.getEpochSecond());

        var encodedHeader = encodeJson(header);
        var encodedPayload = encodeJson(payload);
        var unsignedToken = encodedHeader + "." + encodedPayload;
        var signature = BASE64_ENCODER.encodeToString(sign(unsignedToken));
        return new AccessToken(unsignedToken + "." + signature, expiration.toSeconds());
    }

    @Override
    public AuthenticatedUser verify(String token) {
        try {
            var parts = token.split("\\.", -1);
            if (parts.length != 3) {
                throw new InvalidTokenException();
            }

            var unsignedToken = parts[0] + "." + parts[1];
            var providedSignature = BASE64_DECODER.decode(parts[2]);
            if (!BASE64_ENCODER.encodeToString(providedSignature).equals(parts[2])
                    || !MessageDigest.isEqual(sign(unsignedToken), providedSignature)) {
                throw new InvalidTokenException();
            }

            JsonNode header = objectMapper.readTree(BASE64_DECODER.decode(parts[0]));
            if (!"HS256".equals(header.path("alg").asText())
                    || !"JWT".equals(header.path("typ").asText())) {
                throw new InvalidTokenException();
            }

            JsonNode payload = objectMapper.readTree(BASE64_DECODER.decode(parts[1]));
            var issuedAt = payload.path("iat").asLong(0);
            var expiresAt = payload.path("exp").asLong(0);
            var now = Instant.now(clock).getEpochSecond();
            if (issuedAt <= 0 || issuedAt > now || expiresAt <= issuedAt || expiresAt <= now) {
                throw new InvalidTokenException();
            }

            return new AuthenticatedUser(
                    UUID.fromString(payload.path("sub").asText()),
                    User.normalizeEmail(payload.path("email").asText()),
                    UserRole.valueOf(payload.path("role").asText())
            );
        } catch (InvalidTokenException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new InvalidTokenException();
        }
    }

    private String encodeJson(Map<String, Object> value) {
        try {
            return BASE64_ENCODER.encodeToString(objectMapper.writeValueAsBytes(value));
        } catch (Exception exception) {
            throw new IllegalStateException("Could not create JWT", exception);
        }
    }

    private byte[] sign(String content) {
        try {
            var mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
            return mac.doFinal(content.getBytes(StandardCharsets.US_ASCII));
        } catch (Exception exception) {
            throw new IllegalStateException("Could not sign JWT", exception);
        }
    }
}
