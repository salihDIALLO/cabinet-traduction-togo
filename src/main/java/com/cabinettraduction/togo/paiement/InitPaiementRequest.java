package com.cabinettraduction.togo.paiement;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Corps de la requête POST /api/paiement/init.
 */
@Data
public class InitPaiementRequest {

	@NotNull
	private Integer demandeDevisId;

	@NotNull
	@Positive
	private BigDecimal montant;

	private MoyenPaiement moyenPaiement = MoyenPaiement.CARTE;

}
