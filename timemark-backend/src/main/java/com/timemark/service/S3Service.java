package com.timemark.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * Uploads generated payslip PDFs to S3. Only instantiated when app.s3.enabled=true,
 * so the app starts fine without any AWS credentials in every other environment
 * (local dev, CI, a demo without S3 configured).
 *
 * Credentials come from the AWS SDK's default provider chain (environment variables,
 * an EC2/ECS instance role, or ~/.aws/credentials) — nothing is hardcoded here.
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "app.s3", name = "enabled", havingValue = "true")
public class S3Service {

    private final S3Client s3Client;

    @Value("${app.s3.bucket}")
    private String bucket;

    public S3Service(@Value("${app.s3.region}") String region) {
        this.s3Client = S3Client.builder().region(Region.of(region)).build();
    }

    public String uploadPayslip(String key, byte[] pdfBytes) {
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType("application/pdf")
                        .build(),
                RequestBody.fromBytes(pdfBytes)
        );
        String url = "s3://" + bucket + "/" + key;
        log.info("Uploaded payslip to {}", url);
        return url;
    }
}
