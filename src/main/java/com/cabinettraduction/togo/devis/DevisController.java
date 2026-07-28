package com.cabinettraduction.togo.devis;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Endpoint REST pour la soumission d'une demande de devis avec pièces jointes.
 *
 * <pre>
 * POST /api/devis  (multipart/form-data)
 *   Champs    : nom, email, telephone, typeClient, serviceId,
 *               langueSource, langueCible, description
 *   Fichiers  : fichiers[] (0–5 fichiers, max 10 Mo chacun, PDF/JPG/PNG/DOCX)
 *   Réponse   : 201 { id, message }  |  400 { erreur }
 * </pre>
 */
@RestController
@RequestMapping("/api/devis")
@Validated
public class DevisController {

	private static final int MAX_FICHIERS = 5;

	private final DevisService devisService;

	public DevisController(DevisService devisService) {
		this.devisService = devisService;
	}

	@PostMapping(consumes = "multipart/form-data")
	public ResponseEntity<?> creerDevis(@Valid @ModelAttribute DevisRequest request,
			@RequestPart(value = "fichiers", required = false) List<MultipartFile> fichiers)
			throws IOException {

		List<MultipartFile> docs = fichiers != null ? fichiers : Collections.emptyList();

		if (docs.size() > MAX_FICHIERS) {
			return ResponseEntity.badRequest()
				.body(Map.of("erreur", "Maximum " + MAX_FICHIERS + " fichiers autorisés."));
		}

		Integer id = devisService.creerDemande(request, docs);
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(new DevisResponse(id, "Demande de devis créée avec succès."));
	}

	@ExceptionHandler(FileValidationException.class)
	public ResponseEntity<Map<String, String>> gererErreurFichier(FileValidationException ex) {
		return ResponseEntity.badRequest().body(Map.of("erreur", ex.getMessage()));
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<Map<String, String>> gererErreurArgument(IllegalArgumentException ex) {
		return ResponseEntity.badRequest().body(Map.of("erreur", ex.getMessage()));
	}

}
