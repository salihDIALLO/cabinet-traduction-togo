package com.cabinettraduction.togo.paiement;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cabinettraduction.togo.devis.DemandeDevis;
import com.cabinettraduction.togo.devis.DemandeDevisRepository;
import com.cabinettraduction.togo.devis.StatutDemande;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * Endpoints CinetPay dédiés.
 *
 * POST /api/paiement/cinetpay/init   → démarre un paiement Flooz/Mixx/Carte
 * POST /api/paiement/cinetpay/notify → reçoit le webhook CinetPay + re-confirme
 *
 * Ces endpoints coexistent avec les endpoints FedaPay existants.
 * Le provider actif est contrôlé par app.paiement.provider (cinetpay | fedapay).
 */
@RestController
@RequestMapping("/api/paiement/cinetpay")
public class CinetPayController {

	private static final Logger log = LoggerFactory.getLogger(CinetPayController.class);

	private final CinetPayService cinetPayService;

	private final PaiementRepository paiementRepository;

	private final DemandeDevisRepository demandeDevisRepository;

	public CinetPayController(CinetPayService cinetPayService,
			PaiementRepository paiementRepository,
			DemandeDevisRepository demandeDevisRepository) {
		this.cinetPayService = cinetPayService;
		this.paiementRepository = paiementRepository;
		this.demandeDevisRepository = demandeDevisRepository;
	}

	// ─────────────────────────────────────────────────────────────────────────
	// POST /api/paiement/cinetpay/init
	// ─────────────────────────────────────────────────────────────────────────

	/**
	 * Initialise un paiement CinetPay pour une demande acceptée.
	 *
	 * Corps JSON :
	 * { "demandeDevisId": 42, "montant": 15000 }
	 *
	 * Réponse 201 :
	 * {
	 *   "paiementId":    1,
	 *   "transactionId": "CAB-XXXXXXXXXXXX",
	 *   "paymentUrl":    "https://payment.cinetpay.com/...",
	 *   "paymentToken":  "xxx"
	 * }
	 *
	 * Le frontend redirige vers paymentUrl OU utilise CinetPaySeamless.open(paymentToken).
	 * CinetPay présentera lui-même le choix Flooz / Mixx by Yas / Carte.
	 */
	@PostMapping("/init")
	public ResponseEntity<?> init(@Valid @RequestBody InitRequest request) {

		DemandeDevis demande = demandeDevisRepository.findById(request.getDemandeDevisId())
			.orElseThrow(() -> new IllegalArgumentException(
					"Demande introuvable : " + request.getDemandeDevisId()));

		if (demande.getStatut() != StatutDemande.ACCEPTE) {
			return ResponseEntity.badRequest()
				.body(Map.of("erreur",
						"Paiement impossible : la demande doit être au statut ACCEPTE (actuel : "
								+ demande.getStatut() + ")."));
		}

		// Décomposer le nom client
		String nomComplet = demande.getClient().getNom();
		String[] parts = nomComplet.trim().split("\\s+", 2);
		String prenom = parts.length > 1 ? parts[0] : "";
		String nom = parts.length > 1 ? parts[1] : nomComplet;

		String description = "Demande #" + demande.getId()
				+ " — " + demande.getLangueSource() + " → " + demande.getLangueCible()
				+ " — Cabinet Traduction Togo";

		// Appel CinetPay
		CinetPayService.InitResult result = cinetPayService.init(
				request.getMontant(), description, nom, prenom, demande.getClient().getEmail());

		if (!result.isSuccess()) {
			log.error("CinetPay init échoué — demande #{}: {}", demande.getId(), result.getErrorMessage());
			return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
				.body(Map.of("erreur", "Passerelle CinetPay indisponible : " + result.getErrorMessage()));
		}

		// Enregistrer le paiement (moyenPaiement inconnu à ce stade — CinetPay route lui-même)
		Paiement paiement = new Paiement();
		paiement.setDemandeDevis(demande);
		paiement.setMontant(request.getMontant());
		paiement.setDevise("XOF");
		paiement.setMoyenPaiement(MoyenPaiement.FLOOZ); // valeur par défaut, mise à jour au notify
		paiement.setStatut(StatutPaiement.INITIE);
		paiement.setReferenceTransactionFedaPay(result.getTransactionId()); // champ réutilisé
		paiement = paiementRepository.save(paiement);

		log.info("Paiement CinetPay initié — paiementId={}, txId={}, demande=#{}",
				paiement.getId(), result.getTransactionId(), demande.getId());

		return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
				"paiementId", paiement.getId(),
				"transactionId", result.getTransactionId(),
				"paymentUrl", result.getPaymentUrl(),
				"paymentToken", result.getPaymentToken() != null ? result.getPaymentToken() : ""));
	}

	// ─────────────────────────────────────────────────────────────────────────
	// POST /api/paiement/cinetpay/notify
	// ─────────────────────────────────────────────────────────────────────────

	/**
	 * Reçoit la notification de paiement envoyée par CinetPay (notify_url).
	 *
	 * CinetPay envoie le transaction_id en query param : ?cpm_trans_id=CAB-XXX
	 *
	 * IMPORTANT : on ne fait jamais confiance au contenu du POST reçu.
	 * On re-confirme immédiatement via /v2/payment/check côté CinetPay.
	 *
	 * Réponse : toujours 200 pour éviter que CinetPay retente en boucle.
	 */
	@PostMapping("/notify")
	public ResponseEntity<Void> notify(
			@RequestParam(value = "cpm_trans_id", required = false) String transactionId,
			@RequestBody(required = false) byte[] payload) {

		// CinetPay peut envoyer l'ID dans le query param ou dans le body form-encoded
		String txId = transactionId;
		if ((txId == null || txId.isBlank()) && payload != null) {
			txId = extraireTransactionId(payload);
		}

		if (txId == null || txId.isBlank()) {
			log.warn("CinetPay notify sans transaction_id — ignoré");
			return ResponseEntity.ok().build();
		}

		final String finalTxId = txId;

		// Re-confirmation auprès de CinetPay (mécanisme recommandé, pas de HMAC)
		Optional<CinetPayService.CheckResult> checkOpt = cinetPayService.verifierPaiement(finalTxId);

		if (checkOpt.isEmpty()) {
			log.error("CinetPay check a échoué pour txId={} — aucune mise à jour BDD", finalTxId);
			return ResponseEntity.ok().build(); // retourner 200 pour éviter les retentatives
		}

		CinetPayService.CheckResult check = checkOpt.get();

		paiementRepository.findAll()
			.stream()
			.filter(p -> finalTxId.equals(p.getReferenceTransactionFedaPay()))
			.findFirst()
			.ifPresentOrElse(paiement -> {
				if (check.isAccepted()) {
					// Mettre à jour le moyen de paiement réel utilisé
					paiement.setMoyenPaiement(resoudreMoyenPaiement(check.getPaymentMethod()));
					paiement.setStatut(StatutPaiement.REUSSI);

					DemandeDevis demande = paiement.getDemandeDevis();
					demande.setStatut(StatutDemande.ACCEPTE);
					demandeDevisRepository.save(demande);

					log.info("CinetPay paiement REUSSI — paiementId={}, txId={}, method={}, demande=#{}",
							paiement.getId(), finalTxId, check.getPaymentMethod(), demande.getId());
				}
				else {
					paiement.setStatut(StatutPaiement.ECHOUE);
					log.info("CinetPay paiement ECHOUE — paiementId={}, txId={}, status={}",
							paiement.getId(), finalTxId, check.getStatus());
				}
				paiementRepository.save(paiement);
			}, () -> log.warn("Aucun paiement trouvé pour txId={}", finalTxId));

		return ResponseEntity.ok().build();
	}

	// ── Privé ─────────────────────────────────────────────────────────────────

	/**
	 * Extrait cpm_trans_id d'un body application/x-www-form-urlencoded.
	 */
	private String extraireTransactionId(byte[] payload) {
		String body = new String(payload, StandardCharsets.UTF_8);
		for (String pair : body.split("&")) {
			if (pair.startsWith("cpm_trans_id=")) {
				try {
					return java.net.URLDecoder.decode(pair.substring("cpm_trans_id=".length()),
							StandardCharsets.UTF_8);
				}
				catch (Exception e) {
					return pair.substring("cpm_trans_id=".length());
				}
			}
		}
		return null;
	}

	/**
	 * Convertit la valeur CinetPay payment_method en enum MoyenPaiement.
	 */
	private MoyenPaiement resoudreMoyenPaiement(String paymentMethod) {
		if (paymentMethod == null) {
			return MoyenPaiement.FLOOZ;
		}
		return switch (paymentMethod.toUpperCase()) {
			case "FLOOZ" -> MoyenPaiement.FLOOZ;
			case "MIXX_BY_YAS", "YAS", "YASS" -> MoyenPaiement.YASS;
			case "MOOV", "MOOV_MONEY" -> MoyenPaiement.MOOV_MONEY;
			default -> MoyenPaiement.CARTE;
		};
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<Map<String, String>> gererErreur(IllegalArgumentException ex) {
		return ResponseEntity.badRequest().body(Map.of("erreur", ex.getMessage()));
	}

	// ── DTO requête ───────────────────────────────────────────────────────────

	@Data
	public static class InitRequest {

		@NotNull
		private Integer demandeDevisId;

		@NotNull
		@Positive
		private BigDecimal montant;

	}

}
