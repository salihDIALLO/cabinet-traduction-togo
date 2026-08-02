package com.cabinettraduction.togo.document;

import java.time.LocalDateTime;

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
 * Document traduit livré au client.
 * Stocké sur S3 — accès sécurisé via URL pré-signée (durée limitée).
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "documents_livres")
public class DocumentLivre extends BaseEntity {

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

	@Column(name = "notes_traducteur", columnDefinition = "TEXT")
	private String notesTraducteur;

	@Column(name = "date_livraison", nullable = false, updatable = false)
	private LocalDateTime dateLivraison;

	/** Nombre de fois que le client a téléchargé ce document */
	@Column(name = "nb_telechargements")
	private int nbTelechargements = 0;

	@PrePersist
	void prePersist() {
		this.dateLivraison = LocalDateTime.now();
	}

}
