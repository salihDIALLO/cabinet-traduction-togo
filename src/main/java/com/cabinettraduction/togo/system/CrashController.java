package com.cabinettraduction.togo.system;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Contrôleur utilisé pour tester le comportement lors d'une exception non gérée.
 */
@Controller
class CrashController {

	@GetMapping("/oups")
	public String triggerException() {
		throw new RuntimeException("Expected: controller used to showcase what happens when an exception is thrown");
	}

}
