-- V8 : Table tokens_telechargement
-- Token UUID à usage client pour le téléchargement sécurisé (distinct du JWT admin).

CREATE TABLE IF NOT EXISTS tokens_telechargement (
    id                  INT         NOT NULL AUTO_INCREMENT PRIMARY KEY,
    token               VARCHAR(64) NOT NULL,
    demande_devis_id    INT         NOT NULL,
    document_traduit_id INT         NOT NULL,
    cree_le             DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expire_le           DATETIME    NOT NULL,
    utilise             TINYINT(1)  NOT NULL DEFAULT 0,
    CONSTRAINT uk_token UNIQUE (token),
    CONSTRAINT fk_tt_demande  FOREIGN KEY (demande_devis_id)    REFERENCES demandes_devis    (id),
    CONSTRAINT fk_tt_document FOREIGN KEY (document_traduit_id) REFERENCES documents_traduits (id)
) ENGINE = InnoDB;
