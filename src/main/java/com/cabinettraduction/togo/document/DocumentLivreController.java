package com.cabinettraduction.togo.document;

import java.io.IOException;
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

import com.cabinettraduction.togo.config.DocumentStorageService;
import com.cabinettraduction.togo.devis.DemandeDevis;
import com.cabinettraduction.togo.devis.DemandeDevisRepository;
import com.cabinettraduction.togo.devis.StatutDemande;

import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

/**
 * Livraison de documents traduits (route legacy conservée).
 *
 * Utilise {@link DocumentStorageService} pour être compatible avec
 * le mode local dev — pas d'injection directe de S3Presigner.
 */
@RestController
public class DocumentLivreController {

	private static final Logger log = LoggerFactory.getLogger(DocumentLivreController.class);

	private static final Duration URL_PRESIGNEE_DUREE = Duration.ofMinutes(15);

	private final DocumentLivreRepository documentLivreRepository;

	private final DemandeDevisRepository demandeDevisRepository;

	private final DocumentStorageService storageService;

	@Value("${cloud.aws.s3.bucket:dummy-bucket}")
	private String bucket;

	public DocumentLivreController(DocumentLivreRepository documentLivreRepository,
			DemandeDevisRepository demandeDevisRepository,
			DocumentStorageService storageService) {
		this.documentLivreRepository = documentLivreRepository;
		this.demandeDevisRepository = demandeDevisRepository;
		this.storageService = storageService;
	}

	// ─── POST /admin/documents/{demandeId}/livrer ─────────────────────────────

	@PostMapping("/admin/documents/{demandeId}/livrer")
	@PreAuthorize("hasAnyRole('ADMIN','EDITEUR')")
	public ResponseEntity<?> livrerDocument(@PathVariable Integer demandeId,
			@RequestParam("fichier") MultipartFile fichier,
			@RequestParam(value = "notes", required = false) String notes) throws IOException {

		DemandeDevis demande = demandeDevisRepository.findById(demandeId)
			.orElseThrow(() -> new IllegalArgumentException("Demande introuvable : " + demandeId));

		String cleS3 = storageService.uploadTraduction(fichier, demandeId);

		DocumentLivre doc = new DocumentLivre();
		doc.setDemandeDevis(demande);
		doc.setNomFichier(fichier.getOriginalFilename());
		doc.setCheminS3(cleS3);
		doc.setTypeMime(fichier.getContentType());
		doc.setTailleOctets(fichier.getSize());
		doc.setNotesTraducteur(notes);
		documentLivreRepository.save(doc);

		demande.setStatut(StatutDemande.LIVREE);
		demandeDevisRepository.save(demande);

		log.info("Document livré — demande #{}, fichier={}, clé={}", demandeId,
				fichier.getOriginalFilename(), cleS3);

		return ResponseEntity.status(HttpStatus.CREATED)
			.body(Map.of("documentId", doc.getId(), "nomFichier", doc.getNomFichier(), "statut",
					"LIVREE", "message", "Document livré avec succès."));
	}

	// ─── GET /admin/documents/{demandeId} ────────────────────────────────────

	@GetMapping("/admin/documents/{demandeId}")
	@PreAuthorize("hasAnyRole('ADMIN','EDITEUR')")
	public ResponseEntity<List<DocumentLivre>> listerDocumentsLivres(@PathVariable Integer demandeId) {
		return ResponseEntity.ok(documentLivreRepository.findByDemandeDevisId(demandeId));
	}

	// ─── GET /api/documents/{documentId}/telecharger ─────────────────────────

	/**
	 * Génère une URL de téléchargement pour un document livré.
	 * En mode S3 : URL pré-signée (15 min).
	 * En mode local : URL directe /uploads-dev/...
	 */
	@GetMapping("/api/documents/{documentId}/telecharger")
	public ResponseEntity<?> telechargerDocument(@PathVariable Integer documentId) {
		DocumentLivre doc = documentLivreRepository.findById(documentId)
			.orElseThrow(
					() -> new IllegalArgumentException("Document introuvable : " + documentId));

		doc.setNbTelechargements(doc.getNbTelechargements() + 1);
		documentLivreRepository.save(doc);

		String url;

		if (storageService.supportePresigne()) {
			S3Presigner presigner = storageService.getPresigner();
			GetObjectRequest req = GetObjectRequest.builder()
				.bucket(bucket)
				.key(doc.getCheminS3())
				.build();
			GetObjectPresignRequest presignReq = GetObjectPresignRequest.builder()
				.signatureDuration(URL_PRESIGNEE_DUREE)
				.getObjectRequest(req)
				.build();
			// URL pré-signée jamais loggée
			url = presigner.presignGetObject(presignReq).url().toString();
			log.info("URL pré-signée (S3) — document #{}", documentId);
		}
		else {
			// Mode local dev
			url = "/uploads-dev/" + doc.getCheminS3();
			log.info("URL locale (dev) — document #{}", documentId);
		}

		return ResponseEntity.ok(Map.of("url", url, "nomFichier", doc.getNomFichier(),
				"expireInMinutes", URL_PRESIGNEE_DUREE.toMinutes()));
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<Map<String, String>> gererErreur(IllegalArgumentException ex) {
		return ResponseEntity.badRequest().body(Map.of("erreur", ex.getMessage()));
	}

}
