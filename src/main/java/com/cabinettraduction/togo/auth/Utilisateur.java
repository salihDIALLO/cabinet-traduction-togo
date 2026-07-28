package com.cabinettraduction.togo.auth;

import com.cabinettraduction.togo.model.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Compte utilisateur back-office (admin ou éditeur).
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "utilisateurs",
		uniqueConstraints = @UniqueConstraint(name = "uk_utilisateurs_email", columnNames = "email"))
public class Utilisateur extends BaseEntity {

	@Email
	@NotBlank
	@Column(nullable = false, length = 150)
	private String email;

	@NotBlank
	@Column(name = "mot_de_passe_hash", nullable = false, length = 255)
	private String motDePasseHash;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 15)
	private RoleUtilisateur role;

}
