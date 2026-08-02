package com.cabinettraduction.togo.client;

import java.time.LocalDateTime;

import com.cabinettraduction.togo.model.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Entité Client — particulier, étudiant, entreprise ou ONG ayant soumis une demande.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "clients", uniqueConstraints = @UniqueConstraint(name = "uk_clients_email", columnNames = "email"))
public class Client extends BaseEntity {

	@NotBlank
	@Column(nullable = false, length = 100)
	private String nom;

	@Email
	@NotBlank
	@Column(nullable = false, length = 150)
	private String email;

	@Column(length = 20)
	private String telephone;

	@Column(length = 20)
	private String whatsapp;

	@Enumerated(EnumType.STRING)
	@Column(name = "type_client", nullable = false, length = 20)
	private TypeClient typeClient;

	@Column(name = "date_creation", nullable = false, updatable = false)
	private LocalDateTime dateCreation;

	@PrePersist
	void prePersist() {
		this.dateCreation = LocalDateTime.now();
	}

}
