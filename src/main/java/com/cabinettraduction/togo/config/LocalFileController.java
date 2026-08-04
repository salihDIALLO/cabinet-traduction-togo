package com.cabinettraduction.togo.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Contrôleur de fichiers locaux pour le mode développement.
 *
 * Activé uniquement quand : app.storage.mode=local
 *
 * Sert GET /uploads-dev/** → lit le fichier depuis uploads-dev/ sur le disque.
 * Ce contrôleur ne doit JAMAIS être activé en production.
 */
@RestController
@RequestMapping("/uploads-dev")
@ConditionalOnProperty(name = "app.storage.mode", havingValue = "local")
public class LocalFileController {

	private static final Logger log = LoggerFactory.getLogger(LocalFileController.class);

	private final LocalDocumentStorageService storageService;

	public LocalFileController(LocalDocumentStorageService storageService) {
		this.storageService = storageService;
	}

	/**
	 * Sert un fichier depuis le dossier uploads-dev/.
	 * URL : /uploads-dev/devis/42/uuid_contrat.pdf
	 */
	@GetMapping("/**")
	public ResponseEntity<Resource> servir(@PathVariable(required = false) String path,
			jakarta.servlet.http.HttpServletRequest request) throws IOException {

		// Extraire la clé relative depuis l'URL
		String cle = request.getRequestURI().substring("/uploads-dev/".length());

		Path fichier = storageService.resoudreCheminAbsolu(cle);

		if (!Files.exists(fichier) || !Files.isReadable(fichier)) {
			log.warn("Fichier local introuvable : {}", cle);
			return ResponseEntity.notFound().build();
		}

		// Déterminer le type MIME
		String contentType = Optional.ofNullable(Files.probeContentType(fichier))
			.orElse(MediaType.APPLICATION_OCTET_STREAM_VALUE);

		String nomFichier = fichier.getFileName().toString();
		// Supprimer le préfixe UUID pour afficher un nom lisible
		if (nomFichier.length() > 37 && nomFichier.charAt(36) == '_') {
			nomFichier = nomFichier.substring(37);
		}

		log.info("Fichier servi localement — cle={}", cle);

		return ResponseEntity.ok()
			.contentType(MediaType.parseMediaType(contentType))
			.header(HttpHeaders.CONTENT_DISPOSITION,
					"attachment; filename=\"" + nomFichier + "\"")
			.body(new PathResource(fichier));
	}

}
