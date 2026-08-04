package com.cabinettraduction.togo.config;

import java.io.IOException;

import org.springframework.web.multipart.MultipartFile;

import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * Abstraction du stockage de fichiers.
 *
 * Deux implémentations :
 * - {@link S3StorageService}          activée quand app.storage.mode=s3 (production)
 * - {@link LocalDocumentStorageService} activée quand app.storage.mode=local (dev)
 *
 * La clé retournée par chaque méthode d'upload est un chemin relatif opaque :
 * - En S3 : la clé S3 (ex. "devis/42/uuid_contrat.pdf")
 * - En local : le chemin relatif depuis uploads-dev/ (même format)
 *
 * Les appelants n'ont jamais besoin de savoir où le fichier est stocké.
 */
public interface DocumentStorageService {

	/**
	 * Upload un document original soumis par le client.
	 * Préfixe : devis/{demandeId}/
	 */
	String upload(MultipartFile file, Integer demandeId) throws IOException;

	/**
	 * Upload une traduction brouillon / livraison intermédiaire.
	 * Préfixe : translations/{demandeId}/
	 */
	String uploadTraduction(MultipartFile file, Integer demandeId) throws IOException;

	/**
	 * Upload le document traduit final livré au client.
	 * Préfixe : documents-traduits/{demandeId}/
	 */
	String uploadTraduit(MultipartFile file, Integer demandeId) throws IOException;

	/**
	 * Retourne le S3Presigner pour générer des URLs pré-signées.
	 * En mode local, retourne null — le {@link TelechargementService}
	 * basculera sur une URL locale directe.
	 */
	S3Presigner getPresigner();

	/**
	 * Indique si l'implémentation supporte les URLs pré-signées.
	 * false en mode local, true en mode S3.
	 */
	boolean supportePresigne();

}
