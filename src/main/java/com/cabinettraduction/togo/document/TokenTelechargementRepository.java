package com.cabinettraduction.togo.document;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository JPA pour {@link TokenTelechargement}.
 */
public interface TokenTelechargementRepository extends JpaRepository<TokenTelechargement, Integer> {

	Optional<TokenTelechargement> findByToken(String token);

}
