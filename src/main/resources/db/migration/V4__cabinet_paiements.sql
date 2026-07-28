-- V4 : Table paiements (Flooz, TMoney, Carte — via FedaPay)

CREATE TABLE IF NOT EXISTS paiements (
    id                            INT           NOT NULL AUTO_INCREMENT PRIMARY KEY,
    demande_devis_id              INT           NOT NULL,
    montant                       DECIMAL(10,2) NOT NULL,
    devise                        VARCHAR(10)   NOT NULL DEFAULT 'XOF',
    moyen_paiement                VARCHAR(20)   NOT NULL,
    reference_transaction_fedapay VARCHAR(100),
    statut                        VARCHAR(15)   NOT NULL DEFAULT 'INITIE',
    date_creation                 DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_p_demande FOREIGN KEY (demande_devis_id) REFERENCES demandes_devis (id)
) ENGINE = InnoDB;
