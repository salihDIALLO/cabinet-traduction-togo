package com.cabinettraduction.togo.document;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.cabinettraduction.togo.auth.UtilisateurRepository;
import com.cabinettraduction.togo.config.EmailService;
import com.cabinettraduction.togo.config.S3StorageService;
import com.cabinettraduction.togo.devis.DemandeDevis;
import com.cabinettraduction.togo.devis.DemandeDevisRepository;
import com.cabinettraduction.togo.devis.FileValidationException;
import com.cabinettraduction.togo.devis.FileValidationService;
import com.cabinettraduction.togo.devis.StatutDemande;
import com.cabinettraduction.togo.paiement.PaiementRepository;
import com.cabinettraduction.togo.paiement.StatutPaiement;
/**
 * Gestion de l'upload des documents traduits par l'équipe admin/éditeur.
 *
 * Règle métier : un document traduit ne peut être uploadé que si la
 * demande possède au moins un Paiement au statut REUSSI.
 *
 * Flux :
 * 1. Admin : POST /admin/devis/{id}/document-traduit (multipart fichier)
 * 2. Validation MIME réel (Tika) + taille max 10 Mo
 * 3. Upload S3 dans documents-traduits/{demandeId}/
 * 4. Demande passe en LIVREE
 * 5. Email client avec lien vers la page de téléchargement
 *
 * Endpoint de téléchargement client : GET /client/demandes/{id}/telechargement
 * (à implémenter au prompt 3 — génère une URL pré-signée S3)
 */
@RestController
@RequestMapping("/admin/devis")
public class DocumentTraduitController {

	private static final Logger log = LoggerFactory.getLogger(DocumentTraduitController.class);

	private final DemandeDevisRepository demandeDevisRepository;

	private final DocumentTraduitRepository documentTraduitRepository;

	private final PaiementRepository paiementRepository;

	private final S3StorageService s3StorageService;

	private final FileValidationService fileValidationService;

	private final EmailService emailService;

	private final UtilisateurRepository utilisateurRepository;

	private final TelechargementService telechargementService;

	@Value("${app.base-url:http://localhost:8080}")
	private String baseUrl;

	public DocumentTraduitController(DemandeDevisRepository demandeDevisRepository,
			DocumentTraduitRepository documentTraduitRepository,
			PaiementRepository paiementRepository,
			S3StorageService s3StorageService,
			FileValidationService fileValidationService,
			EmailService emailService,
			UtilisateurRepository utilisateurRepository,
			TelechargementService telechargementService) {
		this.demandeDevisRepository = demandeDevisRepository;
		this.documentTraduitRepository = documentTraduitRepository;
		this.paiementRepository = paiementRepository;
		this.s3StorageService = s3StorageService;
		this.fileValidationService = fileValidationService;
		this.emailService = emailService;
		this.utilisateurRepository = utilisateurRepository;
		this.telechargementService = telechargementService;
	}

	// ─── POST /admin/devis/{id}/document-traduit ──────────────────────────────

	/**
	 * Upload le document traduit pour une demande dont le paiement est confirmé.
	 *
	 * Multipart params :
	 * - fichier (obligatoire) : le fichier traduit (PDF, DOCX, etc.)
	 *
	 * Réponse 201 :
	 * { "documentId": 1, "nomFichier": "...", "statut": "LIVREE",
	 *   "lienTelechargement": "http://…", "message": "…" }
	 *
	 * Erreurs :
	 * - 400 : fichier invalide (type MIME, taille)
	 * - 404 : demande introuvable
	 * - 409 : aucun paiement confirmé pour cette demande
	 */
	@PostMapping("/{id}/document-traduit")
	@PreAuthorize("hasAnyRole('ADMIN','EDITEUR')")
	public ResponseEntity<?> uploadDocumentTraduit(
			@PathVariable Integer id,
			@RequestParam("fichier") MultipartFile fichier,
			@AuthenticationPrincipal UserDetails userDetails) throws IOException {

		// 1. Charger la demande
		DemandeDevis demande = demandeDevisRepository.findById(id)
			.orElseThrow(() -> new IllegalArgumentException("Demande introuvable : " + id));

		// 2. Vérifier qu'il existe un Paiement REUSSI
		boolean paiementConfirme = paiementRepository.findByDemandeDevisId(id)
			.stream()
			.anyMatch(p -> StatutPaiement.REUSSI.equals(p.getStatut()));

		if (!paiementConfirme) {
			return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(Map.of("erreur",
						"Upload impossible : aucun paiement confirmé pour la demande #" + id + "."));
		}

		// 3. Valider le fichier (MIME réel via Tika, 10 Mo max)
		// FileValidationException est interceptée par @ExceptionHandler ci-dessous
		fileValidationService.valider(fichier);

		// 4. Upload S3 dans documents-traduits/
		String cleS3 = s3StorageService.uploadTraduit(fichier, id);

		// 5. Résoudre l'utilisateur connecté (peut être null si non trouvé)
		var uploader = utilisateurRepository.findByEmail(userDetails.getUsername()).orElse(null);

		// 6. Persister DocumentTraduit
		DocumentTraduit doc = new DocumentTraduit();
		doc.setDemandeDevis(demande);
		doc.setNomFichier(fichier.getOriginalFilename());
		doc.setCheminS3(cleS3);
		doc.setTypeMime(fichier.getContentType());
		doc.setTailleOctets(fichier.getSize());
		doc.setUploadePar(uploader);
		documentTraduitRepository.save(doc);

		// 7. Passer la demande en LIVREE
		demande.setStatut(StatutDemande.LIVREE);
		demandeDevisRepository.save(demande);

		// 8. Générer le token client et construire le lien avec le token
		String tokenValeur = telechargementService.creerToken(doc);
		String lien = baseUrl + "/devis/" + id + "/telecharger?token=" + tokenValeur;
		try {
			emailService.envoyerTraductionPrete(
					demande.getClient().getEmail(),
					demande.getClient().getNom(),
					id, lien);
		}
		catch (Exception e) {
			// Email non critique : on log mais on ne fait pas échouer la requête
			log.warn("Email 'traduction prête' non envoyé pour demande #{} : {}", id,
					e.getMessage());
		}

		log.info(
				"Document traduit uploadé — demande={}, fichier={}, clé S3={}, uploadePar={}",
				id, fichier.getOriginalFilename(), cleS3,
				uploader != null ? uploader.getEmail() : "inconnu");

		return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
				"documentId", doc.getId(),
				"nomFichier", doc.getNomFichier(),
				"statut", "LIVREE",
				"lienTelechargement", lien,
				"message", "Document traduit uploadé. Le client a été notifié par email."));
	}

	// ─── GET /admin/devis/{id}/document-traduit ───────────────────────────────

	/**
	 * Liste les documents traduits déjà uploadés pour une demande.
	 */
	@GetMapping("/{id}/document-traduit")
	@PreAuthorize("hasAnyRole('ADMIN','EDITEUR')")
	public ResponseEntity<List<DocumentTraduit>> lister(@PathVariable Integer id) {
		return ResponseEntity.ok(documentTraduitRepository.findByDemandeDevisId(id));
	}

	// ─── Gestion des erreurs ──────────────────────────────────────────────────

	@ExceptionHandler(FileValidationException.class)
	public ResponseEntity<Map<String, String>> gererErreurFichier(FileValidationException ex) {
		return ResponseEntity.badRequest().body(Map.of("erreur", ex.getMessage()));
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<Map<String, String>> gererErreur(IllegalArgumentException ex) {
		return ResponseEntity.badRequest().body(Map.of("erreur", ex.getMessage()));
	}

}
