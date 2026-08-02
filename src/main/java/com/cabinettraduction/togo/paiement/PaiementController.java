package com.cabinettraduction.togo.paiement;

import java.io.IOException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints FedaPay (conservé pour compatibilité et tests).
 *
 * Les nouveaux endpoints CinetPay sont dans {@link CinetPayController}
 * sous /api/paiement/cinetpay/*.
 *
 * Provider actif configurable via : app.paiement.provider=cinetpay|fedapay
 */
@RestController
@RequestMapping("/api/paiement")
public class PaiementController {

	private static final Logger log = LoggerFactory.getLogger(PaiementController.class);

	private final PaiementService paiementService;

	private final FedaPayService fedaPayService;

	public PaiementController(PaiementService paiementService, FedaPayService fedaPayService) {
		this.paiementService = paiementService;
		this.fedaPayService = fedaPayService;
	}

	/**
	 * Webhook FedaPay — signature HMAC-SHA256.
	 * POST /api/paiement/webhook
	 */
	@PostMapping("/webhook")
	public ResponseEntity<Void> webhookFedaPay(
			@RequestHeader(value = "X-FedaPay-Signature", required = false) String signature,
			@RequestBody byte[] payload) throws IOException {

		if (signature == null || !fedaPayService.verifierSignature(payload, signature)) {
			log.warn("Webhook FedaPay rejeté : signature invalide ou absente");
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		paiementService.traiterWebhook(payload);
		return ResponseEntity.ok().build();
	}

	/**
	 * Notification CinetPay legacy (ancienne route /notify).
	 * Les nouvelles notifications passent par /api/paiement/cinetpay/notify.
	 * Cette route reste pour compatibilité si le notify_url pointe encore ici.
	 * POST /api/paiement/notify
	 */
	@PostMapping("/notify")
	public ResponseEntity<Void> notifyLegacy(@RequestBody byte[] payload) throws IOException {
		log.info("CinetPay notify reçu sur route legacy /api/paiement/notify");
		paiementService.traiterNotification(payload);
		return ResponseEntity.ok().build();
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<Map<String, String>> gererErreur(IllegalArgumentException ex) {
		return ResponseEntity.badRequest().body(Map.of("erreur", ex.getMessage()));
	}

}
