package com.cabinettraduction.togo.devis;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Contrôleur MVC pour la page publique du formulaire de devis.
 * GET /devis → affiche devis/formulaire.html
 */
@Controller
@RequestMapping("/devis")
public class DevisPageController {

	@GetMapping
	public String formulaire(Model model) {
		// Message WhatsApp pré-rempli spécifique à cette page
		model.addAttribute("whatsappMessage",
				"Bonjour, je souhaite des informations sur une traduction certifiée.");
		return "devis/formulaire";
	}

}
