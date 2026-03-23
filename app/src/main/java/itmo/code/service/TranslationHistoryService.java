package itmo.code.service;

import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.SessionScope;

import java.util.ArrayList;
import java.util.List;

@Service
@SessionScope
public class TranslationHistoryService {

    private final List<StoredTranslation> items = new ArrayList<>();

    public void save(String sourceText, String translatedText, String targetLevel) {
        items.add(new StoredTranslation(sourceText, translatedText, targetLevel));
    }

    public List<StoredTranslation> getAll() {
        return new ArrayList<>(items);
    }

    public void clear() {
        items.clear();
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public record StoredTranslation(
        String sourceText,
        String translatedText,
        String targetLevel
    ) {
    }
}