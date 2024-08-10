package ru.tbank.edu.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.tbank.edu.model.TranslationRequest;
import ru.tbank.edu.repository.TranslationRepository;

import java.net.InetAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class TranslationService {
    private static final String IAM_TOKEN = "Bearer t1.9euelZqez5LJmJWayp6Xl42dnpXLkO3rnpWaiZfMm5eZzI3OiYrMlMeJx5bl8_dUfyFK-e84f1oK_d3z9xQuH0r57zh_Wgr9zef1656VmpWVmYudipzPmInIlcedkYvJ7_zF656VmpWVmYudipzPmInIlcedkYvJ.l74KFRCcQuH3tdzAGup5sC2XToHl6nDdr7-g2vV8PJ2UrcyLUhHga2okR3gBaBWUn3jq-rFhkZjJIKT0RAUwBg";
    private static final String TRANSLATE_API_URL = "https://translate.api.cloud.yandex.net/translate/v2/translate";
    private static final String folderId = "b1grruricbmgsg19q2vm";

    @Autowired
    private TranslationRepository translationRepository;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    public String translate(String inputText, String sourceLang, String targetLang) {
        String[] words = inputText.split(" ");
        CompletableFuture<String>[] futures = new CompletableFuture[words.length];

        for (int i = 0; i < words.length; i++) {
            final String word = words[i];
            futures[i] = CompletableFuture.supplyAsync(() -> callYandexTranslateAPI(word, sourceLang, targetLang), executorService);
        }

        StringBuilder translatedText = new StringBuilder();
        for (CompletableFuture<String> future : futures) {
            translatedText.append(future.join()).append(" ");
        }

        String result = translatedText.toString().trim();
        saveTranslationRequest(getUserIpAddress(), inputText, result);
        return result;
    }

    private String callYandexTranslateAPI(String word, String sourceLang, String targetLang) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", IAM_TOKEN);
        headers.setContentType(MediaType.APPLICATION_JSON);

        String requestBody = String.format("{\"folderId\":\"%s\",\"sourceLanguageCode\":\"%s\",\"targetLanguageCode\":\"%s\",\"texts\":[\"%s\"]}", folderId, sourceLang, targetLang, word);HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.exchange(TRANSLATE_API_URL, HttpMethod.POST, entity, String.class);

        if (response.getStatusCode().is2xxSuccessful()) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode jsonNode = objectMapper.readTree(response.getBody());
                return jsonNode.path("translations").get(0).path("text").asText();
            } catch (Exception e) {
                e.printStackTrace();
                return "Ошибка при обработке ответа от сервиса перевода";
            }
        } else {
            return "Ошибка доступа к ресурсу перевода: " + response.getStatusCode();
        }
    }

    private void saveTranslationRequest(String ipAddress, String inputText, String translatedText) {
        TranslationRequest request = new TranslationRequest();
        request.setIpAddress(ipAddress);
        request.setInputText(inputText);
        request.setTranslatedText(translatedText);
        translationRepository.save(request);
    }

    private String getUserIpAddress() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "unknown";
        }
    }
}