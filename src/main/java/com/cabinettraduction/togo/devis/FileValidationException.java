package com.cabinettraduction.togo.devis;

/**
 * Levée quand un fichier joint ne respecte pas les contraintes (taille max, type MIME non
 * autorisé).
 */
public class FileValidationException extends RuntimeException {

	public FileValidationException(String message) {
		super(message);
	}

}
