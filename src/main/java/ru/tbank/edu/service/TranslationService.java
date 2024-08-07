package ru.tbank.edu.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.InetAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class TranslationService {
    private static final String IAM_TOKEN = "Bearer y0_AgAAAAAaLkXVAATuwQAAAAEMjjALAADYzyiwXDBLwLVVhvd_0U6Xgxavtg";
    private static final String TRANSLATE_API_URL = "https://translate.api.cloud.yandex.net/translate/v2/translate";

    @Autowired
    private JdbcTemplate jdbcTemplate;

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

        String requestBody = String.format("{\"sourceLanguageCode\":\"%s\",\"targetLanguageCode\":\"%s\",\"texts\":[\"%s\"]}", sourceLang, targetLang, word);
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
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
        String sql = "INSERT INTO translation_requests (ip_address, input_text, translated_text) VALUES (?, ?, ?)";
        jdbcTemplate.update(sql, ipAddress, inputText, translatedText);
    }

    private String getUserIpAddress() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "unknown";
        }
    }
}