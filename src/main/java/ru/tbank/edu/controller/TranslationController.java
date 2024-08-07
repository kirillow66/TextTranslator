package ru.tbank.edu.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ru.tbank.edu.service.TranslationService;

@RestController
@RequestMapping("/api")
public class TranslationController {

    @Autowired
    private TranslationService translationService;

    @PostMapping("/translate")
    public ResponseEntity<String> translate(@RequestParam String text,
                                            @RequestParam String sourceLang,
                                            @RequestParam String targetLang) {
        try {
            String translatedText = translationService.translate(text, sourceLang, targetLang);
            return ResponseEntity.ok(translatedText);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Ошибка: " + e.getMessage());
        }
    }
    @GetMapping("/")
    public String index(Model model) {
        return "index";
    }
}