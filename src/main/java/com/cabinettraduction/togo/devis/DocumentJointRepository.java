package com.cabinettraduction.togo.devis;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository JPA pour l'entité {@link DocumentJoint}.
 */
public interface DocumentJointRepository extends JpaRepository<DocumentJoint, Integer> {

	List<DocumentJoint> findByDemandeDevisId(Integer demandeDevisId);

}
