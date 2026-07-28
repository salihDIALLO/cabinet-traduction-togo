-- V3 : Tables métier — demandes de devis et documents joints (fichiers S3)

CREATE TABLE IF NOT EXISTS demandes_devis (
    id             INT           NOT NULL AUTO_INCREMENT PRIMARY KEY,
    client_id      INT           NOT NULL,
    service_id     INT           NOT NULL,
    langue_source  VARCHAR(10)   NOT NULL,
    langue_cible   VARCHAR(10)   NOT NULL,
    description    TEXT,
    statut         VARCHAR(20)   NOT NULL DEFAULT 'NOUVEAU',
    montant_estime DECIMAL(10,2),
    date_creation  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_dd_client  FOREIGN KEY (client_id)  REFERENCES clients (id),
    CONSTRAINT fk_dd_service FOREIGN KEY (service_id) REFERENCES services (id)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS documents_joints (
    id               INT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    demande_devis_id INT          NOT NULL,
    nom_fichier      VARCHAR(255) NOT NULL,
    chemin_s3        VARCHAR(500) NOT NULL,
    type_mime        VARCHAR(100),
    taille_octets    BIGINT,
    date_upload      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_dj_demande FOREIGN KEY (demande_devis_id) REFERENCES demandes_devis (id)
) ENGINE = InnoDB;
