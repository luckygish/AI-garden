package com.agriculture.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.agriculture.dto.PlantVarietyDescriptionRequest;
import com.agriculture.dto.PlantVarietyDescriptionResponse;
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
import java.util.List;
import java.util.ArrayList;

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

    @Value("${deepseek.api.retry-delay:3000}")
    private int retryDelay;

    @Value("${deepseek.api.max-tokens:8192}")
    private int maxTokens;

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
     * Запрашивает план ухода у DeepSeek API с улучшенной retry логикой
     */
    public JsonNode requestCarePlan(String culture, String region, String gardenType) {
        System.out.println("🌱 Запрос плана ухода для: " + culture + " в регионе " + region + " (" + gardenType + ")");
        System.out.println("⚙️ Настройки: таймаут=" + timeout + "мс, попыток=" + maxRetries + ", задержка=" + retryDelay + "мс");
        
        String prompt = buildPrompt(culture, region, gardenType);
        
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            long startTime = System.currentTimeMillis();
            try {
                System.out.println("🔄 Попытка " + attempt + "/" + maxRetries + " запроса к DeepSeek API...");
                
                String response = sendRequest(prompt);
                
                long duration = System.currentTimeMillis() - startTime;
                System.out.println("✅ Получен ответ от DeepSeek API за " + duration + "мс");
                
                JsonNode result = parseResponse(response);
                System.out.println("🎉 Успешно получен план ухода!");
                return result;
                
            } catch (Exception e) {
                long duration = System.currentTimeMillis() - startTime;
                lastException = e;
                
                System.err.println("❌ Попытка " + attempt + " неудачна за " + duration + "мс");
                System.err.println("💥 Ошибка: " + e.getMessage());
                
                // Если это последняя попытка, выбрасываем исключение
                if (attempt == maxRetries) {
                    System.err.println("🚫 Исчерпаны все попытки (" + maxRetries + ")");
                    break;
                }
                
                // Увеличиваем задержку с каждой попыткой (exponential backoff)
                int currentDelay = retryDelay * attempt;
                System.out.println("⏳ Ожидание " + currentDelay + "мс перед следующей попыткой...");
                
                try {
                    Thread.sleep(currentDelay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Прервано ожидание между попытками", ie);
                }
            }
        }
        
        // Если дошли до сюда, значит все попытки неудачны
        throw new RuntimeException("Ошибка при запросе к DeepSeek API после " + maxRetries + " попыток. " +
            "Последняя ошибка: " + (lastException != null ? lastException.getMessage() : "Неизвестная ошибка"), lastException);
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
                  ]
                }
              ]
            }
            
            ТРЕБОВАНИЯ:
            - Для каждого типа операции предоставляй не менее двух различных материалов в массиве "materials"
            - Для каждого основного материала указывай не менее одного альтернативного варианта в массиве "alternatives"
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
            - Добавляй предупреждения о возможных проблемах и ограничениях для каждого материала
            
            Начинай анализ.
            """, culture, region, gardenType);
    }

    /**
     * Отправляет запрос к DeepSeek API с улучшенной обработкой ошибок
     */
    private String sendRequest(String prompt) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new RuntimeException("DeepSeek API ключ не настроен");
        }

        System.out.println("🔑 API ключ настроен, отправляем запрос к: " + apiUrl);
        System.out.println("📝 Длина промта: " + prompt.length() + " символов");
        System.out.println("⏱️ Таймаут: " + timeout + "мс, Максимум токенов: " + maxTokens);

        try {
            // Подготавливаем заголовки
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);
            headers.set("User-Agent", "Agriculture-App/1.0");

            // Подготавливаем тело запроса
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", new Object[]{
                Map.of("role", "user", "content", prompt)
            });
            requestBody.put("temperature", 0.1);
            requestBody.put("max_tokens", maxTokens);
            requestBody.put("stream", false); // Отключаем стриминг для стабильности

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            // Отправляем запрос
            System.out.println("🚀 Отправляем HTTP POST запрос...");
            long startTime = System.currentTimeMillis();
            
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                apiUrl,
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            long duration = System.currentTimeMillis() - startTime;
            System.out.println("📡 Получен HTTP ответ: " + response.getStatusCode() + " за " + duration + "мс");

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                
                // Логируем структуру ответа для отладки
                System.out.println("📋 Ключи в ответе: " + responseBody.keySet());
                
                // Извлекаем содержимое ответа
                if (responseBody.containsKey("choices") && 
                    responseBody.get("choices") instanceof java.util.List) {
                    
                    java.util.List<?> choices = (java.util.List<?>) responseBody.get("choices");
                    if (!choices.isEmpty() && choices.get(0) instanceof Map) {
                        Map<String, Object> choice = (Map<String, Object>) choices.get(0);
                        if (choice.containsKey("message") && choice.get("message") instanceof Map) {
                            Map<String, Object> message = (Map<String, Object>) choice.get("message");
                            if (message.containsKey("content")) {
                                String content = message.get("content").toString();
                                System.out.println("✅ Получен контент длиной: " + content.length() + " символов");
                                return content;
                            }
                        }
                    }
                }
                
                // Если не удалось извлечь контент, логируем весь ответ
                System.out.println("❌ Неожиданная структура ответа: " + objectMapper.writeValueAsString(responseBody));
                throw new RuntimeException("Неожиданный формат ответа от DeepSeek API");
            } else {
                System.out.println("❌ Ошибка HTTP: " + response.getStatusCode());
                throw new RuntimeException("Ошибка API: " + response.getStatusCode());
            }

        } catch (HttpClientErrorException e) {
            System.err.println("❌ HTTP ошибка: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
            throw new RuntimeException("Ошибка HTTP запроса к DeepSeek API: " + e.getMessage() + 
                " (Статус: " + e.getStatusCode() + ")", e);
        } catch (ResourceAccessException e) {
            System.err.println("❌ Ошибка подключения: " + e.getMessage());
            throw new RuntimeException("Ошибка подключения к DeepSeek API: " + e.getMessage(), e);
        } catch (Exception e) {
            System.err.println("❌ Неожиданная ошибка: " + e.getMessage());
            throw new RuntimeException("Неожиданная ошибка при запросе к DeepSeek API: " + e.getMessage(), e);
        }
    }

    /**
     * Парсит ответ от DeepSeek API в JsonNode с улучшенной обработкой
     */
    private JsonNode parseResponse(String response) {
        try {
            System.out.println("🔍 Начинаем парсинг ответа...");
            System.out.println("📄 Первые 200 символов ответа: " + response.substring(0, Math.min(200, response.length())));
            
            // Очищаем ответ от возможных markdown блоков
            String cleanedResponse = response.trim();
            
            // Удаляем markdown блоки
            if (cleanedResponse.startsWith("```json")) {
                cleanedResponse = cleanedResponse.substring(7);
                System.out.println("🧹 Удален открывающий ```json");
            }
            if (cleanedResponse.startsWith("```")) {
                cleanedResponse = cleanedResponse.substring(3);
                System.out.println("🧹 Удален открывающий ```");
            }
            if (cleanedResponse.endsWith("```")) {
                cleanedResponse = cleanedResponse.substring(0, cleanedResponse.length() - 3);
                System.out.println("🧹 Удален закрывающий ```");
            }
            
            cleanedResponse = cleanedResponse.trim();
            
            // Проверяем что у нас есть валидный JSON
            if (cleanedResponse.isEmpty()) {
                throw new RuntimeException("Пустой ответ от DeepSeek API");
            }
            
            if (!cleanedResponse.startsWith("{") && !cleanedResponse.startsWith("[")) {
                System.out.println("⚠️ Ответ не начинается с { или [, возможно это не JSON");
                // Попробуем найти JSON в тексте
                int jsonStart = cleanedResponse.indexOf("{");
                if (jsonStart != -1) {
                    cleanedResponse = cleanedResponse.substring(jsonStart);
                    System.out.println("🔍 Найден JSON начиная с позиции: " + jsonStart);
                }
            }
            
            System.out.println("📝 Очищенный ответ длиной: " + cleanedResponse.length() + " символов");
            
            JsonNode result = objectMapper.readTree(cleanedResponse);
            System.out.println("✅ JSON успешно распарсен");
            return result;
            
        } catch (Exception e) {
            System.err.println("❌ Ошибка парсинга JSON: " + e.getMessage());
            System.err.println("📄 Проблемный ответ: " + response);
            throw new RuntimeException("Ошибка парсинга JSON ответа от DeepSeek: " + e.getMessage() + 
                "\nОтвет: " + response.substring(0, Math.min(500, response.length())), e);
        }
    }

    /**
     * Проверяет доступность API
     */
    public boolean isApiAvailable() {
        return apiKey != null && !apiKey.trim().isEmpty();
    }

    /**
     * Генерирует описание сорта растения через DeepSeek API
     */
    public PlantVarietyDescriptionResponse generateVarietyDescription(PlantVarietyDescriptionRequest request) {
        String prompt = buildVarietyDescriptionPrompt(request);
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", List.of(
            Map.of("role", "user", "content", prompt)
        ));
        requestBody.put("max_tokens", maxTokens);
        requestBody.put("temperature", 0.7);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            System.out.println("🌱 Генерация описания сорта: " + request.getCulture() + " - " + request.getVariety());
            
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                apiUrl,
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                
                if (responseBody.containsKey("choices")) {
                    List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
                    if (!choices.isEmpty()) {
                        Map<String, Object> firstChoice = choices.get(0);
                        Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
                        String content = (String) message.get("content");
                        
                        System.out.println("✅ Получен ответ от DeepSeek для описания сорта");
                        return parseVarietyDescriptionResponse(content, request);
                    }
                }
            }
            
            throw new RuntimeException("Неожиданный формат ответа от DeepSeek API");
            
        } catch (HttpClientErrorException e) {
            System.err.println("❌ HTTP ошибка при генерации описания сорта: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
            throw new RuntimeException("Ошибка API при генерации описания сорта: " + e.getStatusCode() + " - " + e.getMessage());
        } catch (ResourceAccessException e) {
            System.err.println("❌ Ошибка подключения при генерации описания сорта: " + e.getMessage());
            throw new RuntimeException("Ошибка подключения к DeepSeek API при генерации описания сорта: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("❌ Неожиданная ошибка при генерации описания сорта: " + e.getMessage());
            throw new RuntimeException("Ошибка при генерации описания сорта: " + e.getMessage());
        }
    }

    /**
     * Создает промпт для генерации описания сорта
     */
    private String buildVarietyDescriptionPrompt(PlantVarietyDescriptionRequest request) {
        return String.format("""
            Ты — агроном-эксперт. Твоя задача — строго следовать инструкции и предоставить данные в формате JSON.
            
            ИНСТРУКЦИЯ:
            1. Проанализируй предоставленный источник информации
            2. Извлеки все данные по указанной культуре и сорту
            3. Верни ответ ТОЛЬКО в формате JSON строго по указанной схеме
            4. Не добавляй никакого пояснительного текста
            
            ПАРАМЕТРЫ ЗАПРОСА: в виде JSON 
            {
            "culture": "%s",
            "variety": "%s", 
            "description": "краткое описание сорта",
            "ripening_period": "срок созревания",
            "plant_height": "высота растения",
            "fruit_weight": "масса плода",
            "yield": "урожайность",
            "disease_resistance": ["устойчивость к болезням"],
            "growing_conditions": "условия выращивания"
            }
            - Общее описание растения (кратко, не более 250 символов)
            - Источник: Источники должны быть приоритетно на русском языке и актуальны для территории Российской Федерации. Только книги, официальные документы, научные статьи, методички, регламенты.
            
            СХЕМА JSON:
            {
              "culture": "название культуры",
              "variety": "название сорта",
              "description": "краткое описание сорта",
              "ripening_period": "срок созревания",
              "plant_height": "высота растения",
              "fruit_weight": "масса плода",
              "yield": "урожайность",
              "disease_resistance": ["устойчивость к болезням"],
              "growing_conditions": "условия выращивания"
            }
            
            ТРЕБОВАНИЯ:
            - Описание должно быть кратким (не более 250 символов)
            - Указывай только проверенные данные из авторитетных источников
            - Источники должны быть приоритетно на русском языке
            - Используй только книги, официальные документы, научные статьи, методички, регламенты
            - В ответе не должно быть общих сайтов (Wikipedia) или форумов
            
            Начинай анализ.
            """, request.getCulture(), request.getVariety());
    }

    /**
     * Парсит ответ от DeepSeek для описания сорта
     */
    private PlantVarietyDescriptionResponse parseVarietyDescriptionResponse(String response, PlantVarietyDescriptionRequest request) {
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

            JsonNode jsonNode = objectMapper.readTree(cleanedResponse);
            
            PlantVarietyDescriptionResponse description = new PlantVarietyDescriptionResponse();
            description.setCulture(jsonNode.path("culture").asText(request.getCulture()));
            description.setVariety(jsonNode.path("variety").asText(request.getVariety()));
            description.setDescription(jsonNode.path("description").asText("Описание сорта недоступно"));
            description.setRipeningPeriod(jsonNode.path("ripening_period").asText("Не указан"));
            description.setPlantHeight(jsonNode.path("plant_height").asText("Не указана"));
            description.setFruitWeight(jsonNode.path("fruit_weight").asText("Не указана"));
            description.setYield(jsonNode.path("yield").asText("Не указана"));
            description.setGrowingConditions(jsonNode.path("growing_conditions").asText("Не указаны"));
            
            // Парсим массив disease_resistance
            if (jsonNode.has("disease_resistance") && jsonNode.get("disease_resistance").isArray()) {
                List<String> diseases = new ArrayList<>();
                for (JsonNode disease : jsonNode.get("disease_resistance")) {
                    diseases.add(disease.asText());
                }
                description.setDiseaseResistance(diseases);
            } else {
                description.setDiseaseResistance(List.of());
            }
            
            return description;
            
        } catch (Exception e) {
            System.err.println("❌ Ошибка парсинга JSON описания сорта: " + e.getMessage());
            System.err.println("📄 Проблемный ответ: " + response);
            
            // Возвращаем базовое описание в случае ошибки
            PlantVarietyDescriptionResponse fallback = new PlantVarietyDescriptionResponse();
            fallback.setCulture(request.getCulture());
            fallback.setVariety(request.getVariety());
            fallback.setDescription("Описание сорта " + request.getVariety() + " для " + request.getCulture() + " будет доступно позже.");
            fallback.setRipeningPeriod("Не указан");
            fallback.setPlantHeight("Не указана");
            fallback.setFruitWeight("Не указана");
            fallback.setYield("Не указана");
            fallback.setGrowingConditions("Не указаны");
            fallback.setDiseaseResistance(List.of());
            
            return fallback;
        }
    }
}
