package com.cabinettraduction.togo.blog;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository JPA pour l'entité {@link Article}.
 */
public interface ArticleRepository extends JpaRepository<Article, Integer> {

	Optional<Article> findBySlug(String slug);

	Page<Article> findByPublieTrue(Pageable pageable);

	Page<Article> findByLangueAndPublieTrue(Langue langue, Pageable pageable);

}
