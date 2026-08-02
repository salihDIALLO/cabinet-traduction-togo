package com.cabinettraduction.togo.admin;

import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cabinettraduction.togo.devis.DemandeDevis;
import com.cabinettraduction.togo.devis.DemandeDevisRepository;
import com.cabinettraduction.togo.devis.StatutDemande;

import lombok.Data;

/**
 * Back-office : gestion des demandes de devis. Accessible aux rôles ADMIN et EDITEUR
 * (configuré dans SecurityConfig).
 */
@RestController
@RequestMapping("/admin/devis")
public class AdminDevisController {

	private final DemandeDevisRepository demandeDevisRepository;

	public AdminDevisController(DemandeDevisRepository demandeDevisRepository) {
		this.demandeDevisRepository = demandeDevisRepository;
	}

	/**
	 * GET /admin/devis?statut=NOUVEAU&page=0&size=20 Liste paginée des demandes,
	 * filtrable par statut.
	 */
	@GetMapping
	@PreAuthorize("hasAnyRole('ADMIN','EDITEUR')")
	public Page<DemandeDevis> lister(@RequestParam(required = false) StatutDemande statut,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {

		Pageable pageable = PageRequest.of(page, size, Sort.by("dateCreation").descending());

		if (statut != null) {
			return demandeDevisRepository.findByStatut(statut, pageable);
		}
		return demandeDevisRepository.findAll(pageable);
	}

	/**
	 * PUT /admin/devis/{id}/statut Change le statut d'une demande (réservé à ADMIN).
	 */
	@PutMapping("/{id}/statut")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> changerStatut(@PathVariable Integer id, @Valid @RequestBody ChangerStatutRequest request) {

		DemandeDevis demande = demandeDevisRepository.findById(id)
			.orElseThrow(() -> new IllegalArgumentException("Demande introuvable : " + id));

		demande.setStatut(request.getStatut());
		demandeDevisRepository.save(demande);

		return ResponseEntity.ok(Map.of("id", id, "statut", demande.getStatut().name()));
	}

	@org.springframework.web.bind.annotation.ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<Map<String, String>> gererErreur(IllegalArgumentException ex) {
		return ResponseEntity.badRequest().body(Map.of("erreur", ex.getMessage()));
	}

	@Data
	public static class ChangerStatutRequest {

		@NotNull
		private StatutDemande statut;

	}

}
