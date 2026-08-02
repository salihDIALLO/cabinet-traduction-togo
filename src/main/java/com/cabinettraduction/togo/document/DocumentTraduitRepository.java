package com.cabinettraduction.togo.document;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository JPA pour {@link DocumentTraduit}.
 */
public interface DocumentTraduitRepository extends JpaRepository<DocumentTraduit, Integer> {

	List<DocumentTraduit> findByDemandeDevisId(Integer demandeDevisId);

}
