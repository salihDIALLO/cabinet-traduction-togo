package com.cabinettraduction.togo.config;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Service d'envoi d'emails transactionnels (confirmation de devis, etc.).
 */
@Service
public class EmailService {

	private final JavaMailSender mailSender;

	public EmailService(JavaMailSender mailSender) {
		this.mailSender = mailSender;
	}

	/**
	 * Envoie un email de confirmation au client après création de sa demande.
	 * @param destinataire adresse email du client
	 * @param nomClient prénom ou nom complet du client
	 * @param demandeId identifiant de la demande créée
	 */
	public void envoyerConfirmationDevis(String destinataire, String nomClient, Integer demandeId) {
		SimpleMailMessage msg = new SimpleMailMessage();
		msg.setTo(destinataire);
		msg.setSubject("Votre demande de devis #" + demandeId + " — Cabinet de Traduction Togo");
		msg.setText("""
				Bonjour %s,

				Nous avons bien reçu votre demande de devis (référence #%d).
				Notre équipe vous contactera dans les 24 heures ouvrées.

				Merci de votre confiance.

				Cabinet de Traduction Certifiée et Interprétation — Lomé, Togo
				""".formatted(nomClient, demandeId));
		mailSender.send(msg);
	}

	/**
	 * Notifie le client que sa traduction est prête à télécharger.
	 * @param destinataire email du client
	 * @param nomClient nom complet du client
	 * @param demandeId référence de la demande
	 * @param lienTelechargement URL vers la page de téléchargement (pas le fichier direct)
	 */
	public void envoyerTraductionPrete(String destinataire, String nomClient,
			Integer demandeId, String lienTelechargement) {
		SimpleMailMessage msg = new SimpleMailMessage();
		msg.setTo(destinataire);
		msg.setSubject("Votre traduction #" + demandeId + " est prête — Cabinet Traduction Togo");
		msg.setText("""
				Bonjour %s,

				Votre document traduit (demande #%d) est disponible.
				Cliquez sur le lien ci-dessous pour le télécharger :

				%s

				Ce lien est valable 24 heures. Passé ce délai, connectez-vous
				à votre espace client pour en générer un nouveau.

				En cas de problème, répondez à cet email ou contactez-nous via WhatsApp.

				Cabinet de Traduction Certifiée et Interprétation — Lomé, Togo
				""".formatted(nomClient, demandeId, lienTelechargement));
		mailSender.send(msg);
	}

}
