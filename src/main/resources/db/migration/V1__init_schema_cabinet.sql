-- V1 : Schéma initial — tables héritées du projet de base (vets, owners, pets, visits)
-- Compatible MySQL / PostgreSQL. H2 utilise ses propres scripts dans db/h2/.

CREATE TABLE IF NOT EXISTS vets (
    id         INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    first_name VARCHAR(30),
    last_name  VARCHAR(30),
    INDEX idx_vets_last_name (last_name)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS specialties (
    id   INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(80),
    INDEX idx_specialties_name (name)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS vet_specialties (
    vet_id       INT NOT NULL,
    specialty_id INT NOT NULL,
    CONSTRAINT fk_vs_vets        FOREIGN KEY (vet_id)       REFERENCES vets (id),
    CONSTRAINT fk_vs_specialties FOREIGN KEY (specialty_id) REFERENCES specialties (id),
    UNIQUE (vet_id, specialty_id)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS types (
    id   INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(80),
    INDEX idx_types_name (name)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS owners (
    id         INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    first_name VARCHAR(30),
    last_name  VARCHAR(30),
    address    VARCHAR(255),
    city       VARCHAR(80),
    telephone  VARCHAR(20),
    INDEX idx_owners_last_name (last_name)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS pets (
    id         INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(30),
    birth_date DATE,
    type_id    INT NOT NULL,
    owner_id   INT,
    INDEX idx_pets_name (name),
    CONSTRAINT fk_pets_owners    FOREIGN KEY (owner_id) REFERENCES owners (id),
    CONSTRAINT fk_pets_types     FOREIGN KEY (type_id)  REFERENCES types (id),
    CONSTRAINT uq_owner_pet_name UNIQUE (owner_id, name)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS visits (
    id          INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    pet_id      INT,
    visit_date  DATE,
    description VARCHAR(255),
    CONSTRAINT fk_visits_pets FOREIGN KEY (pet_id) REFERENCES pets (id)
) ENGINE = InnoDB;
