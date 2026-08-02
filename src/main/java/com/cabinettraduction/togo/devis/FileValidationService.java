package com.cabinettraduction.togo.devis;

import java.io.IOException;
import java.util.Set;

import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Valide les fichiers joints à une demande de devis. Utilise Apache Tika pour détecter le
 * type MIME réel par magic bytes (résistant aux extensions renommées).
 */
@Service
public class FileValidationService {

	private static final long MAX_SIZE_BYTES = 10L * 1024 * 1024; // 10 Mo

	private static final Set<String> MIME_AUTORISES = Set.of("application/pdf", "image/jpeg", "image/png",
			"application/vnd.openxmlformats-officedocument.wordprocessingml.document");

	private final Tika tika = new Tika();

	/**
	 * Valide la taille et le type MIME d'un fichier.
	 * @throws FileValidationException si la validation échoue
	 */
	public void valider(MultipartFile file) {
		if (file.getSize() > MAX_SIZE_BYTES) {
			throw new FileValidationException("Le fichier '%s' dépasse la taille maximale autorisée (10 Mo)."
				.formatted(file.getOriginalFilename()));
		}

		String mimeDetecte;
		try {
			mimeDetecte = tika.detect(file.getBytes());
		}
		catch (IOException e) {
			throw new FileValidationException(
					"Impossible de lire le fichier '%s'.".formatted(file.getOriginalFilename()));
		}

		if (!MIME_AUTORISES.contains(mimeDetecte)) {
			throw new FileValidationException(
					("Le type du fichier '%s' n'est pas autorisé (%s). " + "Formats acceptés : PDF, JPG, PNG, DOCX.")
						.formatted(file.getOriginalFilename(), mimeDetecte));
		}
	}

}
