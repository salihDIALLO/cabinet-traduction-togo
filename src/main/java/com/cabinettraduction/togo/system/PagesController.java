package com.cabinettraduction.togo.system;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Contrôleur pour les pages statiques du site vitrine.
 */
@Controller
public class PagesController {

	@GetMapping("/services")
	public String services() {
		return "services";
	}

	@GetMapping("/a-propos")
	public String aPropos() {
		return "a-propos";
	}

	@GetMapping("/contact")
	public String contact() {
		return "contact";
	}

	@GetMapping("/mentions-legales")
	public String mentionsLegales() {
		return "mentions-legales";
	}

}
