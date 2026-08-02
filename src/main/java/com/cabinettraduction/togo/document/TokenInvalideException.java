package com.cabinettraduction.togo.document;

/**
 * Levée quand un token de téléchargement est invalide, expiré ou déjà utilisé.
 * Le message est affiché au client — ne pas y inclure de détails techniques.
 */
public class TokenInvalideException extends RuntimeException {

	public TokenInvalideException(String message) {
		super(message);
	}

}
