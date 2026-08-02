package com.cabinettraduction.togo.document;

import java.time.LocalDateTime;

import com.cabinettraduction.togo.auth.Utilisateur;
import com.cabinettraduction.togo.devis.DemandeDevis;
import com.cabinettraduction.togo.model.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Document traduit uploadé par un admin/éditeur après confirmation du paiement.
 * Stocké sur S3 dans le préfixe documents-traduits/{demandeId}/.
 * Le client y accède via un lien sécurisé (URL pré-signée).
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "documents_traduits")
public class DocumentTraduit extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "demande_devis_id", nullable = false)
	private DemandeDevis demandeDevis;

	@NotBlank
	@Column(name = "nom_fichier", nullable = false, length = 255)
	private String nomFichier;

	@NotBlank
	@Column(name = "chemin_s3", nullable = false, length = 500)
	private String cheminS3;

	@Column(name = "type_mime", length = 100)
	private String typeMime;

	@Column(name = "taille_octets")
	private Long tailleOctets;

	@Column(name = "date_upload", nullable = false, updatable = false)
	private LocalDateTime dateUpload;

	/** Traducteur ou admin qui a uploadé le fichier. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "uploade_par")
	private Utilisateur uploadePar;

	@PrePersist
	void prePersist() {
		this.dateUpload = LocalDateTime.now();
	}

}
