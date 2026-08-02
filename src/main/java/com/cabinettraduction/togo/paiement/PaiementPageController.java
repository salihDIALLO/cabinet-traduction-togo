package com.cabinettraduction.togo.paiement;

import java.math.BigDecimal;
import java.util.Optional;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.cabinettraduction.togo.devis.DemandeDevis;
import com.cabinettraduction.togo.devis.DemandeDevisRepository;

/**
 * Contrôleur MVC pour la page de paiement.
 * GET /paiement/{demandeId} → affiche la page CinetPay Seamless.
 */
@Controller
@RequestMapping("/paiement")
public class PaiementPageController {

	private final DemandeDevisRepository demandeDevisRepository;

	public PaiementPageController(DemandeDevisRepository demandeDevisRepository) {
		this.demandeDevisRepository = demandeDevisRepository;
	}

	@GetMapping("/{demandeId}")
	public String pagePaiement(@PathVariable Integer demandeId, Model model) {
		Optional<DemandeDevis> opt = demandeDevisRepository.findById(demandeId);

		if (opt.isEmpty()) {
			return "redirect:/?erreur=demande-introuvable";
		}

		DemandeDevis demande = opt.get();
		BigDecimal montant = demande.getMontantEstime() != null ? demande.getMontantEstime()
				: BigDecimal.ZERO;

		model.addAttribute("demandeId", demandeId);
		model.addAttribute("montant", montant);
		model.addAttribute("service", demande.getService() != null ? demande.getService().getNom() : "Traduction");
		model.addAttribute("nomClient", demande.getClient().getNom());
		model.addAttribute("langueSource", demande.getLangueSource());
		model.addAttribute("langueCible", demande.getLangueCible());
		model.addAttribute("whatsappMessage",
				"Bonjour, je viens de régler ma demande #" + demandeId + ". Merci !");

		return "paiement/paiement";
	}

	@GetMapping("/retour")
	public String retourPaiement(Model model) {
		model.addAttribute("message",
				"Votre paiement est en cours de traitement. Vous recevrez une confirmation par email.");
		return "redirect:/?info=paiement-en-cours";
	}

}
