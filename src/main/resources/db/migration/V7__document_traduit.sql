-- V7 : Table documents_traduits
-- Stocke les fichiers traduits uploadés par les admins/éditeurs après paiement confirmé.

CREATE TABLE IF NOT EXISTS documents_traduits (
    id               INT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    demande_devis_id INT          NOT NULL,
    nom_fichier      VARCHAR(255) NOT NULL,
    chemin_s3        VARCHAR(500) NOT NULL,
    type_mime        VARCHAR(100),
    taille_octets    BIGINT,
    date_upload      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    uploade_par      INT,
    CONSTRAINT fk_dt_demande FOREIGN KEY (demande_devis_id)
        REFERENCES demandes_devis (id),
    CONSTRAINT fk_dt_user FOREIGN KEY (uploade_par)
        REFERENCES utilisateurs (id)
) ENGINE = InnoDB;
