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
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Token à usage client pour le téléchargement sécurisé d'un document traduit.
 *
 * Un UUID aléatoire est généré lors de la livraison et inséré dans le lien
 * envoyé au client par email. Ce token est :
 * - distinct du JWT admin (pas de rôle, pas d'authentification)
 * - lié à une seule DemandeDevis
 * - valable 24h (expireAt)
 * - révocable en base à tout moment
 *
 * Le client ouvre : GET /devis/{id}/telecharger?token={uuid}
 * Le backend génère une URL pré-signée S3 valable 15 min et redirige.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "tokens_telechargement")
public class TokenTelechargement extends BaseEntity {

	@Column(name = "token", nullable = false, unique = true, length = 64)
	private String token;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "demande_devis_id", nullable = false)
	private DemandeDevis demandeDevis;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "document_traduit_id", nullable = false)
	private DocumentTraduit documentTraduit;

	@Column(name = "cree_le", nullable = false, updatable = false)
	private LocalDateTime creeLe;

	@Column(name = "expire_le", nullable = false)
	private LocalDateTime expireLe;

	@Column(nullable = false)
	private boolean utilise = false;

	@PrePersist
	void prePersist() {
		this.creeLe = LocalDateTime.now();
	}

	public boolean estExpire() {
		return LocalDateTime.now().isAfter(this.expireLe);
	}

	public boolean estValide() {
		return !utilise && !estExpire();
	}

}
