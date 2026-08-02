package com.cabinettraduction.togo.document;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository pour les documents traduits livrés.
 */
public interface DocumentLivreRepository extends JpaRepository<DocumentLivre, Integer> {

	List<DocumentLivre> findByDemandeDevisId(Integer demandeDevisId);

}
