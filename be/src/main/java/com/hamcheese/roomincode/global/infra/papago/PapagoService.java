package com.hamcheese.roomincode.global.infra.papago;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class PapagoService {

    private final WebClient papagoWebClient;

    public Mono<String> translate(String text, String source, String target) {
        Map<String, String> body = Map.of(
                "source", source,
                "target", target,
                "text", text
        );

        return papagoWebClient.post()
                .uri("/nmt/v1/translation")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(json -> {
                    JsonNode translatedTextNode = json.path("message").path("result").path("translatedText");
                    if (translatedTextNode.isMissingNode() || translatedTextNode.isNull()) {
                        log.warn("Papago response missing translatedText: {}", json);
                        throw new RuntimeException("Translation failed: missing translatedText");
                    }
                    return translatedTextNode.asText();
                })
                .onErrorResume(e -> {
                    log.error("Papago translation error: {}", e.getMessage());
                    return Mono.just(text);
                });
    }
}
