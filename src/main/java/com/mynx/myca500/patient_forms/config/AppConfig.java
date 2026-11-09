package com.mynx.myca500.patient_forms.config;




import org.carlspring.cloud.storage.s3fs.S3FileSystemProvider;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.util.HashMap;
import java.util.Map;

import static org.carlspring.cloud.storage.s3fs.S3Factory.ACCESS_KEY;
import static org.carlspring.cloud.storage.s3fs.S3Factory.SECRET_KEY;

@Configuration
@EnableConfigurationProperties(TwilioProperties.class)
public class AppConfig implements WebMvcConfigurer {

    private static final String REGION = "s3fs_region";

    @Value("${aws.accesskey}")
    private String accessKey;

    @Value("${aws.secretkey}")
    private String secretKey;

    @Value("${aws.region}")
    private String region;




    @Bean
    public ModelMapper modelMapper() {
        return new ModelMapper();
    }

    @Bean
    public S3FileSystemProvider s3FileSystemProvider() {
        return new S3FileSystemProvider();
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public FileSystem s3FileSystem() throws IOException {
        Map<String, String> env = new HashMap<>();
        env.put(ACCESS_KEY, accessKey);
        env.put(SECRET_KEY, secretKey);
        env.put(REGION, region);
        return FileSystems.newFileSystem(
                URI.create("s3:///"),
                env,
                Thread.currentThread().getContextClassLoader());
    }

    @Bean
    public S3Presigner s3Presigner() {
        AwsBasicCredentials awsCreds = AwsBasicCredentials.create(accessKey, secretKey);

        return S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
                .build();
    }
}
