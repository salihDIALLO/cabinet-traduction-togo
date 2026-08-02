package com.cabinettraduction.togo.document;

import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.cabinettraduction.togo.config.S3StorageService;
import com.cabinettraduction.togo.devis.DemandeDevis;
import com.cabinettraduction.togo.devis.DemandeDevisRepository;
import com.cabinettraduction.togo.devis.StatutDemande;

import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

/**
 * Gestion de la livraison des documents traduits.
 *
 * Flux complet :
 * 1. Traducteur (ADMIN/EDITEUR) uploade la traduction :
 *    POST /admin/documents/{demandeId}/livrer
 *    → Fichier stocké sur S3 dans translations/{demandeId}/
 *    → Statut demande → LIVREE
 *
 * 2. Client (authentifié) télécharge son document traduit :
 *    GET /api/documents/{documentId}/telecharger
 *    → Retourne une URL pré-signée S3 valable 15 minutes
 *
 * 3. Admin liste les documents d'une demande :
 *    GET /admin/documents/{demandeId}
 */
@RestController
public class DocumentLivreController {

	private static final Logger log = LoggerFactory.getLogger(DocumentLivreController.class);

	private static final Duration URL_PRESIGNEE_DUREE = Duration.ofMinutes(15);

	private final DocumentLivreRepository documentLivreRepository;

	private final DemandeDevisRepository demandeDevisRepository;

	private final S3StorageService s3StorageService;

	private final S3Presigner s3Presigner;

	@Value("${cloud.aws.s3.bucket}")
	private String bucket;

	public DocumentLivreController(DocumentLivreRepository documentLivreRepository,
			DemandeDevisRepository demandeDevisRepository, S3StorageService s3StorageService,
			S3Presigner s3Presigner) {
		this.documentLivreRepository = documentLivreRepository;
		this.demandeDevisRepository = demandeDevisRepository;
		this.s3StorageService = s3StorageService;
		this.s3Presigner = s3Presigner;
	}

	// ─── POST /admin/documents/{demandeId}/livrer ─────────────────────────────

	/**
	 * Le traducteur livre un document traduit.
	 * Multipart : fichier + notes (optionnel).
	 * Nécessite le rôle ADMIN ou EDITEUR.
	 */
	@PostMapping("/admin/documents/{demandeId}/livrer")
	@PreAuthorize("hasAnyRole('ADMIN','EDITEUR')")
	public ResponseEntity<?> livrerDocument(@PathVariable Integer demandeId,
			@RequestParam("fichier") MultipartFile fichier,
			@RequestParam(value = "notes", required = false) String notes) throws IOException {

		DemandeDevis demande = demandeDevisRepository.findById(demandeId)
			.orElseThrow(() -> new IllegalArgumentException("Demande introuvable : " + demandeId));

		// Upload vers S3 dans le dossier translations/
		String cleS3 = s3StorageService.uploadTraduction(fichier, demandeId);

		// Enregistrer en base
		DocumentLivre doc = new DocumentLivre();
		doc.setDemandeDevis(demande);
		doc.setNomFichier(fichier.getOriginalFilename());
		doc.setCheminS3(cleS3);
		doc.setTypeMime(fichier.getContentType());
		doc.setTailleOctets(fichier.getSize());
		doc.setNotesTraducteur(notes);
		documentLivreRepository.save(doc);

		// Passer la demande en LIVREE
		demande.setStatut(StatutDemande.LIVREE);
		demandeDevisRepository.save(demande);

		log.info("Document livré — demande #{}, fichier: {}, clé S3: {}",
				demandeId, fichier.getOriginalFilename(), cleS3);

		return ResponseEntity.status(HttpStatus.CREATED)
			.body(Map.of("documentId", doc.getId(), "nomFichier", doc.getNomFichier(),
					"statut", "LIVREE", "message", "Document livré avec succès."));
	}

	// ─── GET /admin/documents/{demandeId} ────────────────────────────────────

	/**
	 * Liste les documents livrés pour une demande.
	 */
	@GetMapping("/admin/documents/{demandeId}")
	@PreAuthorize("hasAnyRole('ADMIN','EDITEUR')")
	public ResponseEntity<List<DocumentLivre>> listerDocumentsLivres(@PathVariable Integer demandeId) {
		return ResponseEntity.ok(documentLivreRepository.findByDemandeDevisId(demandeId));
	}

	// ─── GET /api/documents/{documentId}/telecharger ─────────────────────────

	/**
	 * Génère une URL pré-signée S3 valable 15 minutes pour le client.
	 * Le client doit être authentifié (JWT).
	 *
	 * Retourne :
	 * {
	 *   "url": "https://s3.amazonaws.com/...?X-Amz-Signature=...",
	 *   "nomFichier": "traduction-acte-naissance.pdf",
	 *   "expireInMinutes": 15
	 * }
	 */
	@GetMapping("/api/documents/{documentId}/telecharger")
	public ResponseEntity<?> telechargerDocument(@PathVariable Integer documentId) {
		DocumentLivre doc = documentLivreRepository.findById(documentId)
			.orElseThrow(() -> new IllegalArgumentException("Document introuvable : " + documentId));

		// Incrémenter le compteur de téléchargements
		doc.setNbTelechargements(doc.getNbTelechargements() + 1);
		documentLivreRepository.save(doc);

		// Générer l'URL pré-signée S3
		GetObjectRequest getObjectRequest = GetObjectRequest.builder()
			.bucket(bucket)
			.key(doc.getCheminS3())
			.build();

		GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
			.signatureDuration(URL_PRESIGNEE_DUREE)
			.getObjectRequest(getObjectRequest)
			.build();

		URL urlPresignee = s3Presigner.presignGetObject(presignRequest).url();

		log.info("URL pré-signée générée — document #{}, téléchargement #{}", documentId,
				doc.getNbTelechargements());

		return ResponseEntity.ok(Map.of("url", urlPresignee.toString(), "nomFichier",
				doc.getNomFichier(), "expireInMinutes", URL_PRESIGNEE_DUREE.toMinutes()));
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<Map<String, String>> gererErreur(IllegalArgumentException ex) {
		return ResponseEntity.badRequest().body(Map.of("erreur", ex.getMessage()));
	}

}
