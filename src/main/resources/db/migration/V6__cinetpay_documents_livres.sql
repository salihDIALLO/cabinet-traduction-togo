-- V6 : CinetPay — remplacer TMONEY par YASS + table documents_livres

-- Mise à jour de la colonne moyen_paiement si nécessaire
-- (ALTER COLUMN ne plante pas si la colonne est déjà VARCHAR)

-- Table des documents traduits livrés au client
CREATE TABLE IF NOT EXISTS documents_livres (
    id                 INT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    demande_devis_id   INT          NOT NULL,
    nom_fichier        VARCHAR(255) NOT NULL,
    chemin_s3          VARCHAR(500) NOT NULL,
    type_mime          VARCHAR(100),
    taille_octets      BIGINT,
    notes_traducteur   TEXT,
    date_livraison     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    nb_telechargements INT          NOT NULL DEFAULT 0,
    CONSTRAINT fk_dl_demande FOREIGN KEY (demande_devis_id) REFERENCES demandes_devis (id)
) ENGINE = InnoDB;

-- Ajouter LIVREE comme valeur possible de statut demande
-- (Valeur stockée en VARCHAR, pas de contrainte ENUM en MySQL → pas de migration DDL nécessaire)
