package com.cabinettraduction.togo.auth;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Génération et validation des tokens JWT (HS256).
 *
 * <p>
 * Génerer un secret aléatoire sûr en local : <pre>
 *   # PowerShell
 *   [System.Convert]::ToBase64String([System.Security.Cryptography.RandomNumberGenerator]::GetBytes(64))
 *
 *   # Linux/macOS
 *   openssl rand -base64 64
 * </pre> Stocker la valeur dans la variable d'environnement {@code JWT_SECRET}.
 */
@Component
public class JwtTokenProvider {

	private static final long EXPIRATION_MS = 8L * 60 * 60 * 1000; // 8 heures

	private final SecretKey secretKey;

	public JwtTokenProvider(@Value("${app.jwt.secret}") String secret) {
		this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
	}

	/**
	 * Crée un token JWT signé HS256 contenant l'email et le rôle.
	 */
	public String creerToken(String email, String role) {
		Date now = new Date();
		Date expiration = new Date(now.getTime() + EXPIRATION_MS);

		return Jwts.builder()
			.subject(email)
			.claim("role", role)
			.issuedAt(now)
			.expiration(expiration)
			.signWith(secretKey)
			.compact();
	}

	/**
	 * Extrait les claims du token après validation de la signature.
	 * @throws JwtException si le token est invalide ou expiré
	 */
	public Claims valider(String token) {
		return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
	}

	public String getEmail(String token) {
		return valider(token).getSubject();
	}

	public String getRole(String token) {
		return valider(token).get("role", String.class);
	}

	public boolean estValide(String token) {
		try {
			valider(token);
			return true;
		}
		catch (JwtException | IllegalArgumentException e) {
			return false;
		}
	}

}
