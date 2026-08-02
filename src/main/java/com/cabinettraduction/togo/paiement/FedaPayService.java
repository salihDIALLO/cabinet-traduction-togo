package com.cabinettraduction.togo.paiement;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.Data;

/**
 * Intégration FedaPay via WebClient (API REST officielle FedaPay v1).
 *
 * Documentation officielle :
 * <a href="https://docs.fedapay.com">https://docs.fedapay.com</a> — Rubriques
 * "Transactions" et "Webhooks" pour le format exact des payloads. Sandbox :
 * https://sandbox.fedapay.com — clé test disponible après inscription.
 */
@Service
public class FedaPayService {

	private static final Logger log = LoggerFactory.getLogger(FedaPayService.class);

	private static final String FEDAPAY_API_URL = "https://api.fedapay.com/v1";

	private static final String FEDAPAY_SANDBOX_URL = "https://sandbox-api.fedapay.com/v1";

	private final WebClient webClient;

	@Value("${app.fedapay.webhook-secret}")
	private String webhookSecret;

	public FedaPayService(@Value("${app.fedapay.api-key}") String apiKey,
			@Value("${app.fedapay.sandbox:true}") boolean sandbox) {

		String baseUrl = sandbox ? FEDAPAY_SANDBOX_URL : FEDAPAY_API_URL;

		this.webClient = WebClient.builder()
			.baseUrl(baseUrl)
			.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
			.defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
			.defaultHeader("FedaPay-Version", "2018-09-21")
			.build();
	}

	/**
	 * Crée une transaction FedaPay et retourne l'URL de paiement.
	 * @param montant montant en XOF (francs CFA)
	 * @param description description affichée au client
	 * @param clientEmail email du client pour le reçu FedaPay
	 * @param callbackUrl URL de retour après paiement (votre frontend)
	 * @return résultat de la création de transaction
	 */
	public FedaPayTransactionResult creerTransaction(BigDecimal montant, String description, String clientEmail,
			String callbackUrl) {

		Map<String, Object> body = Map.of("description", description, "amount", montant.intValue(), "currency",
				Map.of("iso", "XOF"), "callback_url", callbackUrl, "customer", Map.of("email", clientEmail));

		return webClient.post()
			.uri("/transactions")
			.bodyValue(body)
			.retrieve()
			.bodyToMono(FedaPayTransactionResult.class)
			.block();
	}

	/**
	 * Vérifie la signature HMAC-SHA256 du webhook FedaPay.
	 *
	 * FedaPay envoie la signature dans le header {@code X-FedaPay-Signature}. Le secret
	 * se trouve dans ton tableau de bord FedaPay → Webhooks → Secret. Stocker dans la
	 * variable d'environnement {@code FEDAPAY_WEBHOOK_SECRET}.
	 * @param payload corps brut (raw bytes) du webhook
	 * @param signatureRecue valeur du header X-FedaPay-Signature
	 * @return true si la signature est valide
	 */
	public boolean verifierSignature(byte[] payload, String signatureRecue) {
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			SecretKeySpec keySpec = new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
			mac.init(keySpec);
			byte[] hash = mac.doFinal(payload);
			// Convertir en hex
			StringBuilder sb = new StringBuilder();
			for (byte b : hash) {
				sb.append(String.format("%02x", b));
			}
			String calculee = sb.toString();
			return calculee.equals(signatureRecue);
		}
		catch (NoSuchAlgorithmException | InvalidKeyException e) {
			log.error("Erreur lors de la vérification de la signature FedaPay", e);
			return false;
		}
	}

	// ── DTO interne — réponse FedaPay ─────────────────────────────────────────

	@Data
	public static class FedaPayTransactionResult {

		private Transaction transaction;

		@Data
		public static class Transaction {

			private Long id;

			private String reference;

			private String status;

			private String approval_url;

		}

	}

}
