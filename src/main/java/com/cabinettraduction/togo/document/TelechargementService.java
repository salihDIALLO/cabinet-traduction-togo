package com.cabinettraduction.togo.document;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import com.cabinettraduction.togo.config.DocumentStorageService;

/**
 * Service de téléchargement sécurisé des documents traduits.
 *
 * Utilise {@link DocumentStorageService} (pas S3StorageService directement)
 * pour être compatible avec le mode local dev.
 *
 * Deux modes selon l'implémentation active :
 * - S3 (prod)   : génère une URL pré-signée AWS (15 min) → redirection HTTP 302
 * - Local (dev) : génère une URL /uploads-dev/** servie par LocalFileController
 */
@Service
public class TelechargementService {

	private static final Logger log = LoggerFactory.getLogger(TelechargementService.class);

	private static final Duration DUREE_TOKEN = Duration.ofHours(24);

	private static final Duration DUREE_PRESIGNEE = Duration.ofMinutes(15);

	private final TokenTelechargementRepository tokenRepository;

	private final DocumentStorageService storageService;

	@Value("${cloud.aws.s3.bucket:dummy-bucket}")
	private String bucket;

	@Value("${app.base-url:http://localhost:8080}")
	private String baseUrl;

	public TelechargementService(TokenTelechargementRepository tokenRepository,
			DocumentStorageService storageService) {
		this.tokenRepository = tokenRepository;
		this.storageService = storageService;
	}

	/**
	 * Crée un token de téléchargement valable 24h pour un document traduit.
	 * @return la valeur UUID à insérer dans le lien email
	 */
	@Transactional
	public String creerToken(DocumentTraduit document) {
		String valeur = UUID.randomUUID().toString();

		TokenTelechargement token = new TokenTelechargement();
		token.setToken(valeur);
		token.setDemandeDevis(document.getDemandeDevis());
		token.setDocumentTraduit(document);
		token.setExpireLe(LocalDateTime.now().plus(DUREE_TOKEN));
		token.setUtilise(false);
		tokenRepository.save(token);

		log.info("Token téléchargement créé — demandeId={}, documentId={}, expireLe={}",
				document.getDemandeDevis().getId(), document.getId(), token.getExpireLe());

		return valeur;
	}

	/**
	 * Valide le token et retourne l'URL de téléchargement.
	 *
	 * - Mode S3    : URL pré-signée AWS (jamais loggée), expire dans 15 min
	 * - Mode local : URL directe /uploads-dev/{cle} servie par LocalFileController
	 *
	 * @param tokenValeur UUID reçu en query param
	 * @param demandeId   id de la demande dans le path URL
	 * @return URL vers laquelle rediriger le client (HTTP 302)
	 * @throws TokenInvalideException si le token est invalide, expiré ou incohérent
	 */
	@Transactional
	public String validerEtGenererUrl(String tokenValeur, Integer demandeId) {
		TokenTelechargement token = tokenRepository.findByToken(tokenValeur)
			.orElseThrow(() -> new TokenInvalideException("Lien invalide ou introuvable."));

		if (!token.getDemandeDevis().getId().equals(demandeId)) {
			log.warn("Token téléchargement : demandeId mismatch (token={}, path={})",
					token.getDemandeDevis().getId(), demandeId);
			throw new TokenInvalideException("Lien invalide ou introuvable.");
		}

		if (token.isUtilise()) {
			throw new TokenInvalideException(
					"Ce lien a déjà été utilisé. Contactez le cabinet.");
		}

		if (token.estExpire()) {
			throw new TokenInvalideException(
					"Ce lien a expiré (validité 24h). Contactez le cabinet.");
		}

		DocumentTraduit document = token.getDocumentTraduit();
		String cle = document.getCheminS3();

		// ── Mode S3 (production) ──────────────────────────────────────────────
		if (storageService.supportePresigne()) {
			S3Presigner presigner = storageService.getPresigner();

			GetObjectRequest req = GetObjectRequest.builder()
				.bucket(bucket)
				.key(cle)
				.responseContentDisposition(
						"attachment; filename=\"" + sanitize(document.getNomFichier()) + "\"")
				.build();

			GetObjectPresignRequest presignReq = GetObjectPresignRequest.builder()
				.signatureDuration(DUREE_PRESIGNEE)
				.getObjectRequest(req)
				.build();

			PresignedGetObjectRequest presigned = presigner.presignGetObject(presignReq);

			// URL pré-signée jamais loggée (contient la signature AWS)
			log.info("URL pré-signée générée (S3) — demandeId={}, documentId={}, expireDans=15min",
					demandeId, document.getId());

			return presigned.url().toString();
		}

		// ── Mode local (dev) ──────────────────────────────────────────────────
		// Servi par LocalFileController sur GET /uploads-dev/{cle}
		String urlLocale = baseUrl + "/uploads-dev/" + cle;
		log.info("URL locale générée (dev) — demandeId={}, documentId={}", demandeId,
				document.getId());
		return urlLocale;
	}

	private String sanitize(String name) {
		if (name == null) {
			return "document.pdf";
		}
		return name.replaceAll("[^a-zA-Z0-9._-]", "_");
	}

}
