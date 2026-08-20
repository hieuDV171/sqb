package com.frozenheart.backend.core.security;

import com.frozenheart.backend.core.config.property.JwtProperties;
import com.frozenheart.backend.core.dto.jwt.JwtPayload;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties jwtProperties;

    public long getRefreshTokenExpiration() {
        return jwtProperties.getRefreshTokenExpiration();
    }

    public long getAccessTokenExpiration() {
        return jwtProperties.getAccessTokenExpiration();
    }

    private SecretKey getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtProperties.getSecret());
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // Decode chung cho cả AT và RT (Nếu trường nào không có sẽ nhận null)
    public JwtPayload decodeToken(String token) {
        // Hàm này SẼ NÉM LỖI (Exception) ngay lập tức nếu:
        // 1. Token hết hạn (ExpiredJwtException)
        // 2. Chữ ký bị can thiệp (SignatureException)
        // 3. Chuỗi token bị hỏng (MalformedJwtException)
        Claims claims = extractAllClaims(token);

        return JwtPayload.builder()
                .username(claims.getSubject())
                .role(claims.get("role", String.class))
                .userId(claims.get("userId", Long.class))
                .deviceId(claims.get("deviceId", String.class))
                .build();
    }

    private String generateToken(Map<String, Object> extraClaims, String subject, long expiration) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(subject)
                .issuer(jwtProperties.getIssuer())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSignInKey())
                .compact();
    }

    public String generateAccessToken(Map<String, Object> extraClaims, String subject) {

        return generateToken(extraClaims, subject, jwtProperties.getAccessTokenExpiration());
    
    }

    public String generateRefreshToken(String subject, String deviceId) {
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("type", "refresh");
        extraClaims.put("deviceId", deviceId);
        return generateToken(extraClaims, subject, jwtProperties.getRefreshTokenExpiration());
    }

    public boolean isTokenValid(JwtPayload payload, UserDetails userDetails) {

        final String tokenUsername = payload.getUsername();
        return tokenUsername.equals(userDetails.getUsername());

    }

}
