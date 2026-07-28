package com.cabinettraduction.togo.client;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository JPA pour l'entité {@link Client}.
 */
public interface ClientRepository extends JpaRepository<Client, Integer> {

	Optional<Client> findByEmail(String email);

}
