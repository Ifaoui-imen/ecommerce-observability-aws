package ai.ecommerce.orderservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.EventBridgeClientBuilder;

import java.net.URI;

@Configuration
public class AwsConfig {

    @Value("${app.aws.region:us-east-1}")
    private String region;

    // Vide sur AWS (on utilise le vrai EventBridge) ; http://localstack:4566 en local
    @Value("${app.aws.endpoint:}")
    private String endpoint;

    @Bean
    public EventBridgeClient eventBridgeClient() {
        EventBridgeClientBuilder builder = EventBridgeClient.builder()
                .region(Region.of(region));
        // Identifiants : chaîne par défaut du SDK
        //  - en local : variables AWS_ACCESS_KEY_ID / AWS_SECRET_ACCESS_KEY
        //  - sur ECS  : rôle IAM de la tâche (LabRole)
        if (endpoint != null && !endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint));
        }
        return builder.build();
    }
}