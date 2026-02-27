package com.revature.passwordmanager.security;

import com.revature.passwordmanager.config.JwtConfig;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

  private final JwtConfig jwtConfig;

  public String generateAccessToken(Authentication authentication) {
    UserDetails userPrincipal = (UserDetails) authentication.getPrincipal();
    return generateToken(new HashMap<>(), userPrincipal, jwtConfig.getAccessTokenExpiration());
  }

  public String generateRefreshToken(Authentication authentication) {
    UserDetails userPrincipal = (UserDetails) authentication.getPrincipal();
    return generateToken(new HashMap<>(), userPrincipal, jwtConfig.getRefreshTokenExpiration());
  }

  public String getUsernameFromToken(String token) {
    return extractClaim(token, Claims::getSubject);
  }

  public boolean validateToken(String token) {
    try {
      Jwts.parser()
          .verifyWith(getSignInKey())
          .build()
          .parseSignedClaims(token);
      return true;
    } catch (MalformedJwtException ex) {
      // Invalid JWT token
    } catch (ExpiredJwtException ex) {
      // Expired JWT token
    } catch (UnsupportedJwtException ex) {
      // Unsupported JWT token
    } catch (IllegalArgumentException ex) {
      // JWT claims string is empty
    } catch (JwtException ex) {
      // General JWT exception (Signature, etc.)
    }
    return false;
  }

  private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
    final Claims claims = extractAllClaims(token);
    return claimsResolver.apply(claims);
  }

  private Claims extractAllClaims(String token) {
    return Jwts.parser()
        .verifyWith(getSignInKey())
        .build()
        .parseSignedClaims(token)
        .getPayload();
  }

  private String generateToken(Map<String, Object> extraClaims, UserDetails userDetails, long expiration) {
    return Jwts.builder()
        .claims(extraClaims)
        .subject(userDetails.getUsername())
        .issuedAt(new Date(System.currentTimeMillis()))
        .expiration(new Date(System.currentTimeMillis() + expiration))
        .signWith(getSignInKey())
        .compact();
  }

  private SecretKey getSignInKey() {
    byte[] keyBytes = Decoders.BASE64.decode(jwtConfig.getSecret());
    return Keys.hmacShaKeyFor(keyBytes);
  }
}
