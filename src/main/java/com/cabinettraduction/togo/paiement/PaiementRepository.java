package com.cabinettraduction.togo.paiement;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository JPA pour l'entité {@link Paiement}.
 */
public interface PaiementRepository extends JpaRepository<Paiement, Integer> {

	List<Paiement> findByDemandeDevisId(Integer demandeDevisId);

}
