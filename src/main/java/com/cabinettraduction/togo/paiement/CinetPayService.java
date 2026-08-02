package com.cabinettraduction.togo.paiement;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import lombok.Data;

/**
 * Intégration CinetPay v2 — paiements mobiles et cartes en Afrique de l'Ouest.
 *
 * Supports Togo : Flooz (Togocel), Mixx by Yas (Moov Togo), cartes bancaires.
 *
 * Flux :
 * 1. init()      → POST /v2/payment   → obtient payment_url
 * 2. Client paie → CinetPay redirige  → notre notify_url est appelée
 * 3. notify()    → POST /v2/payment/check → on vérifie le statut côté CinetPay
 *
 * Vérification de la notification : CinetPay recommande de re-confirmer
 * le statut via /v2/payment/check plutôt que de faire confiance au POST reçu.
 * Il n'y a pas de signature HMAC côté CinetPay (contrairement à FedaPay).
 *
 * Documentation : https://cinetpay.com/products/api-direct
 * Sandbox : https://sandbox.cinetpay.com (apikey + site_id de test disponibles)
 */
@Service
public class CinetPayService {

	private static final Logger log = LoggerFactory.getLogger(CinetPayService.class);

	/** Endpoint de production CinetPay v2. */
	static final String API_URL = "https://api-checkout.cinetpay.com/v2";

	private final WebClient webClient;

	@Value("${app.cinetpay.api-key}")
	String apiKey;

	@Value("${app.cinetpay.site-id}")
	String siteId;

	@Value("${app.cinetpay.notify-url}")
	String notifyUrl;

	@Value("${app.cinetpay.return-url}")
	String returnUrl;

	public CinetPayService(WebClient.Builder builder) {
		this.webClient = builder
			.defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
			.defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
			.build();
	}

	// ── API publique ─────────────────────────────────────────────────────────

	/**
	 * Initialise un paiement CinetPay.
	 *
	 * @param montant montant en XOF (entier)
	 * @param description description affichée au client sur la page CinetPay
	 * @param clientNom nom de famille du client
	 * @param clientPrenom prénom du client
	 * @param clientEmail email du client
	 * @return InitResult contenant le transactionId généré, payment_url et code
	 */
	public InitResult init(BigDecimal montant, String description,
			String clientNom, String clientPrenom, String clientEmail) {

		String txId = genererTransactionId();

		Map<String, Object> body = Map.ofEntries(
				Map.entry("apikey", apiKey),
				Map.entry("site_id", siteId),
				Map.entry("transaction_id", txId),
				Map.entry("amount", montant.intValue()),
				Map.entry("currency", "XOF"),
				Map.entry("description", description),
				Map.entry("customer_id", clientEmail),
				Map.entry("customer_name", clientNom),
				Map.entry("customer_surname", clientPrenom),
				Map.entry("customer_email", clientEmail),
				// ALL = CinetPay affiche Flooz, Mixx by Yas et carte sur sa propre page
				Map.entry("channels", "ALL"),
				Map.entry("lang", "fr"),
				Map.entry("notify_url", notifyUrl),
				Map.entry("return_url", returnUrl));

		log.info("CinetPay /v2/payment init — txId={}, montant={} XOF", txId, montant);

		try {
			CinetPayApiInitResponse resp = webClient.post()
				.uri(API_URL + "/payment")
				.bodyValue(body)
				.retrieve()
				.bodyToMono(CinetPayApiInitResponse.class)
				.block();

			if (resp == null) {
				return InitResult.failure(txId, "Réponse vide de CinetPay");
			}

			if (resp.isSuccess() && resp.getData() != null) {
				log.info("CinetPay init OK — txId={}, paymentUrl={}", txId, resp.getData().getPayment_url());
				return InitResult.success(txId, resp.getData().getPayment_url(),
						resp.getData().getPayment_token());
			}

			log.warn("CinetPay init KO — code={}, message={}", resp.getCode(), resp.getMessage());
			return InitResult.failure(txId, resp.getMessage());

		}
		catch (WebClientResponseException ex) {
			log.error("CinetPay HTTP {} — {}", ex.getStatusCode(), ex.getResponseBodyAsString());
			return InitResult.failure(txId, "Erreur HTTP CinetPay : " + ex.getStatusCode());
		}
	}

	/**
	 * Re-confirme le statut d'un paiement auprès de CinetPay.
	 * C'est le mécanisme recommandé lors de la réception du webhook notify.
	 * On ne fait jamais confiance au contenu brut du POST entrant.
	 *
	 * @param transactionId l'ID que nous avons généré lors du init()
	 * @return Optional avec le statut vérifié, vide si erreur réseau
	 */
	public Optional<CheckResult> verifierPaiement(String transactionId) {
		Map<String, Object> body = Map.of(
				"apikey", apiKey,
				"site_id", siteId,
				"transaction_id", transactionId);

		log.info("CinetPay /v2/payment/check — txId={}", transactionId);

		try {
			CinetPayApiCheckResponse resp = webClient.post()
				.uri(API_URL + "/payment/check")
				.bodyValue(body)
				.retrieve()
				.bodyToMono(CinetPayApiCheckResponse.class)
				.block();

			if (resp == null || resp.getData() == null) {
				log.warn("CinetPay check : réponse vide pour txId={}", transactionId);
				return Optional.empty();
			}

			String status = resp.getData().getStatus();
			String paymentMethod = resp.getData().getPayment_method();
			log.info("CinetPay check résultat — txId={}, status={}, method={}",
					transactionId, status, paymentMethod);

			return Optional.of(new CheckResult(status, paymentMethod, resp.getData().getAmount()));

		}
		catch (WebClientResponseException ex) {
			log.error("CinetPay check HTTP {} — {}", ex.getStatusCode(), ex.getResponseBodyAsString());
			return Optional.empty();
		}
	}

	// ── Helper ───────────────────────────────────────────────────────────────

	private String genererTransactionId() {
		return "CAB-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
	}

	// ── Résultats (types lisibles pour les appelants) ─────────────────────────

	/** Résultat de l'initialisation d'un paiement. */
	@Data
	public static class InitResult {

		private final String transactionId;

		private final boolean success;

		private final String paymentUrl;

		private final String paymentToken;

		private final String errorMessage;

		public static InitResult success(String txId, String url, String token) {
			return new InitResult(txId, true, url, token, null);
		}

		public static InitResult failure(String txId, String error) {
			return new InitResult(txId, false, null, null, error);
		}

	}

	/** Résultat de la vérification d'un paiement via /v2/payment/check. */
	@Data
	public static class CheckResult {

		/** "ACCEPTED" = réussi, "REFUSED" = refusé, "PENDING" = en attente */
		private final String status;

		/** Ex: "FLOOZ", "MIXX_BY_YAS", "VISA", "MASTERCARD" */
		private final String paymentMethod;

		private final Integer amount;

		public boolean isAccepted() {
			return "ACCEPTED".equalsIgnoreCase(status);
		}

		public boolean isRefused() {
			return "REFUSED".equalsIgnoreCase(status) || "CANCELED".equalsIgnoreCase(status);
		}

	}

	// ── DTOs internes (réponses brutes de l'API CinetPay) ────────────────────

	@Data
	static class CinetPayApiInitResponse {

		private String code;

		private String message;

		private String description;

		private CinetPayPaymentData data;

		/** CinetPay retourne "201" pour succès sur /v2/payment */
		boolean isSuccess() {
			return "201".equals(code);
		}

		@Data
		static class CinetPayPaymentData {

			private String payment_token;

			private String payment_url;

		}

	}

	@Data
	static class CinetPayApiCheckResponse {

		private String code;

		private String message;

		private CinetPayCheckData data;

		@Data
		static class CinetPayCheckData {

			private String status;

			private String transaction_id;

			private Integer amount;

			private String currency;

			private String payment_method;

			private String phone_number;

		}

	}

}
