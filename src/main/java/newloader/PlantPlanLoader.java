package newloader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import java.security.MessageDigest;
import java.sql.*;
import java.util.UUID;
import java.io.File;

//./gradlew run --args="/path/to/your/file.json"
//./gradlew run --args="/path/to/your/directory"

public class PlantPlanLoader {
//    private static final String JSON_PATH = "/absolute/path/to/your/json/files/";
    // ИЛИ для одного файла:
     private static final String JSON_PATH = "C:\\Users\\dmitr\\IdeaProjects\\agriculture-importer\\src\\main\\resources\\Berry.json";
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final Connection connection;

    public PlantPlanLoader(Connection connection) {
        this.connection = connection;
    }

    public String generateInputHash(PlantParameters params) {
        try {
            String normalizedString = String.format("culture:%s|region:%s|garden_type:%s",
                    params.getCulture().toLowerCase().trim(),
                    params.getRegion().toLowerCase().trim(),
                    params.getGardenType().toLowerCase().trim());

            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hashBytes = digest.digest(normalizedString.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error generating hash", e);
        }
    }

    public PlantParameters extractParametersFromJson(String jsonContent) throws Exception {
        JsonNode rootNode = objectMapper.readTree(jsonContent);
        PlantParameters params = new PlantParameters();

        params.setCulture(rootNode.path("culture").asText());
        params.setRegion(rootNode.path("region").asText());
        params.setGardenType(rootNode.path("garden_type").asText());

        return params;
    }

    public boolean planExists(String inputHash) throws SQLException {
        String sql = "SELECT COUNT(*) FROM care_plans WHERE input_hash = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, inputHash);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        }
        return false;
    }

    public UUID insertPlan(String inputHash, PlantParameters params, String jsonContent) throws Exception {
        String sql = "INSERT INTO care_plans (input_hash, input_parameters, ai_generated_plan) " +
                "VALUES (?, ?::jsonb, ?::jsonb) RETURNING id";

        // Создаем JSON для input_parameters
        ObjectNode inputParamsJson = objectMapper.createObjectNode();
        inputParamsJson.put("culture", params.getCulture());
        inputParamsJson.put("region", params.getRegion());
        inputParamsJson.put("garden_type", params.getGardenType());

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, inputHash);
            stmt.setString(2, inputParamsJson.toString());
            stmt.setString(3, jsonContent);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return (UUID) rs.getObject("id");
            }
            throw new SQLException("Insert failed, no ID returned");
        }
    }

    public JsonNode getPlanByHash(String inputHash) throws Exception {
        String sql = "SELECT ai_generated_plan FROM care_plans WHERE input_hash = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, inputHash);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String json = rs.getString("ai_generated_plan");
                return objectMapper.readTree(json);
            }
        }
        return null;
    }

    public VerificationResult verifyPlan(String inputHash, String originalJson) throws Exception {
        JsonNode storedPlan = getPlanByHash(inputHash);
        JsonNode originalPlan = objectMapper.readTree(originalJson);

        if (storedPlan == null) {
            return new VerificationResult(false, "Plan not found in database");
        }

        boolean isEqual = storedPlan.equals(originalPlan);
        return new VerificationResult(isEqual, isEqual ? "Verification successful" : "Data mismatch");
    }

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

    public UUID processJsonFile(String jsonContent) throws Exception {
        // Извлекаем параметры из JSON
        PlantParameters params = extractParametersFromJson(jsonContent);

        // Генерируем хэш
        String inputHash = generateInputHash(params);

        // Проверяем существование
        if (planExists(inputHash)) {
            System.out.println("Plan already exists for hash: " + inputHash);
            return null;
        }

        // Вставляем в БД
        UUID planId = insertPlan(inputHash, params, jsonContent);
        System.out.println("Inserted new plan with ID: " + planId + ", Hash: " + inputHash);

        // Проверяем целостность данных
        VerificationResult result = verifyPlan(inputHash, jsonContent);
        System.out.println("Verification: " + (result.isSuccess() ? "SUCCESS" : "FAILED"));
        System.out.println("Message: " + result.getMessage());

        if (!result.isSuccess()) {
            throw new RuntimeException("Data verification failed: " + result.getMessage());
        }

        return planId;
    }

    private static void processSingleFile(PlantPlanLoader loader, File jsonFile) {
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

    private static void processDirectory(PlantPlanLoader loader, File directory) {
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

    public static void main(String[] args) {
        File inputPath = new File(JSON_PATH);

        try {
            // Подключение к БД
            Connection connection = DriverManager.getConnection(
                    "jdbc:postgresql://localhost:5432/postgres",
                    "postgres",
                    "270707"
            );

            PlantPlanLoader loader = new PlantPlanLoader(connection);

            if (inputPath.isFile() && JSON_PATH.toLowerCase().endsWith(".json")) {
                // Обработка одного файла
                processSingleFile(loader, inputPath);
            } else if (inputPath.isDirectory()) {
                // Обработка всей директории
                processDirectory(loader, inputPath);
            } else {
                System.out.println("Invalid path in JSON_PATH constant: " + JSON_PATH);
            }

            connection.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

//    public static void main(String[] args) {
//        if (args.length < 1) {
//            System.out.println("Usage: java PlantPlanLoader <path_to_json_file_or_directory>");
//            System.out.println("Example: java PlantPlanLoader /path/to/data.json");
//            System.out.println("Example: java PlantPlanLoader /path/to/json/files/");
//            System.exit(1);
//        }
//
//        String path = args[0];
//        File inputPath = new File(path);
//
//        try {
//            // Подключение к БД
//            Connection connection = DriverManager.getConnection(
//                    "jdbc:postgresql://localhost:5432/your_database",
//                    "your_username",
//                    "your_password"
//            );
//
//            PlantPlanLoader loader = new PlantPlanLoader(connection);
//
//            if (inputPath.isFile() && path.toLowerCase().endsWith(".json")) {
//                // Обработка одного файла
//                processSingleFile(loader, inputPath);
//            } else if (inputPath.isDirectory()) {
//                // Обработка всей директории
//                processDirectory(loader, inputPath);
//            } else {
//                System.out.println("Invalid input path. Please provide a JSON file or directory.");
//            }
//
//            connection.close();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//}