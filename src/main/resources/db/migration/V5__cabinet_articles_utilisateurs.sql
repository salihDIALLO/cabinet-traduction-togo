-- V5 : Tables blog (articles) et gestion des utilisateurs back-office

CREATE TABLE IF NOT EXISTS articles (
    id            INT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    titre         VARCHAR(255) NOT NULL,
    slug          VARCHAR(255) NOT NULL,
    contenu       TEXT,
    langue        VARCHAR(5)   NOT NULL DEFAULT 'FR',
    publie        TINYINT(1)   NOT NULL DEFAULT 0,
    date_creation DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_articles_slug UNIQUE (slug)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS utilisateurs (
    id                INT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    email             VARCHAR(150) NOT NULL,
    mot_de_passe_hash VARCHAR(255) NOT NULL,
    role              VARCHAR(15)  NOT NULL,
    CONSTRAINT uk_utilisateurs_email UNIQUE (email)
) ENGINE = InnoDB;
