package com.cabinettraduction.togo.document;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Endpoint de téléchargement sécurisé pour le client.
 *
 * GET /devis/{id}/telecharger?token={uuid}
 *
 * 1. Vérifie le token (présence, expiration, cohérence avec l'id)
 * 2. Génère une URL pré-signée S3 valable 15 minutes
 * 3. Redirige le client → HTTP 302 → URL pré-signée
 *    (le navigateur télécharge directement depuis S3, sans passer par notre serveur)
 *
 * En cas d'erreur : affiche la vue "telechargement/erreur" avec un message
 * humain — aucun détail technique n'est exposé.
 */
@Controller
public class TelechargementController {

	private static final Logger log = LoggerFactory.getLogger(TelechargementController.class);

	private final TelechargementService telechargementService;

	public TelechargementController(TelechargementService telechargementService) {
		this.telechargementService = telechargementService;
	}

	// ─── GET /devis/{id}/telecharger?token=xxx ────────────────────────────────

	@GetMapping("/devis/{id}/telecharger")
	public String telecharger(@PathVariable Integer id,
			@RequestParam(value = "token", required = false) String token,
			HttpServletResponse response,
			Model model) {

		if (token == null || token.isBlank()) {
			model.addAttribute("messageErreur",
					"Lien invalide. Vérifiez le lien reçu dans votre email ou contactez le cabinet.");
			return "telechargement/erreur";
		}

		// Valide le token et génère l'URL pré-signée (jamais loggée ici)
		String urlPresignee = telechargementService.validerEtGenererUrl(token, id);

		// Redirection HTTP 302 vers l'URL pré-signée S3
		response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
		response.setHeader("Pragma", "no-cache");
		return "redirect:" + urlPresignee;
	}

	// ─── Même endpoint, lien alternatif envoyé dans les emails ───────────────
	// GET /client/demandes/{id}/telechargement → redirige vers le vrai endpoint

	@GetMapping("/client/demandes/{id}/telechargement")
	public String pageLienEmail(@PathVariable Integer id,
			@RequestParam(value = "token", required = false) String token,
			Model model) {

		if (token == null || token.isBlank()) {
			// Afficher une page intermédiaire invitant le client à recontacter le cabinet
			model.addAttribute("demandeId", id);
			model.addAttribute("messageErreur",
					"Ce lien est incomplet. Vérifiez que vous avez cliqué sur le lien complet "
							+ "dans votre email, ou contactez-nous pour obtenir un nouveau lien.");
			return "telechargement/erreur";
		}

		// Déléguer au même endpoint principal
		return "redirect:/devis/" + id + "/telecharger?token=" + token;
	}

	// ─── Gestion des erreurs token ────────────────────────────────────────────

	@ExceptionHandler(TokenInvalideException.class)
	public String gererTokenInvalide(TokenInvalideException ex, Model model) {
		// Message humain uniquement, pas de stacktrace
		model.addAttribute("messageErreur", ex.getMessage());
		model.addAttribute("contact", "WhatsApp ou email");
		log.info("Tentative de téléchargement avec token invalide : {}", ex.getMessage());
		return "telechargement/erreur";
	}

}
