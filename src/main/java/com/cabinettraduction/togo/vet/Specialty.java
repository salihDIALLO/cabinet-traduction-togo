package com.cabinettraduction.togo.vet;

import com.cabinettraduction.togo.model.NamedEntity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Spécialité d'un vétérinaire (ex : dentisterie, chirurgie…).
 */
@Entity
@Table(name = "specialties")
public class Specialty extends NamedEntity {

}
