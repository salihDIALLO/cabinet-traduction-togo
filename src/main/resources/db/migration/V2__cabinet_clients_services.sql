-- V2 : Tables métier — clients et services du cabinet de traduction

CREATE TABLE IF NOT EXISTS clients (
    id            INT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    nom           VARCHAR(100) NOT NULL,
    email         VARCHAR(150) NOT NULL,
    telephone     VARCHAR(20),
    whatsapp      VARCHAR(20),
    type_client   VARCHAR(20)  NOT NULL,
    date_creation DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_clients_email UNIQUE (email)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS services (
    id              INT            NOT NULL AUTO_INCREMENT PRIMARY KEY,
    nom             VARCHAR(150)   NOT NULL,
    description     TEXT,
    categorie       VARCHAR(40)    NOT NULL,
    tarif_indicatif DECIMAL(10, 2)
) ENGINE = InnoDB;
