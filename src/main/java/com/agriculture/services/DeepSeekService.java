package com.agriculture.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import jakarta.annotation.PostConstruct;

import java.util.HashMap;
import java.util.Map;

@Service
public class DeepSeekService {

    @Value("${deepseek.api.url:https://api.deepseek.com/v1/chat/completions}")
    private String apiUrl;

    @Value("${deepseek.api.key:}")
    private String apiKey;

    @Value("${deepseek.api.model:deepseek-chat}")
    private String model;

    @Value("${deepseek.api.timeout:30000}")
    private int timeout;

    @Value("${deepseek.api.max-retries:3}")
    private int maxRetries;

    @Value("${deepseek.api.retry-delay:1000}")
    private int retryDelay;

    private RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public DeepSeekService() {
        this.objectMapper = new ObjectMapper();
    }

    @PostConstruct
    public void init() {
        // Настраиваем RestTemplate с таймаутами после инициализации полей
        org.springframework.http.client.SimpleClientHttpRequestFactory factory = 
            new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeout);
        factory.setReadTimeout(timeout);
        
        this.restTemplate = new RestTemplate(factory);
    }

    /**
     * Запрашивает план ухода у DeepSeek API с retry логикой
     */
    public JsonNode requestCarePlan(String culture, String region, String gardenType) {
        System.out.println("🌱 Запрос плана ухода для: " + culture + " в регионе " + region + " (" + gardenType + ")");
        
        String prompt = buildPrompt(culture, region, gardenType);
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            long startTime = System.currentTimeMillis();
            try {
                System.out.println("🔄 Попытка " + attempt + "/" + maxRetries + " запроса к DeepSeek API...");
                
                String response = sendRequest(prompt);
                
                long duration = System.currentTimeMillis() - startTime;
                System.out.println("✅ Получен ответ от DeepSeek API за " + duration + "мс");
                
                return parseResponse(response);
            } catch (Exception e) {
                long duration = System.currentTimeMillis() - startTime;
                System.err.println("❌ Попытка " + attempt + " неудачна за " + duration + "мс: " + e.getMessage());
                
                if (attempt == maxRetries) {
                    throw new RuntimeException("Ошибка при запросе к DeepSeek API после " + maxRetries + " попыток: " + e.getMessage(), e);
                }
                
                // Логируем попытку и ждем перед повтором
                System.out.println("⏳ Ожидание " + retryDelay + "мс перед следующей попыткой...");
                try {
                    Thread.sleep(retryDelay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Прервано ожидание между попытками", ie);
                }
            }
        }
        
        throw new RuntimeException("Неожиданная ошибка в retry логике");
    }

    /**
     * Строит промт для DeepSeek API на основе параметров
     */
    private String buildPrompt(String culture, String region, String gardenType) {
        return String.format("""
            Ты — агроном-эксперт. Твоя задача — строго следовать инструкции и предоставить данные в формате JSON.
            
            ИНСТРУКЦИЯ:
            1. Проанализируй предоставленный источник информации
            2. Извлеки все данные по удобрениям и препаратам для указанной культуры
            3. Верни ответ ТОЛЬКО в формате JSON строго по указанной схеме
            4. Не добавляй никакого пояснительного текста
            
            ПАРАМЕТРЫ ЗАПРОСА:
            - Культура: %s
            - Регион: %s
            - Тип посадки: %s
            - Источник: Источники должны быть приоритетно на русском языке и актуальны для территории Российской Федерации. Только книги, официальные документы, научные статьи, методички, регламенты.
            
            СХЕМА JSON:
            {
              "culture": "название культуры",
              "region": "регион",
              "garden_type": "тип посадки",
              "operations": [
                {
                  "type": "тип операции (подкормка, обработка, полив)",
                  "fase": "фаза роста растения",
                  "description": "описание операции и цели",
                  "period": "рекомендуемый месяц/период времени",
                  "trigger": "условие применения (температура, фаза роста, погодные условия)",
                  "application_condition": "условие для внесения (наличие вредителей, болезней, симптомов дефицита)",
                  "materials": [
                    {
                      "name": "точное название удобрения/препарата",
                      "type": "тип (минеральное, органическое, фунгицид, инсектицид)",
                      "norm": "норма внесения с единицами измерения",
                      "method": "метод внесения (внекорневая, полив под корень, опрыскивание)",
                      "frequency": "кратность обработок и интервалы",
                      "warning": "предупреждение (накопление в почве, токсичность, ограничения)",
                      "alternatives": [
                        {
                          "name": "альтернативное удобрение/препарат",
                          "type": "тип альтернативы",
                          "norm": "норма внесения",
                          "warning": "предупреждение для альтернативного материала",
                          "comment": "преимущества/недостатки альтернативы"
                        }
                      ]
                    }
                  ],
                  "source": "источник (название, автор, страница)"
                }
              ]
            }
            
            ТРЕБОВАНИЯ:
            - Для каждого типа операции предоставляй не менее двух различных материалов в массиве "materials"
            - Для каждого основного материала указывай не менее двух альтернативных вариантов в массиве "alternatives"
            - Для каждого альтернативного материала добавляй поле "warning" с предупреждениями
            - Для каждой операции указывай рекомендуемый временной период (месяц, декада)
            - Предоставляй альтернативные материалы (органические вместо минеральных и т.д.)
            - При выборе инсектицидов и пестицидов, предлагай менее токсичные
            - Учитывай указанный регион и тип почвы при рекомендациях норм внесения
            - Указывай условия, когда обработка не требуется или противопоказана
            - В поле "application_condition" указывай конкретные условия для внесения (наличие колорадского жука, тли, фитофтороза, хлороза листьев, дефицита элементов и т.д.)
            - Источники должны быть приоритетно на русском языке и актуальны для территории Российской Федерации
            - В ответе не должно быть общих сайтов (Wikipedia) или форумов без конкретных авторитетных авторов
            - Используй только книги, официальные документы, научные статьи, методички, регламенты
            - Указывай точные названия источников с авторами и страницами
            - Добавляй предупреждения о возможных проблемах и ограничениях для каждого материала
            
            Начинай анализ.
            """, culture, region, gardenType);
    }

    /**
     * Отправляет запрос к DeepSeek API
     */
    private String sendRequest(String prompt) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new RuntimeException("DeepSeek API ключ не настроен");
        }

        System.out.println("🔑 API ключ настроен, отправляем запрос к: " + apiUrl);
        System.out.println("📝 Длина промта: " + prompt.length() + " символов");

        try {
            // Подготавливаем заголовки
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            // Подготавливаем тело запроса
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", new Object[]{
                Map.of("role", "user", "content", prompt)
            });
            requestBody.put("temperature", 0.1);
            requestBody.put("max_tokens", 4000);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            // Отправляем запрос
            System.out.println("🚀 Отправляем HTTP POST запрос...");
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                apiUrl,
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            System.out.println("📡 Получен HTTP ответ: " + response.getStatusCode());

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                
                // Извлекаем содержимое ответа
                if (responseBody.containsKey("choices") && 
                    responseBody.get("choices") instanceof java.util.List) {
                    
                    java.util.List<?> choices = (java.util.List<?>) responseBody.get("choices");
                    if (!choices.isEmpty() && choices.get(0) instanceof Map) {
                        Map<String, Object> choice = (Map<String, Object>) choices.get(0);
                        if (choice.containsKey("message") && choice.get("message") instanceof Map) {
                            Map<String, Object> message = (Map<String, Object>) choice.get("message");
                            if (message.containsKey("content")) {
                                return message.get("content").toString();
                            }
                        }
                    }
                }
                
                throw new RuntimeException("Неожиданный формат ответа от DeepSeek API");
            } else {
                throw new RuntimeException("Ошибка API: " + response.getStatusCode());
            }

        } catch (HttpClientErrorException e) {
            throw new RuntimeException("Ошибка HTTP запроса к DeepSeek API: " + e.getMessage(), e);
        } catch (ResourceAccessException e) {
            throw new RuntimeException("Ошибка подключения к DeepSeek API: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Неожиданная ошибка при запросе к DeepSeek API: " + e.getMessage(), e);
        }
    }

    /**
     * Парсит ответ от DeepSeek API в JsonNode
     */
    private JsonNode parseResponse(String response) {
        try {
            // Очищаем ответ от возможных markdown блоков
            String cleanedResponse = response.trim();
            if (cleanedResponse.startsWith("```json")) {
                cleanedResponse = cleanedResponse.substring(7);
            }
            if (cleanedResponse.endsWith("```")) {
                cleanedResponse = cleanedResponse.substring(0, cleanedResponse.length() - 3);
            }
            cleanedResponse = cleanedResponse.trim();

            return objectMapper.readTree(cleanedResponse);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка парсинга JSON ответа от DeepSeek: " + e.getMessage(), e);
        }
    }

    /**
     * Проверяет доступность API
     */
    public boolean isApiAvailable() {
        return apiKey != null && !apiKey.trim().isEmpty();
    }
}
