    package com.example.finance.security;

        import com.example.finance.entity.Authority;
        import com.example.finance.entity.Role;
        import com.example.finance.entity.User;
        import io.jsonwebtoken.*;
        import io.jsonwebtoken.io.Decoders;
        import io.jsonwebtoken.security.Keys;
        import io.jsonwebtoken.security.SignatureException;
        import org.springframework.beans.factory.annotation.Value;
        import org.springframework.security.authentication.BadCredentialsException;
        import org.springframework.stereotype.Service;

        import javax.crypto.SecretKey;
        import java.time.Instant;
        import java.util.*;

            @Service
            public class JwtService {

                private final SecretKey signingKey;
                private final long expiration;
                private final String issuer;

                public JwtService(
                        @Value("${app.jwt.secret}") String secret,
                        @Value("${app.jwt.expiration-seconds:3600}") long expiration,
                        @Value("${app.jwt.issuer:demo-api}") String issuer) {

                    this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
                    this.expiration = expiration;
                    this.issuer = issuer;
                }

                public String generateToken(User user) {
                    //Tạo thời gian hết hạn
                    Instant now = Instant.now();
                    Instant exp = now.plusSeconds(expiration);

                    // Lấy roles từ user
                    List<String> roles = user.getRoles() == null ? List.of() :
                            user.getRoles().stream()
                                    .map(role -> role.getName().name())
                                    .filter(Objects::nonNull)
                                    .toList();

                    // Lấy authorities từ các role
                    Set<String> authorities = new HashSet<>();
                    if (user.getRoles() != null) {
                        for (Role role : user.getRoles()) {
                            if (role.getAuthorities() != null) {
                                for (Authority authority : role.getAuthorities()) {
                                    if (authority != null && authority.getName() != null) {
                                        authorities.add(authority.getName());
                                    }
                                }
                            }
                        }
                    }

                    // Tạo claims
                    Map<String, Object> claims = new HashMap<>();
                    claims.put("roles", roles);
                    claims.put("authorities", authorities);

                    // Tạo JWT token
                    return Jwts.builder()
                            .claims(claims)
                            .subject(user.getEmail())
                            .issuedAt(Date.from(now))
                            .expiration(Date.from(exp))
                            .issuer(issuer)
                            .signWith(signingKey)
                            .compact();
                }

                //Kiểm tra token
                public boolean isTokenValid(String token){
                    try{
                        parseClaims(token);
                        return true;
                    } catch (ExpiredJwtException ex){
                        throw new BadCredentialsException("Token has expired");
                    } catch(SignatureException ex){
                        throw new BadCredentialsException("Token signature is invalid");
                    } catch (IllegalArgumentException ex){
                        throw new BadCredentialsException("Missing access token");
                    } catch (JwtException ex){
                        throw new BadCredentialsException("Invalid token");
                    }
                }

                //Extract Claims
                public Claims parseClaims(String token){
                    return Jwts.parser()
                            .verifyWith(signingKey)
                            .build()
                            .parseSignedClaims(token)
                            .getPayload();
                }

                //Extract Email
                public String extractEmail(String token){
                    return parseClaims(token).getSubject();
                }


                //Lấy thời gian hết hạn
                public long getExpiration(String token){
                    Date exp = parseClaims(token).getExpiration();
                    return exp.toInstant().getEpochSecond();
                }
            }

