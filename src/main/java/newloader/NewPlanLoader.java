package newloader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.*;
import java.util.UUID;
import java.io.File;

/**
 * Новый загрузчик планов ухода без использования input_hash
 * Использует уникальное ограничение по input_parameters для предотвращения дубликатов
 */
public class NewPlanLoader {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final Connection connection;

    public NewPlanLoader(Connection connection) {
        this.connection = connection;
    }

    /**
     * Извлекает параметры из JSON файла
     */
    public PlantParameters extractParametersFromJson(String jsonContent) throws Exception {
        JsonNode rootNode = objectMapper.readTree(jsonContent);
        PlantParameters params = new PlantParameters();

        params.setCulture(rootNode.path("culture").asText());
        params.setRegion(rootNode.path("region").asText());
        params.setGardenType(rootNode.path("garden_type").asText());

        return params;
    }

    /**
     * Проверяет существование плана по параметрам
     */
    public boolean planExists(PlantParameters params) throws SQLException {
        String sql = "SELECT COUNT(*) FROM care_plans WHERE " +
                "input_parameters->>'culture' = ? AND " +
                "input_parameters->>'region' = ? AND " +
                "input_parameters->>'garden_type' = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, params.getCulture());
            stmt.setString(2, params.getRegion());
            stmt.setString(3, params.getGardenType());
            
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        }
        return false;
    }

    /**
     * Вставляет новый план в базу данных
     */
    public UUID insertPlan(PlantParameters params, String jsonContent) throws Exception {
        String sql = "INSERT INTO care_plans (input_parameters, ai_generated_plan) " +
                "VALUES (?::jsonb, ?::jsonb) RETURNING id";

        // Создаем JSON для input_parameters
        ObjectNode inputParamsJson = objectMapper.createObjectNode();
        inputParamsJson.put("culture", params.getCulture());
        inputParamsJson.put("region", params.getRegion());
        inputParamsJson.put("garden_type", params.getGardenType());

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, inputParamsJson.toString());
            stmt.setString(2, jsonContent);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return (UUID) rs.getObject("id");
            }
            throw new SQLException("Insert failed, no ID returned");
        }
    }

    /**
     * Получает план по параметрам
     */
    public JsonNode getPlanByParameters(PlantParameters params) throws Exception {
        String sql = "SELECT ai_generated_plan FROM care_plans WHERE " +
                "input_parameters->>'culture' = ? AND " +
                "input_parameters->>'region' = ? AND " +
                "input_parameters->>'garden_type' = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, params.getCulture());
            stmt.setString(2, params.getRegion());
            stmt.setString(3, params.getGardenType());
            
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String json = rs.getString("ai_generated_plan");
                return objectMapper.readTree(json);
            }
        }
        return null;
    }

    /**
     * Проверяет целостность данных
     */
    public VerificationResult verifyPlan(PlantParameters params, String originalJson) throws Exception {
        JsonNode storedPlan = getPlanByParameters(params);
        JsonNode originalPlan = objectMapper.readTree(originalJson);

        if (storedPlan == null) {
            return new VerificationResult(false, "Plan not found in database");
        }

        boolean isEqual = storedPlan.equals(originalPlan);
        return new VerificationResult(isEqual, isEqual ? "Verification successful" : "Data mismatch");
    }

    /**
     * Обрабатывает JSON файл
     */
    public UUID processJsonFile(String jsonContent) throws Exception {
        // Извлекаем параметры из JSON
        PlantParameters params = extractParametersFromJson(jsonContent);

        // Проверяем существование
        if (planExists(params)) {
            System.out.println("Plan already exists for: " + params.toString());
            return null;
        }

        // Вставляем в БД
        UUID planId = insertPlan(params, jsonContent);
        System.out.println("Inserted new plan with ID: " + planId);
        System.out.println("Parameters: " + params.toString());

        // Проверяем целостность данных
        VerificationResult result = verifyPlan(params, jsonContent);
        System.out.println("Verification: " + (result.isSuccess() ? "SUCCESS" : "FAILED"));
        System.out.println("Message: " + result.getMessage());

        if (!result.isSuccess()) {
            throw new RuntimeException("Data verification failed: " + result.getMessage());
        }

        return planId;
    }

    /**
     * Обрабатывает один файл
     */
    private static void processSingleFile(NewPlanLoader loader, File jsonFile) {
        try {
            System.out.println("Processing file: " + jsonFile.getName());
            String jsonContent = Files.readString(jsonFile.toPath(), StandardCharsets.UTF_8);
            UUID planId = loader.processJsonFile(jsonContent);

            if (planId != null) {
                System.out.println("✓ Successfully processed: " + jsonFile.getName());
            } else {
                System.out.println("ⓘ Skipped (already exists): " + jsonFile.getName());
            }
        } catch (Exception e) {
            System.err.println("✗ Error processing " + jsonFile.getName() + ": " + e.getMessage());
        }
    }

    /**
     * Обрабатывает директорию
     */
    private static void processDirectory(NewPlanLoader loader, File directory) {
        try {
            File[] jsonFiles = directory.listFiles((dir, name) ->
                    name.toLowerCase().endsWith(".json"));

            if (jsonFiles == null || jsonFiles.length == 0) {
                System.out.println("No JSON files found in directory: " + directory.getAbsolutePath());
                return;
            }

            System.out.println("Found " + jsonFiles.length + " JSON files in directory");

            for (File jsonFile : jsonFiles) {
                processSingleFile(loader, jsonFile);
            }

            System.out.println("Directory processing completed: " + directory.getAbsolutePath());
        } catch (Exception e) {
            System.err.println("Error processing directory: " + e.getMessage());
        }
    }

    /**
     * Результат проверки
     */
    public static class VerificationResult {
        private final boolean success;
        private final String message;

        public VerificationResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
    }

    public static void main(String[] args) {
        String directoryPath = "C:\\Users\\dmitr\\IdeaProjects\\agriculture-importer\\src\\main\\resources";

        try {
            Connection connection = DriverManager.getConnection(
                    "jdbc:postgresql://localhost:5432/postgres",
                    "postgres",
                    "270707"
            );

            NewPlanLoader loader = new NewPlanLoader(connection);

            File inputPath = new File(directoryPath);
            if (inputPath.isDirectory()) {
                processDirectory(loader, inputPath);
            } else {
                System.out.println("Invalid directory path: " + directoryPath);
            }

            connection.close();
            System.out.println("Batch processing completed");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
