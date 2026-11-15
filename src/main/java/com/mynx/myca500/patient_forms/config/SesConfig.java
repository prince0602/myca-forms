package com.mynx.myca500.patient_forms.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.SesClientBuilder;

@Configuration
public class SesConfig {


    @Bean
    public SesClient sesClient(@Value("${aws.accesskey:}") String accessKey,
                               @Value("${aws.secretkey:}") String secretKey,
                               @Value("${aws.region:us-east-1}") String region) {
        SesClientBuilder builder = SesClient.builder().region(Region.of(region));
        if (!accessKey.isBlank() && !secretKey.isBlank()) {
            builder.credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKey, secretKey)));
        }
        return builder.build();
    }
}
