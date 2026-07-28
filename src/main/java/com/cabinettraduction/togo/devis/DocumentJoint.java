package com.cabinettraduction.togo.devis;

import java.time.LocalDateTime;

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
 * Fichier joint à une demande de devis, stocké sur Amazon S3.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "documents_joints")
public class DocumentJoint extends BaseEntity {

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

	@PrePersist
	void prePersist() {
		this.dateUpload = LocalDateTime.now();
	}

}
