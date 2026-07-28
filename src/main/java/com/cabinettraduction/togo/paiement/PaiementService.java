package com.cabinettraduction.togo.paiement;

import java.io.IOException;
import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cabinettraduction.togo.devis.DemandeDevis;
import com.cabinettraduction.togo.devis.DemandeDevisRepository;
import com.cabinettraduction.togo.devis.StatutDemande;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Logique métier liée aux paiements FedaPay.
 */
@Service
public class PaiementService {

	private static final Logger log = LoggerFactory.getLogger(PaiementService.class);

	private final PaiementRepository paiementRepository;

	private final DemandeDevisRepository demandeDevisRepository;

	private final ObjectMapper objectMapper;

	public PaiementService(PaiementRepository paiementRepository,
			DemandeDevisRepository demandeDevisRepository, ObjectMapper objectMapper) {
		this.paiementRepository = paiementRepository;
		this.demandeDevisRepository = demandeDevisRepository;
		this.objectMapper = objectMapper;
	}

	/**
	 * Crée un enregistrement Paiement au statut INITIE.
	 */
	@Transactional
	public Paiement creerPaiementInitie(DemandeDevis demande, BigDecimal montant,
			MoyenPaiement moyen) {
		Paiement p = new Paiement();
		p.setDemandeDevis(demande);
		p.setMontant(montant);
		p.setDevise("XOF");
		p.setMoyenPaiement(moyen);
		p.setStatut(StatutPaiement.INITIE);
		return paiementRepository.save(p);
	}

	/**
	 * Traite le payload du webhook FedaPay.
	 *
	 * Format attendu (voir docs.fedapay.com → Webhooks) :
	 * <pre>
	 * {
	 *   "name": "transaction.approved",   // ou transaction.declined / transaction.canceled
	 *   "data": {
	 *     "object": {
	 *       "reference": "TRX-XXXX",
	 *       "status":    "approved"
	 *     }
	 *   }
	 * }
	 * </pre>
	 */
	@Transactional
	public void traiterWebhook(byte[] payload) throws IOException {
		JsonNode root = objectMapper.readTree(payload);
		String eventName = root.path("name").asText();
		JsonNode objet = root.path("data").path("object");
		String reference = objet.path("reference").asText();

		paiementRepository.findAll().stream()
			.filter(p -> reference.equals(p.getReferenceTransactionFedaPay()))
			.findFirst()
			.ifPresent(paiement -> {
				if ("transaction.approved".equals(eventName)) {
					paiement.setStatut(StatutPaiement.REUSSI);
					// Mettre à jour la demande associée
					DemandeDevis demande = paiement.getDemandeDevis();
					demande.setStatut(StatutDemande.ACCEPTE);
					demandeDevisRepository.save(demande);
					log.info("Paiement #{} approuvé pour la demande #{}", paiement.getId(),
							demande.getId());
				}
				else {
					paiement.setStatut(StatutPaiement.ECHOUE);
					log.info("Paiement #{} échoué (événement: {})", paiement.getId(), eventName);
				}
				paiementRepository.save(paiement);
			});
	}

}
