package com.cabinettraduction.togo.paiement;

import java.io.IOException;
import java.util.Map;

import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cabinettraduction.togo.devis.DemandeDevis;
import com.cabinettraduction.togo.devis.DemandeDevisRepository;
import com.cabinettraduction.togo.devis.StatutDemande;

/**
 * Endpoints REST pour l'initiation et la réception des paiements FedaPay.
 */
@RestController
@RequestMapping("/api/paiement")
public class PaiementController {

	private static final Logger log = LoggerFactory.getLogger(PaiementController.class);

	private final PaiementService paiementService;

	private final FedaPayService fedaPayService;

	private final DemandeDevisRepository demandeDevisRepository;

	private final PaiementRepository paiementRepository;

	@Value("${app.fedapay.callback-url:https://votre-site.tg/paiement/retour}")
	private String callbackUrl;

	public PaiementController(PaiementService paiementService, FedaPayService fedaPayService,
			DemandeDevisRepository demandeDevisRepository, PaiementRepository paiementRepository) {
		this.paiementService = paiementService;
		this.fedaPayService = fedaPayService;
		this.demandeDevisRepository = demandeDevisRepository;
		this.paiementRepository = paiementRepository;
	}

	// ─────────────────────────────────────────────────────────────────────────
	// POST /api/paiement/init
	// ─────────────────────────────────────────────────────────────────────────

	/**
	 * Initialise un paiement FedaPay pour une demande de devis acceptée.
	 * Retourne l'URL de paiement pour redirection côté frontend.
	 */
	@PostMapping("/init")
	public ResponseEntity<?> initierPaiement(@Valid @RequestBody InitPaiementRequest request) {
		DemandeDevis demande = demandeDevisRepository.findById(request.getDemandeDevisId())
			.orElseThrow(() -> new IllegalArgumentException(
					"Demande introuvable : " + request.getDemandeDevisId()));

		if (demande.getStatut() != StatutDemande.ACCEPTE) {
			return ResponseEntity.badRequest()
				.body(Map.of("erreur",
						"Le paiement n'est possible que pour une demande au statut ACCEPTE."));
		}

		// 1. Créer l'enregistrement Paiement (statut INITIE)
		Paiement paiement = paiementService.creerPaiementInitie(demande, request.getMontant(),
				request.getMoyenPaiement());

		// 2. Appel API FedaPay
		FedaPayService.FedaPayTransactionResult result = fedaPayService.creerTransaction(
				request.getMontant(),
				"Demande #" + demande.getId() + " — Cabinet Traduction Togo",
				demande.getClient().getEmail(), callbackUrl);

		// 3. Stocker la référence FedaPay
		String ref = result.getTransaction().getReference();
		paiement.setReferenceTransactionFedaPay(ref);
		paiementRepository.save(paiement);

		return ResponseEntity.status(HttpStatus.CREATED).body(new InitPaiementResponse(
				paiement.getId(), result.getTransaction().getApproval_url(), ref));
	}

	// ─────────────────────────────────────────────────────────────────────────
	// POST /api/paiement/webhook
	// ─────────────────────────────────────────────────────────────────────────

	/**
	 * Reçoit les notifications FedaPay (webhook).
	 * Vérifie la signature HMAC-SHA256 avant toute mise à jour en base.
	 */
	@PostMapping("/webhook")
	public ResponseEntity<Void> webhook(
			@RequestHeader(value = "X-FedaPay-Signature", required = false) String signature,
			@RequestBody byte[] payload) throws IOException {

		// 1. Vérification de la signature
		if (signature == null || !fedaPayService.verifierSignature(payload, signature)) {
			log.warn("Webhook FedaPay rejeté : signature invalide ou absente");
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		// 2. Déléguer le traitement au service
		paiementService.traiterWebhook(payload);

		return ResponseEntity.ok().build();
	}

	@org.springframework.web.bind.annotation.ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<Map<String, String>> gererErreur(IllegalArgumentException ex) {
		return ResponseEntity.badRequest().body(Map.of("erreur", ex.getMessage()));
	}

}
