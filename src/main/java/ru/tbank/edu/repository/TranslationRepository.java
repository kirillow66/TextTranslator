package ru.tbank.edu.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.tbank.edu.model.TranslationRequest;

@Repository
public class TranslationRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public void save(TranslationRequest translationRequest) {
        String sql = "INSERT INTO translation_requests (ip_address, input_text, translated_text) VALUES (?, ?, ?)";
        jdbcTemplate.update(sql, translationRequest.getIpAddress(), translationRequest.getInputText(), translationRequest.getTranslatedText());
    }
}