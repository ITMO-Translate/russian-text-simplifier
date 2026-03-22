package itmo.code.controller;

import itmo.code.service.TranslatorService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.nio.charset.StandardCharsets;

@Controller
public class TranslatorController {

    private final TranslatorService translatorService;

    public TranslatorController(TranslatorService translatorService) {
        this.translatorService = translatorService;
    }

    @GetMapping("/translator")
    public String showForm(Model model) {
        model.addAttribute("text", "");
        model.addAttribute("translation", "");
        model.addAttribute("promptType", 1);
        model.addAttribute("level", "B1");
        model.addAttribute("error", "");
        return "translator";
    }

    @PostMapping("/translator")
    public String translate(@RequestParam String text,
                            @RequestParam Integer promptType,
                            @RequestParam String level,
                            Model model) {
        model.addAttribute("text", text);
        model.addAttribute("promptType", promptType);
        model.addAttribute("level", level);

        try {
            String translation = translatorService.translate(text, promptType, level);
            model.addAttribute("translation", translation);
            model.addAttribute("error", "");
        } catch (RuntimeException e) {
            model.addAttribute("translation", "");
            model.addAttribute("error", e.getMessage());
        }

        return "translator";
    }

    @GetMapping("/translator/download")
    public ResponseEntity<Resource> downloadFile() {
        String content = "Это тестовый файл для скачивания";
        ByteArrayResource resource = new ByteArrayResource(content.getBytes(StandardCharsets.UTF_8));

        return ResponseEntity.ok()
                             .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=test.txt")
                             .contentType(MediaType.TEXT_PLAIN)
                             .contentLength(resource.contentLength())
                             .body(resource);
    }
}