package itmo.code.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

@Service
public class TranslatorService {

    private final RestClient client;
    private final String apiKey;
    private final String model;

    private static final Map<Integer, Map<String, String>> PROMPTS = Map.of(
        1, Map.of(
            "B1", """
                    Возьми следующий текст, написанный на уровне владения языком C1 (продвинутый).
                    Адаптируй его для целевой аудитории уровня B1.

                    Требования:
                    — Упрости синтаксис: разбей сложные предложения на простые, не более 10–12 слов.
                    — Замени отглагольные существительные на глаголы.
                    — Убери причастные и деепричастные обороты, заменяя их придаточными предложениями с союзом который или простыми глагольными конструкциями.
                    — Замени книжную лексику на нейтральную, общеупотребительную.

                    Верни только готовый текст без пояснений и комментариев.

                    Текст:
                    %s
                    """,
            "B2", """
                    Возьми следующий текст, написанный на уровне владения языком C1 (продвинутый).
                    Адаптируй его для целевой аудитории уровня B2.

                    Требования:
                    — Сохрани логическую сложность рассуждений и структуру абзацев.
                    — Замени редкую книжную лексику на общеупотребительные слова и устойчивые обороты, характерные для уровня B2.
                    — Сократи количество придаточных предложений: не более двух придаточных на одно сложное предложение.
                    — Профессиональную терминологию сохрани, но если термин сложный, поясни его значение через перефразирование внутри предложения без использования скобок.

                    Верни только готовый текст без пояснений и комментариев.

                    Текст:
                    %s
                    """
        ),
        2, Map.of(
            "B1", """
                    Роль:
                    Ты — кандидат филологических наук, специалист по русскому языку и редактированию текстов.
                    Твоя задача — упростить сложный текст, сохранив его смысл.

                    Задача:
                    Адаптируй текст для уровня B1.
                    В ответе укажи только готовый текст без заголовков, пояснений и аналитики.

                    Требования:
                    · Пиши короткими предложениями, не больше 12 слов.
                    · Используй только активный залог.
                    · Убери причастия и деепричастия.
                    · Замени сложные и абстрактные слова на простые, повседневные.
                    · Оставляй не больше одной придаточной части в предложении.

                    Текст для обработки:
                    %s
                    """,
            "B2", """
                    Роль:
                    Ты — кандидат филологических наук, специалист по русскому языку и редактированию текстов.
                    Твоя задача — упростить сложный текст, сохранив его смысл.

                    Задача:
                    Адаптируй текст для уровня B2.
                    В ответе укажи только готовый текст без заголовков, пояснений и аналитики.

                    Требования:
                    · Сохрани смысловую глубину и логическую связность текста.
                    · Замени редкие и устаревшие слова на современные и понятные.
                    · Если используешь специальные термины, объясни их значение по-другому в том же предложении без скобок.
                    · Разбивай предложения, которые длиннее 20 слов.
                    · Причастные обороты оставляй только после того слова, к которому они относятся.
                    · Избегай цепочек из двух и более существительных в родительном падеже подряд.

                    Текст для обработки:
                    %s
                    """
        ),
        3, Map.of(
            "B1", """
                    Адаптируй текст для уровня B1.
                    Сохрани смысл, но сделай текст проще, короче и понятнее.
                    Используй короткие предложения, простые слова и только естественные конструкции.
                    Верни только готовый текст.

                    Текст:
                    %s
                    """,
            "B2", """
                    Адаптируй текст для уровня B2.
                    Сохрани смысл и структуру, но сделай лексику и синтаксис более доступными.
                    Избегай слишком книжных и редких слов.
                    Верни только готовый текст.

                    Текст:
                    %s
                    """
        )
    );

    public TranslatorService(
        @Value("${gemini.base-url}") String baseUrl,
        @Value("${gemini.api-key:}") String apiKey,
        @Value("${gemini.model}") String model
    ) {
        this.client = RestClient.create(baseUrl);
        this.apiKey = apiKey;
        this.model = model;
    }

    public String translate(String text, Integer promptType, String level) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new RuntimeException("Не задан GEMINI_API_KEY");
        }

        String promptTemplate = getPromptTemplate(promptType, level);
        String finalPrompt = String.format(promptTemplate, text);

        Map<String, Object> body = Map.of(
            "contents", List.of(
                Map.of(
                    "parts", List.of(
                        Map.of("text", finalPrompt)
                    )
                )
            )
        );

        try {
            Map<?, ?> response = client.post()
                                       .uri("/v1beta/models/{model}:generateContent", model)
                                       .contentType(MediaType.APPLICATION_JSON)
                                       .header("x-goog-api-key", apiKey)
                                       .body(body)
                                       .retrieve()
                                       .body(Map.class);

            if (response == null || response.get("candidates") == null) {
                throw new RuntimeException("Gemini вернул пустой ответ");
            }

            List<?> candidates = (List<?>) response.get("candidates");
            if (candidates.isEmpty()) {
                throw new RuntimeException("Gemini не вернул вариантов ответа");
            }

            Map<?, ?> firstCandidate = (Map<?, ?>) candidates.get(0);
            Map<?, ?> content = (Map<?, ?>) firstCandidate.get("content");
            if (content == null || content.get("parts") == null) {
                throw new RuntimeException("Gemini вернул ответ без content");
            }

            List<?> parts = (List<?>) content.get("parts");
            if (parts.isEmpty()) {
                throw new RuntimeException("Gemini вернул ответ без parts");
            }

            Map<?, ?> firstPart = (Map<?, ?>) parts.get(0);
            Object result = firstPart.get("text");
            if (result == null) {
                throw new RuntimeException("Gemini вернул ответ без text");
            }

            return result.toString().trim();

        } catch (HttpClientErrorException.Unauthorized e) {
            throw new RuntimeException("Неверный API ключ Gemini");
        } catch (HttpClientErrorException.TooManyRequests e) {
            throw new RuntimeException("Превышен лимит Gemini API");
        } catch (HttpClientErrorException e) {
            throw new RuntimeException("Ошибка Gemini API: " + e.getStatusCode().value());
        } catch (RestClientException e) {
            throw new RuntimeException("Не удалось связаться с Gemini API");
        }
    }

    private String getPromptTemplate(Integer promptType, String level) {
        int safePromptType = (promptType == null || !PROMPTS.containsKey(promptType)) ? 1 : promptType;
        String safeLevel = (level == null) ? "B1" : level.toUpperCase();

        Map<String, String> levelMap = PROMPTS.get(safePromptType);

        if (!levelMap.containsKey(safeLevel)) {
            safeLevel = "B1";
        }

        return levelMap.get(safeLevel);
    }
}