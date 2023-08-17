package org.camunda.bpm.extension.keycloak;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;
import java.util.Properties;

/**
 * The Camunda Showcase Spring Boot application.
 */
@Configuration
public class CamundaExtConfiguration {

    @Value("${keycloak.url}")
    private String keycloakUrl;
    @Value("${keycloak.url.realm}")
    private String keycloakRealm;
    @Value("${keycloak.clientId}")
    private String keycloakClientId;
    @Value("${keycloak.clientSecret}")
    private String keycloakClientSecret;

    /**
     * Secondary datasource.
     * This is used only for publishing data to analytics.
     * 
     * @return
     */
    @Bean("analyticsDS")
    @ConfigurationProperties("analytics.datasource")
    public DataSource analyticsDS() {
        return DataSourceBuilder.create().build();
    }

    /**
     * JDBC template for analytics datasource interaction.
     * 
     * @param analyticsDS
     * @return
     */
    @Bean("analyticsJdbcTemplate")
    public NamedParameterJdbcTemplate analyticsJdbcTemplate(@Qualifier("analyticsDS") DataSource analyticsDS) {
        return new NamedParameterJdbcTemplate(analyticsDS);
    }

    @Bean
    @ConfigurationProperties(prefix = "websocket")
    public Properties messageBrokerProperties() {
        return new Properties();
    }

    @Bean
    Keycloak keycloak() {
        return KeycloakBuilder.builder()
                .serverUrl("https://dev.loginproxy.gov.bc.ca")
                .realm("servicebc")
                .clientId("forms-flow-bpm")
                .grantType("client_credentials")
                .clientSecret("fYdhYzqQcBUQB2oRe0MuauIH4CvBmf4B")
                .build();
    }

}
