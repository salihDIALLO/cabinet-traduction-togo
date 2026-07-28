package com.cabinettraduction.togo.devis;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Corps de la réponse 201 après création d'une demande de devis.
 */
@Data
@AllArgsConstructor
public class DevisResponse {

	private Integer id;

	private String message;

}
