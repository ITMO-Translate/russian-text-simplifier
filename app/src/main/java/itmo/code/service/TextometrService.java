package itmo.code.service;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

@Service
public class TextometrService {

    private final RestClient client;

    public TextometrService() {
        this.client = RestClient.create("https://api.textometr.ru");
    }

    public TextometrAnalysis analyzeText(String text) {
        if (text == null || text.isBlank()) {
            throw new RuntimeException("Нет текста для анализа");
        }

        Map<String, Object> body = Map.of(
            "text", text,
            "mode", "foreign"
        );

        try {
            Map<?, ?> response = client.post()
                                       .uri("/analyze")
                                       .contentType(MediaType.APPLICATION_JSON)
                                       .body(body)
                                       .retrieve()
                                       .body(Map.class);

            if (response == null) {
                throw new RuntimeException("Текстометр вернул пустой ответ");
            }

            Object textOk = response.get("text_ok");
            if (textOk instanceof Boolean ok && !ok) {
                Object errorMessage = response.get("text_error_message");
                throw new RuntimeException(
                    errorMessage == null || errorMessage.toString().isBlank()
                    ? "Текстометр не смог обработать текст"
                    : errorMessage.toString()
                );
            }

            Object levelNumberObj = response.get("level_number");
            Object levelCommentObj = response.get("level_comment");

            if (levelNumberObj == null || levelCommentObj == null) {
                throw new RuntimeException("Текстометр вернул неполный ответ");
            }

            double levelNumber = ((Number) levelNumberObj).doubleValue();
            String levelComment = levelCommentObj.toString();

            return new TextometrAnalysis(levelNumber, levelComment);

        } catch (HttpClientErrorException e) {
            throw new RuntimeException("Ошибка API Текстометра: " + e.getStatusCode().value());
        } catch (RestClientException e) {
            throw new RuntimeException("Не удалось связаться с API Текстометра");
        }
    }

    public record TextometrAnalysis(double levelNumber, String levelComment) {
    }
}