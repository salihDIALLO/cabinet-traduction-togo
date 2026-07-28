package com.cabinettraduction.togo.service;

import java.math.BigDecimal;

import com.cabinettraduction.togo.model.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Service proposé par le cabinet (traduction, interprétation, location…).
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "services")
public class Service extends BaseEntity {

	@NotBlank
	@Column(nullable = false, length = 150)
	private String nom;

	@Column(columnDefinition = "TEXT")
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 40)
	private CategorieService categorie;

	@Column(name = "tarif_indicatif", precision = 10, scale = 2)
	private BigDecimal tarifIndicatif;

}
