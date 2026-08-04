package com.cabinettraduction.togo.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.aot.DisabledInAotMode;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Tests d'intégration Spring Security. Vérifie les règles d'accès : JWT obligatoire,
 * rôles ADMIN/EDITEUR.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK,
		properties = { "spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=none",
				"spring.sql.init.mode=never",
				"app.jwt.secret=test-secret-key-for-unit-tests-minimum-64-bytes-long-padding-ok",
				"cloud.aws.credentials.access-key=test", "cloud.aws.credentials.secret-key=test",
				"cloud.aws.s3.bucket=test", "cloud.aws.region.static=eu-west-3", "spring.mail.host=localhost",
				"spring.mail.port=25", "app.fedapay.api-key=test", "app.fedapay.webhook-secret=test" })
@AutoConfigureMockMvc
@DisabledInNativeImage
@DisabledInAotMode
class SecurityIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	// ── Accès public : page d'accueil ─────────────────────────────────────────

	@Test
	void accueil_sansAuth_retourne200() throws Exception {
		mockMvc.perform(get("/")).andExpect(status().isOk());
	}

	// ── Accès public : formulaire devis ──────────────────────────────────────

	@Test
	void devis_sansAuth_retourne200() throws Exception {
		mockMvc.perform(get("/devis")).andExpect(status().isOk());
	}

	// ── Admin sans JWT → 403 ─────────────────────────────────────────────────

	@Test
	void adminDevis_sansJwt_retourne403() throws Exception {
		mockMvc.perform(get("/admin/devis")).andExpect(status().isForbidden());
	}

	// ── Admin avec rôle EDITEUR → GET autorisé ────────────────────────────────

	@Test
	@WithMockUser(roles = "EDITEUR")
	void adminDevisGet_roleEditeur_retourne200ouAutre() throws Exception {
		// 200 ou 500 (pas de BDD en test) — l'important est que ce ne soit pas 403
		mockMvc.perform(get("/admin/devis")).andExpect(result -> {
			int status = result.getResponse().getStatus();
			assert status != 403 : "EDITEUR ne devrait pas avoir 403 sur GET /admin/devis";
		});
	}

	// ── PUT statut avec rôle EDITEUR → 403 ───────────────────────────────────

	@Test
	@WithMockUser(roles = "EDITEUR")
	void adminDevisPutStatut_roleEditeur_retourne403() throws Exception {
		mockMvc.perform(put("/admin/devis/1/statut").contentType(MediaType.APPLICATION_JSON).content("""
				{"statut":"ACCEPTE"}
				""")).andExpect(status().isForbidden());
	}

	// ── PUT statut avec rôle ADMIN → autorisé ────────────────────────────────

	@Test
	@WithMockUser(roles = "ADMIN")
	void adminDevisPutStatut_roleAdmin_nonForbidden() throws Exception {
		// 400 ou 500 (pas de BDD) mais pas 403
		mockMvc.perform(put("/admin/devis/999/statut").contentType(MediaType.APPLICATION_JSON).content("""
				{"statut":"ACCEPTE"}
				""")).andExpect(result -> {
			int s = result.getResponse().getStatus();
			assert s != 403 : "ADMIN ne devrait pas avoir 403";
		});
	}

	// ── Paiement init sans JWT → 403 ─────────────────────────────────────────

	@Test
	void paiementInit_sansJwt_retourne403() throws Exception {
		mockMvc.perform(put("/api/paiement/init").contentType(MediaType.APPLICATION_JSON).content("""
				{"demandeDevisId":1,"montant":5000}
				""")).andExpect(status().isForbidden());
	}

	@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class,
			DataSourceTransactionManagerAutoConfiguration.class, HibernateJpaAutoConfiguration.class },
			scanBasePackages = "com.cabinettraduction.togo")
	static class TestConfig {

	}

}
