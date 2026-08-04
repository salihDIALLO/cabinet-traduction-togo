package com.cabinettraduction.togo.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * Implémentation locale de {@link DocumentStorageService} pour le développement.
 *
 * Activée quand : app.storage.mode=local
 *
 * Les fichiers sont stockés dans uploads-dev/ à la racine du projet.
 * Ce dossier est exclu de Git (.gitignore).
 *
 * La "clé" retournée est le chemin relatif depuis uploads-dev/ —
 * même format que les clés S3 pour que le code appelant soit identique.
 *
 * Les URLs de téléchargement sont servies par
 * GET /uploads-dev/** via {@link LocalFileController}.
 */
@Service
@ConditionalOnProperty(name = "app.storage.mode", havingValue = "local")
public class LocalDocumentStorageService implements DocumentStorageService {

	private static final Logger log = LoggerFactory.getLogger(LocalDocumentStorageService.class);

	private final Path baseDir;

	public LocalDocumentStorageService(
			@Value("${app.storage.local.base-dir:uploads-dev}") String baseDirPath) {
		this.baseDir = Paths.get(baseDirPath).toAbsolutePath().normalize();
		try {
			Files.createDirectories(this.baseDir);
			log.info("LocalDocumentStorageService initialisé — baseDir={}", this.baseDir);
		}
		catch (IOException e) {
			throw new IllegalStateException("Impossible de créer le dossier uploads-dev : " + this.baseDir, e);
		}
	}

	@Override
	public String upload(MultipartFile file, Integer demandeId) throws IOException {
		return stocker(file, "devis/" + demandeId);
	}

	@Override
	public String uploadTraduction(MultipartFile file, Integer demandeId) throws IOException {
		return stocker(file, "translations/" + demandeId);
	}

	@Override
	public String uploadTraduit(MultipartFile file, Integer demandeId) throws IOException {
		return stocker(file, "documents-traduits/" + demandeId);
	}

	/** Pas de présignature en local — retourne null. */
	@Override
	public S3Presigner getPresigner() {
		return null;
	}

	@Override
	public boolean supportePresigne() {
		return false;
	}

	/**
	 * Retourne le chemin absolu du fichier sur le disque à partir de sa clé.
	 * Utilisé par {@link LocalFileController} pour servir le fichier.
	 */
	public Path resoudreCheminAbsolu(String cle) {
		return baseDir.resolve(cle).normalize();
	}

	// ── Privé ─────────────────────────────────────────────────────────────────

	private String stocker(MultipartFile file, String prefixe) throws IOException {
		String nomFichier = UUID.randomUUID() + "_" + sanitize(file.getOriginalFilename());
		String cle = prefixe + "/" + nomFichier;

		Path destination = baseDir.resolve(cle).normalize();

		// Sécurité : s'assurer que la destination est bien sous baseDir
		if (!destination.startsWith(baseDir)) {
			throw new IllegalArgumentException("Chemin invalide : " + cle);
		}

		Files.createDirectories(destination.getParent());
		Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);

		log.info("Fichier stocké localement — cle={}", cle);
		return cle;
	}

	private String sanitize(String filename) {
		if (filename == null) {
			return "file";
		}
		return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
	}

}
