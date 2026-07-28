package com.cabinettraduction.togo.owner;

import com.cabinettraduction.togo.model.NamedEntity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Type d'animal (ex : chat, chien, hamster…).
 */
@Entity
@Table(name = "types")
public class PetType extends NamedEntity {

}
