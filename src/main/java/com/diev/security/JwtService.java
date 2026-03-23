package com.diev.security;

import com.diev.entity.User;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class JwtService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String JWT_HEADER_JSON = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";

    private final JwtProperties properties;
    private final ObjectMapper objectMapper;

    public JwtService(JwtProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public String generateToken(User user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(properties.getExpiration());

        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("iss", properties.getIssuer());
        claims.put("sub", user.getId().toString());
        claims.put("email", user.getEmail());
        claims.put("role", user.getRole());
        claims.put("iat", now.getEpochSecond());
        claims.put("exp", expiresAt.getEpochSecond());

        try {
            String headerPart = base64UrlEncode(JWT_HEADER_JSON.getBytes(StandardCharsets.UTF_8));
            String payloadPart = base64UrlEncode(objectMapper.writeValueAsBytes(claims));
            String signingInput = headerPart + "." + payloadPart;
            String signaturePart = base64UrlEncode(sign(signingInput));
            return signingInput + "." + signaturePart;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to generate JWT.", ex);
        }
    }

    public Optional<JwtPrincipal> parse(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return Optional.empty();
            }

            if (!isValidSignature(parts[0], parts[1], parts[2])) {
                return Optional.empty();
            }

            JsonNode header = readJson(parts[0]);
            if (!"HS256".equals(header.path("alg").asText(null))) {
                return Optional.empty();
            }

            JsonNode payload = readJson(parts[1]);
            if (!properties.getIssuer().equals(payload.path("iss").asText(null))) {
                return Optional.empty();
            }

            long exp = payload.path("exp").asLong(0L);
            if (exp <= 0L || Instant.now().isAfter(Instant.ofEpochSecond(exp))) {
                return Optional.empty();
            }

            String sub = payload.path("sub").asText(null);
            String email = payload.path("email").asText(null);
            String role = payload.path("role").asText(null);

            if (sub == null || sub.isBlank() || email == null || email.isBlank() || role == null || role.isBlank()) {
                return Optional.empty();
            }

            return Optional.of(new JwtPrincipal(UUID.fromString(sub), email, role));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    private boolean isValidSignature(String headerPart, String payloadPart, String signaturePart) throws Exception {
        String signingInput = headerPart + "." + payloadPart;
        byte[] expectedSignature = sign(signingInput);
        byte[] actualSignature = Base64.getUrlDecoder().decode(signaturePart);
        return MessageDigest.isEqual(expectedSignature, actualSignature);
    }

    private byte[] sign(String value) throws Exception {
        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        SecretKeySpec keySpec = new SecretKeySpec(properties.getSecret().getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
        mac.init(keySpec);
        return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
    }

    private JsonNode readJson(String base64UrlPart) throws Exception {
        byte[] decoded = Base64.getUrlDecoder().decode(base64UrlPart);
        return objectMapper.readTree(decoded);
    }

    private String base64UrlEncode(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }
}