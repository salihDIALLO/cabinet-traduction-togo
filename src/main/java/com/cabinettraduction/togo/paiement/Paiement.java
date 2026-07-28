package com.cabinettraduction.togo.paiement;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.cabinettraduction.togo.devis.DemandeDevis;
import com.cabinettraduction.togo.model.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Paiement associé à une demande de devis (Flooz, TMoney, Carte via FedaPay).
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "paiements")
public class Paiement extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "demande_devis_id", nullable = false)
	private DemandeDevis demandeDevis;

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal montant;

	@Column(nullable = false, length = 10)
	private String devise = "XOF";

	@Enumerated(EnumType.STRING)
	@Column(name = "moyen_paiement", nullable = false, length = 20)
	private MoyenPaiement moyenPaiement;

	@Column(name = "reference_transaction_fedapay", length = 100)
	private String referenceTransactionFedaPay;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 15)
	private StatutPaiement statut = StatutPaiement.INITIE;

	@Column(name = "date_creation", nullable = false, updatable = false)
	private LocalDateTime dateCreation;

	@PrePersist
	void prePersist() {
		this.dateCreation = LocalDateTime.now();
	}

}
