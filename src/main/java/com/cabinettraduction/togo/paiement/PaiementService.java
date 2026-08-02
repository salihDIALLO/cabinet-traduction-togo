package com.cabinettraduction.togo.paiement;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import com.cabinettraduction.togo.devis.DemandeDevis;
import com.cabinettraduction.togo.devis.DemandeDevisRepository;
import com.cabinettraduction.togo.devis.StatutDemande;

/**
 * Logique métier paiements CinetPay.
 *
 * Le webhook CinetPay envoie un formulaire URL-encoded avec :
 * - cpm_trans_id : transaction_id généré par notre backend
 * - cpm_result : "00" = succès, autre = échec
 * - cpm_error_message : message d'erreur
 * - cpm_amount : montant payé
 * - cpm_payment_config : "MOBILE_MONEY" | "CREDIT_CARD"
 * - cpm_phone_prefixe + tel : numéro du payeur (ex: 228 + 90000000)
 */
@Service
public class PaiementService {

	private static final Logger log = LoggerFactory.getLogger(PaiementService.class);

	private static final String CINETPAY_SUCCESS_CODE = "00";

	private final PaiementRepository paiementRepository;

	private final DemandeDevisRepository demandeDevisRepository;

	public PaiementService(PaiementRepository paiementRepository,
			DemandeDevisRepository demandeDevisRepository) {
		this.paiementRepository = paiementRepository;
		this.demandeDevisRepository = demandeDevisRepository;
	}

	/**
	 * Crée un enregistrement Paiement au statut INITIE.
	 */
	@Transactional
	public Paiement creerPaiementInitie(DemandeDevis demande, BigDecimal montant, MoyenPaiement moyen) {
		Paiement p = new Paiement();
		p.setDemandeDevis(demande);
		p.setMontant(montant);
		p.setDevise("XOF");
		p.setMoyenPaiement(moyen != null ? moyen : MoyenPaiement.FLOOZ);
		p.setStatut(StatutPaiement.INITIE);
		return paiementRepository.save(p);
	}

	/**
	 * Traite la notification webhook CinetPay.
	 *
	 * CinetPay envoie les données en application/x-www-form-urlencoded.
	 * Paramètre clé : cpm_trans_id → correspond à notre transaction_id (CAB-XXXX).
	 */
	@Transactional
	public void traiterNotification(byte[] payload) throws IOException {
		String body = new String(payload, StandardCharsets.UTF_8);

		// Parser le form-urlencoded
		MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		String[] pairs = body.split("&");
		for (String pair : pairs) {
			String[] kv = pair.split("=", 2);
			if (kv.length == 2) {
				params.add(
						UriComponentsBuilder.fromUriString("?" + pair).build().getQueryParams().entrySet()
							.stream()
							.findFirst()
							.map(e -> e.getKey())
							.orElse(kv[0]),
						java.net.URLDecoder.decode(kv[1], StandardCharsets.UTF_8));
			}
		}

		String transactionId = params.getFirst("cpm_trans_id");
		String result = params.getFirst("cpm_result");
		String errorMsg = params.getFirst("cpm_error_message");

		log.info("CinetPay notification reçue — transactionId: {}, result: {}", transactionId, result);

		if (transactionId == null || transactionId.isBlank()) {
			log.warn("Notification CinetPay sans cpm_trans_id — ignorée");
			return;
		}

		// Trouver le paiement par référence
		paiementRepository.findAll()
			.stream()
			.filter(p -> transactionId.equals(p.getReferenceTransactionFedaPay()))
			.findFirst()
			.ifPresentOrElse(paiement -> {
				if (CINETPAY_SUCCESS_CODE.equals(result)) {
					paiement.setStatut(StatutPaiement.REUSSI);
					// Mettre à jour la demande
					DemandeDevis demande = paiement.getDemandeDevis();
					demande.setStatut(StatutDemande.ACCEPTE);
					demandeDevisRepository.save(demande);
					log.info("Paiement #{} REUSSI via CinetPay — demande #{} → ACCEPTE",
							paiement.getId(), demande.getId());
				}
				else {
					paiement.setStatut(StatutPaiement.ECHOUE);
					log.info("Paiement #{} ECHOUE — code: {}, message: {}",
							paiement.getId(), result, errorMsg);
				}
				paiementRepository.save(paiement);
			}, () -> log.warn("Aucun paiement trouvé pour transactionId: {}", transactionId));
	}

}
