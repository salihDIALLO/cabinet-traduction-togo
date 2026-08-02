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

import com.cabinettraduction.togo.config.S3StorageService;

/**
 * Service de téléchargement sécurisé des documents traduits.
 *
 * Responsabilités :
 * 1. Générer un token UUID (24h) lié à un DocumentTraduit
 * 2. Valider un token reçu et générer une URL pré-signée S3 (15 min)
 * 3. Marquer le token comme utilisé (usage unique optionnel)
 *
 * L'URL pré-signée n'est jamais loggée, jamais stockée en base.
 */
@Service
public class TelechargementService {

	private static final Logger log = LoggerFactory.getLogger(TelechargementService.class);

	/** Durée de validité du token email (lien dans l'email au client). */
	private static final Duration DUREE_TOKEN = Duration.ofHours(24);

	/** Durée de validité de l'URL pré-signée S3 (après clic sur le lien). */
	private static final Duration DUREE_PRESIGNEE = Duration.ofMinutes(15);

	private final TokenTelechargementRepository tokenRepository;

	private final S3StorageService s3StorageService;

	@Value("${cloud.aws.s3.bucket}")
	private String bucket;

	public TelechargementService(TokenTelechargementRepository tokenRepository,
			S3StorageService s3StorageService) {
		this.tokenRepository = tokenRepository;
		this.s3StorageService = s3StorageService;
	}

	/**
	 * Crée un token de téléchargement pour un document traduit.
	 * Appelé après l'upload admin (DocumentTraduitController).
	 *
	 * @return la valeur UUID du token (à insérer dans le lien email)
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
	 * Valide le token et génère une URL pré-signée S3 (15 min).
	 *
	 * Lève {@link TokenInvalideException} si :
	 * - token inconnu en base
	 * - token expiré
	 * - token déjà utilisé
	 * - token ne correspond pas à la demandeId du path
	 *
	 * @param tokenValeur valeur UUID reçue en query param
	 * @param demandeId   id de la demande dans le path URL
	 * @return URL pré-signée S3 — à utiliser pour la redirection HTTP 302
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
			throw new TokenInvalideException("Ce lien a déjà été utilisé. Contactez le cabinet.");
		}

		if (token.estExpire()) {
			throw new TokenInvalideException("Ce lien a expiré (validité 24h). Contactez le cabinet.");
		}

		DocumentTraduit document = token.getDocumentTraduit();
		String cleS3 = document.getCheminS3();

		// Générer l'URL pré-signée S3 (15 min) — jamais loggée
		S3Presigner presigner = s3StorageService.getPresigner();

		GetObjectRequest getObjectRequest = GetObjectRequest.builder()
			.bucket(bucket)
			.key(cleS3)
			.responseContentDisposition(
					"attachment; filename=\"" + sanitize(document.getNomFichier()) + "\"")
			.build();

		GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
			.signatureDuration(DUREE_PRESIGNEE)
			.getObjectRequest(getObjectRequest)
			.build();

		PresignedGetObjectRequest presigned = presigner.presignGetObject(presignRequest);

		// Ne pas logger l'URL (contient la signature AWS)
		log.info("URL pré-signée générée — demandeId={}, documentId={}, expireDans=15min",
				demandeId, document.getId());

		return presigned.url().toString();
	}

	private String sanitize(String name) {
		if (name == null) {
			return "document.pdf";
		}
		return name.replaceAll("[^a-zA-Z0-9._-]", "_");
	}

}
