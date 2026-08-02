package com.cabinettraduction.togo.owner;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository pour l'entité {@link Owner}. Toutes les méthodes suivent les conventions de
 * nommage Spring Data.
 */
public interface OwnerRepository extends JpaRepository<Owner, Integer> {

	Page<Owner> findByLastNameStartingWith(String lastName, Pageable pageable);

	Optional<Owner> findById(Integer id);

}
