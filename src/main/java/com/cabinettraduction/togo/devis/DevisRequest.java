package com.cabinettraduction.togo.devis;

import com.cabinettraduction.togo.client.TypeClient;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * DTO de réception pour la création d'une demande de devis (multipart/form-data).
 */
@Data
public class DevisRequest {

	@NotBlank
	private String nom;

	@Email
	@NotBlank
	private String email;

	@NotBlank
	private String telephone;

	@NotNull
	private TypeClient typeClient;

	@NotNull
	private Integer serviceId;

	@NotBlank
	private String langueSource;

	@NotBlank
	private String langueCible;

	private String description;

}
