package com.cabinettraduction.togo.auth;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository JPA pour l'entité {@link Utilisateur}.
 */
public interface UtilisateurRepository extends JpaRepository<Utilisateur, Integer> {

	Optional<Utilisateur> findByEmail(String email);

}
