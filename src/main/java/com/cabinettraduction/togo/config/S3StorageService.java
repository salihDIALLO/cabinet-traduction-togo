package com.cabinettraduction.togo.config;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * Implémentation S3 de {@link DocumentStorageService} (production).
 *
 * Activée quand : app.storage.mode=s3 (valeur par défaut).
 *
 * Chemins S3 :
 * - devis/{demandeId}/...             → documents originaux du client
 * - translations/{demandeId}/...      → traductions intermédiaires
 * - documents-traduits/{demandeId}/... → documents traduits finaux livrés
 */
@Service
@ConditionalOnProperty(name = "app.storage.mode", havingValue = "s3", matchIfMissing = true)
public class S3StorageService implements DocumentStorageService {

	private static final Logger log = LoggerFactory.getLogger(S3StorageService.class);

	private final S3Client s3Client;

	private final S3Presigner s3Presigner;

	@Value("${cloud.aws.s3.bucket}")
	private String bucket;

	public S3StorageService(
			@Value("${cloud.aws.credentials.access-key}") String accessKey,
			@Value("${cloud.aws.credentials.secret-key}") String secretKey,
			@Value("${cloud.aws.region.static}") String region) {

		StaticCredentialsProvider creds = StaticCredentialsProvider
			.create(AwsBasicCredentials.create(accessKey, secretKey));
		Region awsRegion = Region.of(region);

		this.s3Client = S3Client.builder().region(awsRegion).credentialsProvider(creds).build();
		this.s3Presigner = S3Presigner.builder().region(awsRegion).credentialsProvider(creds).build();

		log.info("S3StorageService initialisé — région={}", region);
	}

	@Override
	public String upload(MultipartFile file, Integer demandeId) throws IOException {
		String key = "devis/%d/%s_%s".formatted(
				demandeId, UUID.randomUUID(), sanitize(file.getOriginalFilename()));
		putObject(key, file);
		return key;
	}

	@Override
	public String uploadTraduction(MultipartFile file, Integer demandeId) throws IOException {
		String key = "translations/%d/%s_%s".formatted(
				demandeId, UUID.randomUUID(), sanitize(file.getOriginalFilename()));
		putObject(key, file);
		return key;
	}

	@Override
	public String uploadTraduit(MultipartFile file, Integer demandeId) throws IOException {
		String key = "documents-traduits/%d/%s_%s".formatted(
				demandeId, UUID.randomUUID(), sanitize(file.getOriginalFilename()));
		putObject(key, file);
		return key;
	}

	@Override
	public S3Presigner getPresigner() {
		return this.s3Presigner;
	}

	@Override
	public boolean supportePresigne() {
		return true;
	}

	/** Accès au client S3 brut si nécessaire (ex. suppressions). */
	public S3Client getS3Client() {
		return this.s3Client;
	}

	// ── Privé ─────────────────────────────────────────────────────────────────

	private void putObject(String key, MultipartFile file) throws IOException {
		PutObjectRequest request = PutObjectRequest.builder()
			.bucket(bucket)
			.key(key)
			.contentType(file.getContentType())
			.contentLength(file.getSize())
			.build();
		s3Client.putObject(request, RequestBody.fromBytes(file.getBytes()));
	}

	private String sanitize(String filename) {
		if (filename == null) {
			return "file";
		}
		return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
	}

}
