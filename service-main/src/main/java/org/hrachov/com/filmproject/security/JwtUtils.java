package org.hrachov.com.filmproject.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

@Component
public class JwtUtils {

    @Value("${jwt.secret}")
    private String jwtSecretString; // Renamed to avoid confusion with SecretKey type

    private final long jwtExpirationMs = 86400000; // 24 hours

    private SecretKey secretKey;
    private JwtParser jwtParser;

    @PostConstruct
    public void init() {
        // Ensure your jwt.secret is strong enough for HS512 (ideally 64 URL-safe base64 characters)
        // Keys.hmacShaKeyFor will throw an error if the key is too weak for the algorithm.
        // For HS512, the key material must be at least 512 bits (64 bytes).
        byte[] keyBytes = jwtSecretString.getBytes(StandardCharsets.UTF_8);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.jwtParser = Jwts.parser().verifyWith(this.secretKey).build();
    }

    // Helper method to get the signing key
    private SecretKey getSigningKey() {
        return this.secretKey;
    }

    // Generates a token for a given UserDetails
    public String generateToken(UserDetails userDetails) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        var authorities = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority) // например, "ROLE_ADMIN"
                .toList();

        return Jwts.builder()
                .subject(userDetails.getUsername()) // Use .subject() instead of .setSubject()
                .claim("roles", authorities) //Выставляем роли
                .issuedAt(now)                     // Use .issuedAt() instead of .setIssuedAt()
                .expiration(expiryDate)            // Use .expiration() instead of .setExpiration()
                .signWith(getSigningKey(), Jwts.SIG.HS512) // Use SecretKey and Jwts.SIG.HS512
                .compact();
    }

    // Extracts all claims from the token
    private Claims extractAllClaims(String token) {
        return jwtParser.parseSignedClaims(token).getPayload(); // Use .getPayload() instead of .getBody()
    }

    // Extracts a specific claim from the token using a claims resolver function
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    // Extracts the username (subject) from the token
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // Extracts the expiration date from the token
    public Date extractExpirationDate(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    // Checks if the token has expired
    boolean isTokenExpired(String token) {
        return extractExpirationDate(token).before(new Date());
    }

    // Validates the token against UserDetails
    public boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }
    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("roles", List.class);
    }

    // You can also add a method to validate the token structure and signature without checking expiration or user details
    public boolean isTokenSignatureValid(String token) {
        try {
            jwtParser.parseSignedClaims(token);
            return true;
        } catch (io.jsonwebtoken.JwtException | IllegalArgumentException e) {
            // Log the exception if needed: e.g., log.error("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }
}