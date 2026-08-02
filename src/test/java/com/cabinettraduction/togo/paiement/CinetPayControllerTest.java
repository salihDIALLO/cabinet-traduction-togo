package com.cabinettraduction.togo.paiement;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import com.cabinettraduction.togo.client.Client;
import com.cabinettraduction.togo.devis.DemandeDevis;
import com.cabinettraduction.togo.devis.DemandeDevisRepository;
import com.cabinettraduction.togo.devis.StatutDemande;
import com.cabinettraduction.togo.service.Service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Tests unitaires pour {@link CinetPayController}.
 *
 * Cas clé vérifié :
 * - Le re-call /v2/payment/check est TOUJOURS invoqué lors d'un notify
 * - La BDD n'est jamais mise à jour si le check échoue ou est absent
 * - L'ordre : 1) check API, 2) mise à jour paiement, 3) mise à jour demande
 */
@ExtendWith(MockitoExtension.class)
class CinetPayControllerTest {

	@Mock
	private CinetPayService cinetPayService;

	@Mock
	private PaiementRepository paiementRepository;

	@Mock
	private DemandeDevisRepository demandeDevisRepository;

	@InjectMocks
	private CinetPayController controller;

	private MockMvc mockMvc;

	// ── Données de test ───────────────────────────────────────────────────────

	private static final String TX_ID = "CAB-ABCD1234EF56";

	private Paiement paiementExistant;

	private DemandeDevis demandeAcceptee;

	@BeforeEach
	void setup() {
		mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

		Client client = new Client();
		client.setId(1);
		client.setNom("Kofi Agbeko");
		client.setEmail("kofi@example.tg");

		Service service = new Service();
		service.setNom("Traduction Certifiée");

		demandeAcceptee = new DemandeDevis();
		demandeAcceptee.setId(10);
		demandeAcceptee.setStatut(StatutDemande.ACCEPTE);
		demandeAcceptee.setClient(client);
		demandeAcceptee.setService(service);
		demandeAcceptee.setLangueSource("fr");
		demandeAcceptee.setLangueCible("en");
		demandeAcceptee.setMontantEstime(new BigDecimal("15000"));

		paiementExistant = new Paiement();
		paiementExistant.setId(99);
		paiementExistant.setDemandeDevis(demandeAcceptee);
		paiementExistant.setMontant(new BigDecimal("15000"));
		paiementExistant.setStatut(StatutPaiement.INITIE);
		paiementExistant.setMoyenPaiement(MoyenPaiement.FLOOZ);
		paiementExistant.setReferenceTransactionFedaPay(TX_ID);
	}

	// ═══════════════════════════════════════════════════════════════════════
	// INIT
	// ═══════════════════════════════════════════════════════════════════════

	@Nested
	@DisplayName("POST /api/paiement/cinetpay/init")
	class InitTests {

		@Test
		@DisplayName("Succès — retourne 201 avec paymentUrl et transactionId")
		void init_succesNominal_retourne201() throws Exception {
			when(demandeDevisRepository.findById(10)).thenReturn(Optional.of(demandeAcceptee));

			CinetPayService.InitResult result = CinetPayService.InitResult.success(
					TX_ID,
					"https://payment.cinetpay.com/pay/TOKEN123",
					"TOKEN123");
			when(cinetPayService.init(any(), anyString(), anyString(), anyString(), anyString()))
				.thenReturn(result);
			when(paiementRepository.save(any())).thenReturn(paiementExistant);

			mockMvc.perform(post("/api/paiement/cinetpay/init")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{"demandeDevisId":10,"montant":15000}
							"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.transactionId").value(TX_ID))
				.andExpect(jsonPath("$.paymentUrl").value("https://payment.cinetpay.com/pay/TOKEN123"))
				.andExpect(jsonPath("$.paiementId").value(99));
		}

		@Test
		@DisplayName("Demande non ACCEPTE — retourne 400")
		void init_demandeNonAcceptee_retourne400() throws Exception {
			DemandeDevis demandeNouvelle = new DemandeDevis();
			demandeNouvelle.setId(20);
			demandeNouvelle.setStatut(StatutDemande.NOUVEAU);
			Client c = new Client();
			c.setNom("Test");
			c.setEmail("t@t.com");
			demandeNouvelle.setClient(c);

			when(demandeDevisRepository.findById(20)).thenReturn(Optional.of(demandeNouvelle));

			mockMvc.perform(post("/api/paiement/cinetpay/init")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{"demandeDevisId":20,"montant":5000}
							"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erreur").exists());

			// CinetPay ne doit pas être appelé si la demande n'est pas ACCEPTE
			verify(cinetPayService, never()).init(any(), any(), any(), any(), any());
		}

		@Test
		@DisplayName("Demande introuvable — retourne 400")
		void init_demandeIntrouvable_retourne400() throws Exception {
			when(demandeDevisRepository.findById(999)).thenReturn(Optional.empty());

			mockMvc.perform(post("/api/paiement/cinetpay/init")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{"demandeDevisId":999,"montant":5000}
							"""))
				.andExpect(status().isBadRequest());
		}

		@Test
		@DisplayName("CinetPay renvoie une erreur — retourne 502")
		void init_cinetpayEchec_retourne502() throws Exception {
			when(demandeDevisRepository.findById(10)).thenReturn(Optional.of(demandeAcceptee));
			when(cinetPayService.init(any(), any(), any(), any(), any()))
				.thenReturn(CinetPayService.InitResult.failure(TX_ID, "API indisponible"));

			mockMvc.perform(post("/api/paiement/cinetpay/init")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{"demandeDevisId":10,"montant":15000}
							"""))
				.andExpect(status().isBadGateway())
				.andExpect(jsonPath("$.erreur").value(
						org.hamcrest.Matchers.containsString("CinetPay")));

			// Aucun paiement ne doit être sauvegardé si CinetPay échoue
			verify(paiementRepository, never()).save(any());
		}

	}

	// ═══════════════════════════════════════════════════════════════════════
	// NOTIFY — vérification de l'ordre des opérations
	// ═══════════════════════════════════════════════════════════════════════

	@Nested
	@DisplayName("POST /api/paiement/cinetpay/notify — re-confirmation obligatoire")
	class NotifyTests {

		@Test
		@DisplayName("Paiement ACCEPTE — check appelé AVANT save, statut mis à jour")
		void notify_paiementAccepte_checkAvantSave() throws Exception {
			// Le re-call /v2/payment/check confirme ACCEPTED
			CinetPayService.CheckResult checkResult = new CinetPayService.CheckResult(
					"ACCEPTED", "FLOOZ", 15000);
			when(cinetPayService.verifierPaiement(TX_ID)).thenReturn(Optional.of(checkResult));
			when(paiementRepository.findAll()).thenReturn(List.of(paiementExistant));
			when(paiementRepository.save(any())).thenReturn(paiementExistant);
			when(demandeDevisRepository.save(any())).thenReturn(demandeAcceptee);

			mockMvc.perform(post("/api/paiement/cinetpay/notify")
					.param("cpm_trans_id", TX_ID)
					.contentType(MediaType.APPLICATION_FORM_URLENCODED))
				.andExpect(status().isOk());

			// ── ORDRE VÉRIFIÉ : 1) check, 2) save paiement, 3) save demande ──
			InOrder inOrder = Mockito.inOrder(cinetPayService, paiementRepository,
					demandeDevisRepository);
			inOrder.verify(cinetPayService, times(1)).verifierPaiement(TX_ID);
			inOrder.verify(paiementRepository, times(1)).save(any());
			inOrder.verify(demandeDevisRepository, times(1)).save(any());
		}

		@Test
		@DisplayName("Check retourne REFUSED — statut ECHOUE, demande NON modifiée")
		void notify_paiementRefuse_demandeNonModifiee() throws Exception {
			CinetPayService.CheckResult checkResult = new CinetPayService.CheckResult(
					"REFUSED", "FLOOZ", 15000);
			when(cinetPayService.verifierPaiement(TX_ID)).thenReturn(Optional.of(checkResult));
			when(paiementRepository.findAll()).thenReturn(List.of(paiementExistant));
			when(paiementRepository.save(any())).thenReturn(paiementExistant);

			mockMvc.perform(post("/api/paiement/cinetpay/notify")
					.param("cpm_trans_id", TX_ID)
					.contentType(MediaType.APPLICATION_FORM_URLENCODED))
				.andExpect(status().isOk());

			// Paiement sauvegardé (statut ECHOUE)
			verify(paiementRepository, times(1)).save(any());
			// Demande NON modifiée quand paiement refusé
			verify(demandeDevisRepository, never()).save(any());
		}

		@Test
		@DisplayName("Check API échoue (réseau) — aucune mise à jour BDD")
		void notify_checkApiFailed_aucuneSauvegarde() throws Exception {
			// Le re-call échoue (timeout réseau, etc.)
			when(cinetPayService.verifierPaiement(TX_ID)).thenReturn(Optional.empty());

			mockMvc.perform(post("/api/paiement/cinetpay/notify")
					.param("cpm_trans_id", TX_ID)
					.contentType(MediaType.APPLICATION_FORM_URLENCODED))
				.andExpect(status().isOk()); // toujours 200 pour éviter les retentatives

			// AUCUNE modification BDD si le check échoue
			verify(paiementRepository, never()).save(any());
			verify(demandeDevisRepository, never()).save(any());
		}

		@Test
		@DisplayName("Notify sans transaction_id — ignoré silencieusement")
		void notify_sansTxId_ignore() throws Exception {
			mockMvc.perform(post("/api/paiement/cinetpay/notify")
					.contentType(MediaType.APPLICATION_FORM_URLENCODED)
					.content(""))
				.andExpect(status().isOk());

			// Aucun appel à quoi que ce soit
			verify(cinetPayService, never()).verifierPaiement(anyString());
			verify(paiementRepository, never()).save(any());
		}

		@Test
		@DisplayName("Notify avec txId dans le body (form-encoded) — extrait correctement")
		void notify_txIdDansBody_extraitEtVerifie() throws Exception {
			CinetPayService.CheckResult checkResult = new CinetPayService.CheckResult(
					"ACCEPTED", "MIXX_BY_YAS", 15000);
			when(cinetPayService.verifierPaiement(TX_ID)).thenReturn(Optional.of(checkResult));
			when(paiementRepository.findAll()).thenReturn(List.of(paiementExistant));
			when(paiementRepository.save(any())).thenReturn(paiementExistant);
			when(demandeDevisRepository.save(any())).thenReturn(demandeAcceptee);

			// CinetPay envoie parfois l'ID dans le body plutôt que le query param
			String body = "cpm_trans_id=" + TX_ID + "&cpm_result=00";

			mockMvc.perform(post("/api/paiement/cinetpay/notify")
					.contentType(MediaType.APPLICATION_FORM_URLENCODED)
					.content(body))
				.andExpect(status().isOk());

			verify(cinetPayService, times(1)).verifierPaiement(TX_ID);
		}

		@Test
		@DisplayName("MIXX_BY_YAS → enum YASS correctement mappé")
		void notify_mixxByYas_mappedToYass() throws Exception {
			CinetPayService.CheckResult checkResult = new CinetPayService.CheckResult(
					"ACCEPTED", "MIXX_BY_YAS", 15000);
			when(cinetPayService.verifierPaiement(TX_ID)).thenReturn(Optional.of(checkResult));
			when(paiementRepository.findAll()).thenReturn(List.of(paiementExistant));

			Paiement[] savedPaiement = new Paiement[1];
			when(paiementRepository.save(any(Paiement.class))).thenAnswer(invocation -> {
				savedPaiement[0] = invocation.getArgument(0);
				return savedPaiement[0];
			});
			when(demandeDevisRepository.save(any())).thenReturn(demandeAcceptee);

			mockMvc.perform(post("/api/paiement/cinetpay/notify")
					.param("cpm_trans_id", TX_ID)
					.contentType(MediaType.APPLICATION_FORM_URLENCODED))
				.andExpect(status().isOk());

			// Le moyen de paiement doit être YASS (pas FLOOZ qui était la valeur par défaut)
			assert savedPaiement[0] != null : "paiement.save() non appelé";
			assert MoyenPaiement.YASS.equals(savedPaiement[0].getMoyenPaiement())
					: "MIXX_BY_YAS devrait mapper vers YASS, obtenu : "
							+ savedPaiement[0].getMoyenPaiement();
		}

	}

}
