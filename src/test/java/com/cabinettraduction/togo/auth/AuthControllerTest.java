package com.cabinettraduction.togo.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Tests unitaires pour {@link AuthController}.
 */
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

	@Mock
	private UtilisateurRepository utilisateurRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private JwtTokenProvider tokenProvider;

	@InjectMocks
	private AuthController authController;

	private MockMvc mockMvc;

	private Utilisateur adminUser;

	@BeforeEach
	void setup() {
		mockMvc = MockMvcBuilders.standaloneSetup(authController).build();

		adminUser = new Utilisateur();
		adminUser.setId(1);
		adminUser.setEmail("admin@cabinet.tg");
		adminUser.setMotDePasseHash("$2a$10$hashed_password");
		adminUser.setRole(RoleUtilisateur.ADMIN);
	}

	// ── Cas 1 : Login succès → 200 + JWT ─────────────────────────────────────

	@Test
	void login_credentialsValides_retourne200AvecToken() throws Exception {
		when(utilisateurRepository.findByEmail("admin@cabinet.tg"))
			.thenReturn(Optional.of(adminUser));
		when(passwordEncoder.matches("monMotDePasse", adminUser.getMotDePasseHash()))
			.thenReturn(true);
		when(tokenProvider.creerToken("admin@cabinet.tg", "ADMIN"))
			.thenReturn("eyJhbGciOiJIUzI1NiJ9.fake.token");

		mockMvc
			.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "email": "admin@cabinet.tg",
						  "motDePasse": "monMotDePasse"
						}
						"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.token").value("eyJhbGciOiJIUzI1NiJ9.fake.token"))
			.andExpect(jsonPath("$.role").value("ADMIN"))
			.andExpect(jsonPath("$.email").value("admin@cabinet.tg"));
	}

	// ── Cas 2 : Mauvais mot de passe → 401 ───────────────────────────────────

	@Test
	void login_mauvaisMotDePasse_retourne401() throws Exception {
		when(utilisateurRepository.findByEmail("admin@cabinet.tg"))
			.thenReturn(Optional.of(adminUser));
		when(passwordEncoder.matches("mauvaisMotDePasse", adminUser.getMotDePasseHash()))
			.thenReturn(false);

		mockMvc
			.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "email": "admin@cabinet.tg",
						  "motDePasse": "mauvaisMotDePasse"
						}
						"""))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.erreur").value("Email ou mot de passe incorrect."));
	}

	// ── Cas 3 : Utilisateur inconnu → 401 ────────────────────────────────────

	@Test
	void login_utilisateurInconnu_retourne401() throws Exception {
		when(utilisateurRepository.findByEmail("inconnu@test.com")).thenReturn(Optional.empty());

		mockMvc
			.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "email": "inconnu@test.com",
						  "motDePasse": "n'importe"
						}
						"""))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.erreur").exists());
	}

	// ── Cas 4 : Email invalide (pas un email) → 400 ───────────────────────────

	@Test
	void login_emailInvalide_retourne400() throws Exception {
		mockMvc
			.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "email": "pas-un-email",
						  "motDePasse": "secret"
						}
						"""))
			.andExpect(status().isBadRequest());
	}

	// ── Cas 5 : Corps vide → 400 ──────────────────────────────────────────────

	@Test
	void login_corpsVide_retourne400() throws Exception {
		mockMvc
			.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "email": "",
						  "motDePasse": ""
						}
						"""))
			.andExpect(status().isBadRequest());
	}

}
