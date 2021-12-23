package org.camunda.bpm.extension.commons.connector.support;

import com.google.gson.JsonObject;
// import org.slf4j.Logger;
import java.util.logging.Logger;
// import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId;


/**
 * This class serves as gateway for all application service interactions.
 *
 * @author sumathi.thirumani@aot-technologies.com
 */
@Service("applicationAccessHandler")
public class ApplicationAccessHandler implements IAccessHandler {

    // private final Logger LOGGER = LoggerFactory.getLogger(ApplicationAccessHandler.class);
    private final Logger LOGGER = Logger.getLogger(ApplicationAccessHandler.class.getName());

    @Autowired
    private WebClient webClient;

    public ResponseEntity<String> exchange(String url, HttpMethod method, String payload) {

        payload = (payload == null) ? new JsonObject().toString() : payload;

        LOGGER.info("Application url::" + url + " method::" + method + " payload::" + payload );
        LOGGER.info("clientRegistrationId::" + clientRegistrationId("keycloak-client"));
        Mono<ResponseEntity<String>> entityMono = webClient.method(method).uri(url)
                .attributes(clientRegistrationId("keycloak-client"))
                .accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(Mono.just(payload), String.class)
                .retrieve()
                .toEntity(String.class);

        ResponseEntity<String> response = entityMono.block();
        return new ResponseEntity<>(response.getBody(), response.getStatusCode());
    }

}