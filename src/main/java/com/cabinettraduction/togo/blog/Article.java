package com.cabinettraduction.togo.blog;

import java.time.LocalDateTime;

import com.cabinettraduction.togo.model.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Article de blog publié sur le site du cabinet.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "articles",
		uniqueConstraints = @UniqueConstraint(name = "uk_articles_slug", columnNames = "slug"))
public class Article extends BaseEntity {

	@NotBlank
	@Column(nullable = false, length = 255)
	private String titre;

	@NotBlank
	@Column(nullable = false, length = 255)
	private String slug;

	@Column(columnDefinition = "TEXT")
	private String contenu;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 5)
	private Langue langue;

	@Column(nullable = false)
	private boolean publie = false;

	@Column(name = "date_creation", nullable = false, updatable = false)
	private LocalDateTime dateCreation;

	@PrePersist
	void prePersist() {
		this.dateCreation = LocalDateTime.now();
	}

}
