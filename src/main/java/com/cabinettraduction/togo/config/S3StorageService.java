package com.cabinettraduction.togo.config;

import java.io.IOException;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * Service d'upload de fichiers vers Amazon S3 (AWS SDK v2).
 * Les credentials et la région sont injectés depuis les variables d'environnement.
 */
@Service
public class S3StorageService {

	private final S3Client s3Client;

	@Value("${cloud.aws.s3.bucket}")
	private String bucket;

	public S3StorageService(
			@Value("${cloud.aws.credentials.access-key}") String accessKey,
			@Value("${cloud.aws.credentials.secret-key}") String secretKey,
			@Value("${cloud.aws.region.static}") String region) {

		this.s3Client = S3Client.builder()
			.region(Region.of(region))
			.credentialsProvider(StaticCredentialsProvider.create(
					AwsBasicCredentials.create(accessKey, secretKey)))
			.build();
	}

	/**
	 * Upload un fichier vers S3.
	 * @param file le fichier à uploader
	 * @param demandeId identifiant de la demande (utilisé dans le chemin S3)
	 * @return la clé S3 (chemin relatif dans le bucket)
	 */
	public String upload(MultipartFile file, Integer demandeId) throws IOException {
		String key = "devis/%d/%s_%s".formatted(
				demandeId,
				UUID.randomUUID(),
				sanitize(file.getOriginalFilename()));

		PutObjectRequest request = PutObjectRequest.builder()
			.bucket(bucket)
			.key(key)
			.contentType(file.getContentType())
			.contentLength(file.getSize())
			.build();

		s3Client.putObject(request, RequestBody.fromBytes(file.getBytes()));
		return key;
	}

	/**
	 * Supprime les caractères dangereux du nom de fichier pour la clé S3.
	 */
	private String sanitize(String filename) {
		if (filename == null) {
			return "file";
		}
		return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
	}

}
