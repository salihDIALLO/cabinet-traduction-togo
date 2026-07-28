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

}
