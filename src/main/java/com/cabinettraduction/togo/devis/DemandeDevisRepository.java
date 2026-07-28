package com.cabinettraduction.togo.devis;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository JPA pour l'entité {@link DemandeDevis}.
 */
public interface DemandeDevisRepository extends JpaRepository<DemandeDevis, Integer> {

	Page<DemandeDevis> findByClientId(Integer clientId, Pageable pageable);

	Page<DemandeDevis> findByStatut(StatutDemande statut, Pageable pageable);

}
