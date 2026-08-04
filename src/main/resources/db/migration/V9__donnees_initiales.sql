-- V9 : Données initiales — Services du cabinet de traduction
-- Ces données sont nécessaires au fonctionnement du formulaire de devis.

INSERT INTO services (nom, description, categorie, tarif_indicatif)
SELECT 'Traduction Certifiée',
       'Traduction officielle de documents administratifs et juridiques avec certification du cabinet.',
       'TRADUCTION_CERTIFIEE', 5000.00
WHERE NOT EXISTS (SELECT 1 FROM services WHERE categorie = 'TRADUCTION_CERTIFIEE');

INSERT INTO services (nom, description, categorie, tarif_indicatif)
SELECT 'Traduction Assermentée',
       'Traduction réalisée par un traducteur assermenté auprès des tribunaux togolais.',
       'TRADUCTION_ASSERMENTEE', 8000.00
WHERE NOT EXISTS (SELECT 1 FROM services WHERE categorie = 'TRADUCTION_ASSERMENTEE');

INSERT INTO services (nom, description, categorie, tarif_indicatif)
SELECT 'Interprétation',
       'Interprétation consécutive et simultanée pour conférences, réunions et audiences.',
       'INTERPRETATION', NULL
WHERE NOT EXISTS (SELECT 1 FROM services WHERE categorie = 'INTERPRETATION');

INSERT INTO services (nom, description, categorie, tarif_indicatif)
SELECT 'Location d''Équipement',
       'Location de matériel d''interprétation (cabines, casques, micros) pour vos événements.',
       'LOCATION_EQUIPEMENT', NULL
WHERE NOT EXISTS (SELECT 1 FROM services WHERE categorie = 'LOCATION_EQUIPEMENT');
