package com.cabinettraduction.togo.admin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import com.cabinettraduction.togo.devis.DemandeDevis;
import com.cabinettraduction.togo.devis.DemandeDevisRepository;
import com.cabinettraduction.togo.devis.StatutDemande;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Tests unitaires pour {@link AdminDevisController}.
 *
 * Note sur l'accès non autorisé : Les tests d'accès JWT (403 sans token, 403 EDITEUR sur
 * PUT) sont vérifiés au niveau de la configuration Spring Security dans SecurityConfig.
 * Ici on teste la logique métier du contrôleur en isolation. Les tests d'intégration
 * sécurité sont dans SecurityIntegrationTest.
 */
@ExtendWith(MockitoExtension.class)
class AdminDevisControllerTest {

	@Mock
	private DemandeDevisRepository demandeDevisRepository;

	@InjectMocks
	private AdminDevisController adminDevisController;

	private MockMvc mockMvc;

	private DemandeDevis demandeTest;

	@BeforeEach
	void setup() {
		mockMvc = MockMvcBuilders.standaloneSetup(adminDevisController).build();

		demandeTest = new DemandeDevis();
		demandeTest.setId(1);
		demandeTest.setStatut(StatutDemande.NOUVEAU);
	}

	// ── Cas 1 : Liste paginée → 200 ──────────────────────────────────────────

	@Test
	void lister_sansFiltre_retourne200AvecPage() throws Exception {
		when(demandeDevisRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(demandeTest)));

		mockMvc.perform(get("/admin/devis?page=0&size=10"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content").isArray())
			.andExpect(jsonPath("$.content[0].id").value(1));
	}

	// ── Cas 2 : Liste filtrée par statut → 200 ───────────────────────────────

	@Test
	void lister_avecFiltreStatut_retourne200() throws Exception {
		when(demandeDevisRepository.findByStatut(any(StatutDemande.class), any(Pageable.class)))
			.thenReturn(new PageImpl<>(List.of(demandeTest)));

		mockMvc.perform(get("/admin/devis?statut=NOUVEAU"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[0].id").value(1));
	}

	// ── Cas 3 : Changer statut succès → 200 ──────────────────────────────────

	@Test
	void changerStatut_succesNominal_retourne200() throws Exception {
		when(demandeDevisRepository.findById(1)).thenReturn(Optional.of(demandeTest));
		when(demandeDevisRepository.save(any())).thenReturn(demandeTest);

		mockMvc.perform(put("/admin/devis/1/statut").contentType(MediaType.APPLICATION_JSON).content("""
				{"statut": "ACCEPTE"}
				"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(1))
			.andExpect(jsonPath("$.statut").value("ACCEPTE"));
	}

	// ── Cas 4 : Changer statut — demande introuvable → 400 ───────────────────

	@Test
	void changerStatut_demandeIntrouvable_retourne400() throws Exception {
		when(demandeDevisRepository.findById(999)).thenReturn(Optional.empty());

		mockMvc.perform(put("/admin/devis/999/statut").contentType(MediaType.APPLICATION_JSON).content("""
				{"statut": "ACCEPTE"}
				""")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.erreur").exists());
	}

	// ── Cas 5 : Statut null → 400 ────────────────────────────────────────────

	@Test
	void changerStatut_statutNull_retourne400() throws Exception {
		mockMvc.perform(put("/admin/devis/1/statut").contentType(MediaType.APPLICATION_JSON).content("""
				{"statut": null}
				""")).andExpect(status().isBadRequest());
	}

}
