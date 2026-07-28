package com.cabinettraduction.togo.paiement;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Corps de la réponse 201 après initialisation du paiement FedaPay.
 */
@Data
@AllArgsConstructor
public class InitPaiementResponse {

	/** Identifiant interne du paiement (table paiements). */
	private Integer paiementId;

	/** URL de paiement FedaPay vers laquelle rediriger le client. */
	private String urlPaiement;

	/** Référence de la transaction FedaPay. */
	private String referenceTransaction;

}
