package org.app.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import org.app.schemas.session_object.SessionObject;

public final class SessionAccess {
    public static final String HEADER = "X-Claim-Token";
    private static final SecureRandom RANDOM = new SecureRandom();

    private SessionAccess() {
    }

    public static String issue(SessionObject session) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        session.setAccessTokenHash(hash(token));
        return token;
    }

    public static boolean permits(SessionObject session, String token) {
        if (session == null || session.getAccessTokenHash() == null
                || token == null || token.length() > 128) {
            return false;
        }
        return MessageDigest.isEqual(
                session.getAccessTokenHash().getBytes(StandardCharsets.US_ASCII),
                hash(token).getBytes(StandardCharsets.US_ASCII));
    }

    private static String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 no disponible", error);
        }
    }
}
