package com.mynx.myca500.patient_forms.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;


@Configuration
@EnableConfigurationProperties({ImageProperties.class, PdfProperties.class})
public class S3ClientConfig {

    @Bean
    public S3Client s3Client(@Value("${aws.accesskey:}") String accessKey,
                             @Value("${aws.secretkey:}") String secretKey,
                             @Value("${aws.region:us-east-1}") String region) {
        S3ClientBuilder builder = S3Client.builder().region(Region.of(region));
        if (!accessKey.isBlank() && !secretKey.isBlank()) {
            builder.credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKey, secretKey)));
        }
        return builder.build();
    }
}
