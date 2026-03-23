package itmo.code.controller;

import itmo.code.service.ExcelExportService;
import itmo.code.service.helper.LevelRanges;
import itmo.code.service.TextometrService;
import itmo.code.service.TranslationHistoryService;
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

import java.util.ArrayList;
import java.util.List;

@Controller
public class TranslatorController {

    private final TranslatorService translatorService;
    private final TranslationHistoryService translationHistoryService;
    private final TextometrService textometrService;
    private final ExcelExportService excelExportService;

    public TranslatorController(TranslatorService translatorService,
                                TranslationHistoryService translationHistoryService,
                                TextometrService textometrService,
                                ExcelExportService excelExportService) {
        this.translatorService = translatorService;
        this.translationHistoryService = translationHistoryService;
        this.textometrService = textometrService;
        this.excelExportService = excelExportService;
    }

    @GetMapping("/translator")
    public String showForm(Model model) {
        model.addAttribute("text", "");
        model.addAttribute("translation", "");
        model.addAttribute("promptType", 1);
        model.addAttribute("level", "B1");
        model.addAttribute("error", "");
        model.addAttribute("historyCount", translationHistoryService.getAll().size());
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

            translationHistoryService.save(text, translation, level);

            model.addAttribute("translation", translation);
            model.addAttribute("error", "");
        } catch (RuntimeException e) {
            model.addAttribute("translation", "");
            model.addAttribute("error", e.getMessage());
        }

        model.addAttribute("historyCount", translationHistoryService.getAll().size());
        return "translator";
    }

    @GetMapping("/translator/download")
    public ResponseEntity<Resource> downloadFile() throws Exception {
        List<TranslationHistoryService.StoredTranslation> entries = translationHistoryService.getAll();

        if (entries.isEmpty()) {
            throw new RuntimeException("Нет сохранённых переводов для выгрузки");
        }

        List<ExcelExportService.AnalyzedTranslation> rows = new ArrayList<>();

        for (TranslationHistoryService.StoredTranslation entry : entries) {
            TextometrService.TextometrAnalysis analysis =
                textometrService.analyzeText(entry.translatedText());

            boolean inRange = LevelRanges.isInsideTarget(entry.targetLevel(), analysis.levelNumber());

            rows.add(new ExcelExportService.AnalyzedTranslation(
                entry.targetLevel(),
                entry.translatedText(),
                analysis.levelNumber(),
                analysis.levelComment(),
                inRange
            ));
        }

        double average = rows.stream()
                             .mapToDouble(ExcelExportService.AnalyzedTranslation::levelNumber)
                             .average()
                             .orElseThrow(() -> new RuntimeException("Не удалось вычислить среднее значение"));

        byte[] bytes = excelExportService.generateWorkbook(rows, average);

        translationHistoryService.clear();

        ByteArrayResource resource = new ByteArrayResource(bytes);

        return ResponseEntity.ok()
                             .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=translator-report.xlsx")
                             .contentType(MediaType.parseMediaType(
                                 "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                             .contentLength(bytes.length)
                             .body(resource);
    }
}