package com.cabinettraduction.togo.devis;

import java.io.IOException;
import java.util.List;

import com.cabinettraduction.togo.client.Client;
import com.cabinettraduction.togo.client.ClientRepository;
import com.cabinettraduction.togo.config.EmailService;
import com.cabinettraduction.togo.config.S3StorageService;
import com.cabinettraduction.togo.service.Service;
import com.cabinettraduction.togo.service.ServiceRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Service métier : création d'une demande de devis avec upload S3.
 */
@org.springframework.stereotype.Service
public class DevisService {

	private static final Logger log = LoggerFactory.getLogger(DevisService.class);

	private final ClientRepository clientRepository;

	private final ServiceRepository serviceRepository;

	private final DemandeDevisRepository demandeDevisRepository;

	private final DocumentJointRepository documentJointRepository;

	private final S3StorageService s3;

	private final EmailService emailService;

	private final FileValidationService fileValidationService;

	public DevisService(ClientRepository clientRepository, ServiceRepository serviceRepository,
			DemandeDevisRepository demandeDevisRepository,
			DocumentJointRepository documentJointRepository, S3StorageService s3,
			EmailService emailService, FileValidationService fileValidationService) {
		this.clientRepository = clientRepository;
		this.serviceRepository = serviceRepository;
		this.demandeDevisRepository = demandeDevisRepository;
		this.documentJointRepository = documentJointRepository;
		this.s3 = s3;
		this.emailService = emailService;
		this.fileValidationService = fileValidationService;
	}

	/**
	 * Crée une demande de devis avec ses fichiers joints.
	 * <ol>
	 * <li>Valide tous les fichiers (taille + MIME) avant toute persistance</li>
	 * <li>Recherche ou crée le client par email</li>
	 * <li>Charge le service demandé</li>
	 * <li>Persiste la demande</li>
	 * <li>Upload chaque fichier vers S3 et enregistre un DocumentJoint</li>
	 * <li>Envoie un email de confirmation (non bloquant)</li>
	 * </ol>
	 * @return l'identifiant de la demande créée
	 */
	@Transactional
	public Integer creerDemande(DevisRequest req, List<MultipartFile> fichiers) throws IOException {

		// 1. Valider tous les fichiers avant de persister quoi que ce soit
		for (MultipartFile f : fichiers) {
			fileValidationService.valider(f);
		}

		// 2. Rechercher ou créer le client par email
		Client client = clientRepository.findByEmail(req.getEmail()).orElseGet(() -> {
			Client c = new Client();
			c.setNom(req.getNom());
			c.setEmail(req.getEmail());
			c.setTelephone(req.getTelephone());
			c.setTypeClient(req.getTypeClient());
			return clientRepository.save(c);
		});

		// 3. Charger le service demandé
		Service service = serviceRepository.findById(req.getServiceId())
			.orElseThrow(() -> new IllegalArgumentException(
					"Service introuvable avec l'id : " + req.getServiceId()));

		// 4. Créer la demande de devis
		DemandeDevis demande = new DemandeDevis();
		demande.setClient(client);
		demande.setService(service);
		demande.setLangueSource(req.getLangueSource());
		demande.setLangueCible(req.getLangueCible());
		demande.setDescription(req.getDescription());
		demande.setStatut(StatutDemande.NOUVEAU);
		DemandeDevis sauvegardee = demandeDevisRepository.save(demande);

		// 5. Uploader chaque fichier vers S3 et créer un DocumentJoint
		for (MultipartFile f : fichiers) {
			String cle = s3.upload(f, sauvegardee.getId());
			DocumentJoint doc = new DocumentJoint();
			doc.setDemandeDevis(sauvegardee);
			doc.setNomFichier(f.getOriginalFilename());
			doc.setCheminS3(cle);
			doc.setTypeMime(f.getContentType());
			doc.setTailleOctets(f.getSize());
			documentJointRepository.save(doc);
		}

		// 6. Email de confirmation — non bloquant sur erreur SMTP
		try {
			emailService.envoyerConfirmationDevis(client.getEmail(), client.getNom(),
					sauvegardee.getId());
		}
		catch (Exception e) {
			log.warn("Échec de l'envoi de l'email de confirmation pour la demande #{} : {}",
					sauvegardee.getId(), e.getMessage());
		}

		return sauvegardee.getId();
	}

}
