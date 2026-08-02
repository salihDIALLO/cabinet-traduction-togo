package com.cabinettraduction.togo.paiement;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.Optional;

import com.cabinettraduction.togo.client.Client;
import com.cabinettraduction.togo.devis.DemandeDevis;
import com.cabinettraduction.togo.devis.DemandeDevisRepository;
import com.cabinettraduction.togo.devis.StatutDemande;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Tests unitaires pour {@link PaiementController}.
 */
@ExtendWith(MockitoExtension.class)
class PaiementControllerTest {

	@Mock
	private PaiementService paiementService;

	@Mock
	private FedaPayService fedaPayService;

	@Mock
	private DemandeDevisRepository demandeDevisRepository;

	@Mock
	private PaiementRepository paiementRepository;

	@InjectMocks
	private PaiementController paiementController;

	private MockMvc mockMvc;

	private DemandeDevis demandeAcceptee;

	private Paiement paiementInitie;

	@BeforeEach
	void setup() {
		mockMvc = MockMvcBuilders.standaloneSetup(paiementController).build();
		ReflectionTestUtils.setField(paiementController, "callbackUrl", "https://votre-site.tg/paiement/retour");

		Client client = new Client();
		client.setEmail("kofi@example.com");
		client.setNom("Kofi");

		demandeAcceptee = new DemandeDevis();
		demandeAcceptee.setId(10);
		demandeAcceptee.setStatut(StatutDemande.ACCEPTE);
		demandeAcceptee.setClient(client);

		paiementInitie = new Paiement();
		paiementInitie.setId(99);
	}

	// ── Cas 1 : Succès initiation paiement → 201 ─────────────────────────────

	@Test
	void initierPaiement_succesNominal_retourne201() throws Exception {
		when(demandeDevisRepository.findById(10)).thenReturn(Optional.of(demandeAcceptee));
		when(paiementService.creerPaiementInitie(any(), any(), any())).thenReturn(paiementInitie);

		FedaPayService.FedaPayTransactionResult result = new FedaPayService.FedaPayTransactionResult();
		FedaPayService.FedaPayTransactionResult.Transaction tx = new FedaPayService.FedaPayTransactionResult.Transaction();
		tx.setReference("TRX-001");
		tx.setApproval_url("https://sandbox.fedapay.com/pay/TRX-001");
		result.setTransaction(tx);

		when(fedaPayService.creerTransaction(any(), anyString(), anyString(), anyString())).thenReturn(result);
		when(paiementRepository.save(any())).thenReturn(paiementInitie);

		mockMvc.perform(post("/api/paiement/init").contentType(MediaType.APPLICATION_JSON).content("""
				{
				  "demandeDevisId": 10,
				  "montant": 15000,
				  "moyenPaiement": "CARTE"
				}
				"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.paiementId").value(99))
			.andExpect(jsonPath("$.urlPaiement").exists())
			.andExpect(jsonPath("$.referenceTransaction").value("TRX-001"));
	}

	// ── Cas 2 : Demande non ACCEPTE → 400 ────────────────────────────────────

	@Test
	void initierPaiement_demandeNonAcceptee_retourne400() throws Exception {
		DemandeDevis demandeNouvelle = new DemandeDevis();
		demandeNouvelle.setId(20);
		demandeNouvelle.setStatut(StatutDemande.NOUVEAU);
		Client c = new Client();
		c.setEmail("test@test.com");
		demandeNouvelle.setClient(c);

		when(demandeDevisRepository.findById(20)).thenReturn(Optional.of(demandeNouvelle));

		mockMvc.perform(post("/api/paiement/init").contentType(MediaType.APPLICATION_JSON).content("""
				{
				  "demandeDevisId": 20,
				  "montant": 5000,
				  "moyenPaiement": "FLOOZ"
				}
				""")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.erreur").exists());
	}

	// ── Cas 3 : Demande introuvable → 400 ────────────────────────────────────

	@Test
	void initierPaiement_demandeIntrouvable_retourne400() throws Exception {
		when(demandeDevisRepository.findById(999)).thenReturn(Optional.empty());

		mockMvc.perform(post("/api/paiement/init").contentType(MediaType.APPLICATION_JSON).content("""
				{
				  "demandeDevisId": 999,
				  "montant": 5000,
				  "moyenPaiement": "TMONEY"
				}
				""")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.erreur").exists());
	}

	// ── Cas 4 : Webhook signature valide → 200 ───────────────────────────────

	@Test
	void webhook_signatureValide_retourne200() throws Exception {
		byte[] payload = """
				{"name":"transaction.approved","data":{"object":{"reference":"TRX-001"}}}
				""".getBytes();

		when(fedaPayService.verifierSignature(any(), anyString())).thenReturn(true);

		mockMvc
			.perform(post("/api/paiement/webhook").header("X-FedaPay-Signature", "valid-hmac-signature")
				.contentType(MediaType.APPLICATION_JSON)
				.content(payload))
			.andExpect(status().isOk());
	}

	// ── Cas 5 : Webhook signature invalide → 401 ─────────────────────────────

	@Test
	void webhook_signatureInvalide_retourne401() throws Exception {
		when(fedaPayService.verifierSignature(any(), anyString())).thenReturn(false);

		mockMvc
			.perform(post("/api/paiement/webhook").header("X-FedaPay-Signature", "bad-signature")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"name":"transaction.approved"}
						"""))
			.andExpect(status().isUnauthorized());
	}

	// ── Cas 6 : Webhook sans signature → 401 ─────────────────────────────────

	@Test
	void webhook_sansSignature_retourne401() throws Exception {
		mockMvc.perform(post("/api/paiement/webhook").contentType(MediaType.APPLICATION_JSON).content("""
				{"name":"transaction.approved"}
				""")).andExpect(status().isUnauthorized());
	}

}
