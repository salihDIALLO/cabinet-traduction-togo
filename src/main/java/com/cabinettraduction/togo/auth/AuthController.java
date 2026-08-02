package com.cabinettraduction.togo.auth;

import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.Data;

/**
 * Endpoint d'authentification back-office. POST /api/auth/login — retourne un JWT signé
 * (validité 8h).
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final UtilisateurRepository utilisateurRepository;

	private final PasswordEncoder passwordEncoder;

	private final JwtTokenProvider tokenProvider;

	public AuthController(UtilisateurRepository utilisateurRepository, PasswordEncoder passwordEncoder,
			JwtTokenProvider tokenProvider) {
		this.utilisateurRepository = utilisateurRepository;
		this.passwordEncoder = passwordEncoder;
		this.tokenProvider = tokenProvider;
	}

	@PostMapping("/login")
	public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
		Utilisateur utilisateur = utilisateurRepository.findByEmail(request.getEmail())
			.orElseThrow(() -> new AuthException("Email ou mot de passe incorrect."));

		if (!passwordEncoder.matches(request.getMotDePasse(), utilisateur.getMotDePasseHash())) {
			throw new AuthException("Email ou mot de passe incorrect.");
		}

		String token = tokenProvider.creerToken(utilisateur.getEmail(), utilisateur.getRole().name());

		return ResponseEntity
			.ok(Map.of("token", token, "role", utilisateur.getRole().name(), "email", utilisateur.getEmail()));
	}

	@org.springframework.web.bind.annotation.ExceptionHandler(AuthException.class)
	public ResponseEntity<Map<String, String>> gererErreurAuth(AuthException ex) {
		return ResponseEntity.status(401).body(Map.of("erreur", ex.getMessage()));
	}

	// ── DTOs internes ─────────────────────────────────────────────────────────

	@Data
	public static class LoginRequest {

		@Email
		@NotBlank
		private String email;

		@NotBlank
		private String motDePasse;

	}

	static class AuthException extends RuntimeException {

		AuthException(String message) {
			super(message);
		}

	}

}
