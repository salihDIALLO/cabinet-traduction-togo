package com.cabinettraduction.togo.devis;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.cabinettraduction.togo.client.Client;
import com.cabinettraduction.togo.model.BaseEntity;
import com.cabinettraduction.togo.service.Service;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Demande de devis soumise par un client.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "demandes_devis")
public class DemandeDevis extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "client_id", nullable = false)
	private Client client;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "service_id", nullable = false)
	private Service service;

	@NotBlank
	@Column(name = "langue_source", nullable = false, length = 10)
	private String langueSource;

	@NotBlank
	@Column(name = "langue_cible", nullable = false, length = 10)
	private String langueCible;

	@Column(columnDefinition = "TEXT")
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private StatutDemande statut = StatutDemande.NOUVEAU;

	@Column(name = "montant_estime", precision = 10, scale = 2)
	private BigDecimal montantEstime;

	@Column(name = "date_creation", nullable = false, updatable = false)
	private LocalDateTime dateCreation;

	@OneToMany(mappedBy = "demandeDevis", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<DocumentJoint> documents = new ArrayList<>();

	@PrePersist
	void prePersist() {
		this.dateCreation = LocalDateTime.now();
	}

}
