package com.cabinettraduction.togo.devis;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Tests unitaires pour {@link DevisController}.
 * Vérifie les 3 scénarios clés sans démarrer le contexte Spring complet.
 */
@ExtendWith(MockitoExtension.class)
class DevisControllerTest {

	@Mock
	private DevisService devisService;

	@InjectMocks
	private DevisController devisController;

	private MockMvc mockMvc;

	@BeforeEach
	void setup() {
		mockMvc = MockMvcBuilders.standaloneSetup(devisController).build();
	}

	// ── Cas 1 : Succès avec fichiers PDF valides ──────────────────────────────

	@Test
	void creerDevis_avecFichiersValides_retourne201() throws Exception {
		when(devisService.creerDemande(any(), anyList())).thenReturn(42);

		MockMultipartFile pdf1 = new MockMultipartFile(
				"fichiers", "contrat.pdf", "application/pdf", "data-pdf".getBytes());
		MockMultipartFile pdf2 = new MockMultipartFile(
				"fichiers", "diplome.pdf", "application/pdf", "data-pdf".getBytes());

		mockMvc
			.perform(multipart("/api/devis").file(pdf1)
				.file(pdf2)
				.param("nom", "Kofi Agbeko")
				.param("email", "kofi@example.com")
				.param("telephone", "+22890123456")
				.param("typeClient", "PARTICULIER")
				.param("serviceId", "1")
				.param("langueSource", "fr")
				.param("langueCible", "en"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.id").value(42))
			.andExpect(jsonPath("$.message").exists());
	}

	// ── Cas 2 : Fichier trop volumineux → 400 ────────────────────────────────

	@Test
	void creerDevis_fichierTropGrand_retourne400() throws Exception {
		doThrow(new FileValidationException(
				"Le fichier 'gros.pdf' dépasse la taille maximale autorisée (10 Mo)."))
			.when(devisService)
			.creerDemande(any(), anyList());

		MockMultipartFile grosFichier = new MockMultipartFile(
				"fichiers", "gros.pdf", "application/pdf", new byte[11 * 1024 * 1024]);

		mockMvc
			.perform(multipart("/api/devis").file(grosFichier)
				.param("nom", "Ama Koffi")
				.param("email", "ama@example.com")
				.param("telephone", "+22891234567")
				.param("typeClient", "ETUDIANT")
				.param("serviceId", "2")
				.param("langueSource", "fr")
				.param("langueCible", "en"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.erreur").value(containsString("taille maximale")));
	}

	// ── Cas 3 : Extension interdite (.exe) → 400 ─────────────────────────────

	@Test
	void creerDevis_extensionInterdite_retourne400() throws Exception {
		doThrow(new FileValidationException(
				"Le type du fichier 'virus.exe' n'est pas autorisé (application/x-dosexec). "
						+ "Formats acceptés : PDF, JPG, PNG, DOCX."))
			.when(devisService)
			.creerDemande(any(), anyList());

		MockMultipartFile fichierExe = new MockMultipartFile(
				"fichiers", "virus.exe", "application/octet-stream", "MZ".getBytes());

		mockMvc
			.perform(multipart("/api/devis").file(fichierExe)
				.param("nom", "Yao Mensah")
				.param("email", "yao@example.com")
				.param("telephone", "+22892345678")
				.param("typeClient", "ENTREPRISE")
				.param("serviceId", "1")
				.param("langueSource", "fr")
				.param("langueCible", "de"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.erreur").value(containsString("n'est pas autorisé")));
	}

}
