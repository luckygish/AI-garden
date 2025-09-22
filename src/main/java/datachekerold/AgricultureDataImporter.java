//package com.agriculture.datachekerold;
//
//import org.json.JSONArray;
//import org.json.JSONObject;
//import java.sql.*;
//import java.nio.file.Files;
//import java.nio.file.Paths;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.Properties;
//import java.io.FileWriter;
//import java.io.IOException;
//import java.text.SimpleDateFormat;
//import java.util.Date;
//import java.util.ArrayList;
//import java.util.List;
//
//public class AgricultureDataImporter implements AutoCloseable {
//
//    private Connection connection;
//    private Map<String, Integer> cache = new HashMap<>();
//    private List<String> importLog = new ArrayList<>();
//    private String logFileName;
//
//    public AgricultureDataImporter() throws SQLException {
//        // Настройки подключения к БД
//        String dbUrl = "jdbc:postgresql://localhost:5432/agriculture";
//        String user = "postgres";
//        String password = "270707";
//
//        Properties connectionProps = new Properties();
//        connectionProps.put("user", user);
//        connectionProps.put("password", password);
//        connectionProps.put("ssl", "false");
//
//        this.connection = DriverManager.getConnection(dbUrl, connectionProps);
//        this.connection.setAutoCommit(false);
//
//        // Создаем имя файла лога с timestamp
//        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
//        this.logFileName = "import_log_" + dateFormat.format(new Date()) + ".txt";
//
//        log("✅ Успешное подключение к PostgreSQL");
//    }
//
//    /**
//     * Логирование сообщений
//     */
//    private void log(String message) {
//        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
//        String logMessage = "[" + timestamp + "] " + message;
//        importLog.add(logMessage);
//        System.out.println(logMessage);
//    }
//
//    /**
//     * Сохранение лога в файл
//     */
//    private void saveLogToFile() {
//        try (FileWriter writer = new FileWriter(logFileName)) {
//            for (String message : importLog) {
//                writer.write(message + "\n");
//            }
//            log("📄 Лог сохранен в файл: " + logFileName);
//        } catch (IOException e) {
//            System.err.println("❌ Ошибка сохранения лога: " + e.getMessage());
//        }
//    }
//
//    public void importJsonFile(String filePath) throws Exception {
//        log("📥 Начинаем импорт из файла: " + filePath);
//
//        String jsonContent = new String(Files.readAllBytes(Paths.get(filePath)));
//        JSONObject originalJson = new JSONObject(jsonContent);
//
//        // Сохраняем оригинальный JSON для сравнения
//        String originalJsonStr = originalJson.toString(2);
//
//        // Очищаем кэш для нового файла
//        cache.clear();
//
//        // Получаем или создаем основные сущности
//        int cultureId = getOrCreateId("cultures", "name", originalJson.getString("culture"));
//        int regionId = getOrCreateId("regions", "name", originalJson.getString("region"));
//        int areaId = getOrCreateId("areas", "name", originalJson.getString("area"));
//        int soilTypeId = getOrCreateId("soil_types", "name", originalJson.getString("soil_type"));
//
//        log("🔍 Найдены/созданы: культура=" + originalJson.getString("culture") +
//                ", регион=" + originalJson.getString("region"));
//
//        // Обрабатываем операции
//        JSONArray operations = originalJson.getJSONArray("operations");
//        log("📊 Найдено операций: " + operations.length());
//
//        for (int i = 0; i < operations.length(); i++) {
//            JSONObject operation = operations.getJSONObject(i);
//            log("🔄 Обработка операции " + (i+1) + ": " + operation.getString("type") +
//                    " (" + operation.getString("fase") + ")");
//            importOperation(operation, cultureId, regionId, areaId, soilTypeId);
//        }
//
//        connection.commit();
//        log("✅ Данные успешно импортированы из: " + filePath);
//
//        // Проверяем соответствие после импорта
//        verifyAfterImport(originalJson, originalJsonStr);
//    }
//
//    private void importOperation(JSONObject operation, int cultureId, int regionId,
//                                 int areaId, int soilTypeId) throws SQLException {
//
//        int operationTypeId = getOrCreateId("operation_types", "name", operation.getString("type"));
//        int faseId = getOrCreateId("fases", "name", operation.getString("fase"));
//
//        // Вставляем рекомендацию
//        String insertRecommendation = """
//            INSERT INTO recommendations
//            (culture_id, region_id, area_id, soil_type_id, operation_type_id, fase_id,
//             description, period, trigger_condition, application_condition)
//            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
//            RETURNING id
//            """;
//
//        int recommendationId;
//        try (PreparedStatement stmt = connection.prepareStatement(insertRecommendation)) {
//            stmt.setInt(1, cultureId);
//            stmt.setInt(2, regionId);
//            stmt.setInt(3, areaId);
//            stmt.setInt(4, soilTypeId);
//            stmt.setInt(5, operationTypeId);
//            stmt.setInt(6, faseId);
//            stmt.setString(7, operation.getString("description"));
//            stmt.setString(8, operation.getString("period"));
//            stmt.setString(9, operation.getString("trigger"));
//            stmt.setString(10, operation.getString("application_condition"));
//
//            ResultSet rs = stmt.executeQuery();
//            rs.next();
//            recommendationId = rs.getInt(1);
//        }
//
//        // Обрабатываем материалы
//        JSONArray materials = operation.getJSONArray("materials");
//        for (int j = 0; j < materials.length(); j++) {
//            JSONObject material = materials.getJSONObject(j);
//            importMaterial(material, recommendationId);
//        }
//    }
//
//    private void importMaterial(JSONObject material, int recommendationId) throws SQLException {
//        int materialId = getOrCreateId("materials", "name", material.getString("name"),
//                "type", material.getString("type"));
//
//        int methodId = getOrCreateId("application_methods", "name", material.getString("method"));
//
//        // Вставляем материал операции
//        String insertMaterial = """
//            INSERT INTO operation_materials
//            (recommendation_id, material_id, norm, method_id, frequency, warning)
//            VALUES (?, ?, ?, ?, ?, ?)
//            RETURNING id
//            """;
//
//        int operationMaterialId;
//        try (PreparedStatement stmt = connection.prepareStatement(insertMaterial)) {
//            stmt.setInt(1, recommendationId);
//            stmt.setInt(2, materialId);
//            stmt.setString(3, material.getString("norm"));
//            stmt.setInt(4, methodId);
//            stmt.setString(5, material.getString("frequency"));
//            stmt.setString(6, material.getString("warning"));
//
//            ResultSet rs = stmt.executeQuery();
//            rs.next();
//            operationMaterialId = rs.getInt(1);
//        }
//
//        // Обрабатываем альтернативы
//        if (material.has("alternatives")) {
//            JSONArray alternatives = material.getJSONArray("alternatives");
//            for (int k = 0; k < alternatives.length(); k++) {
//                JSONObject alternative = alternatives.getJSONObject(k);
//                importAlternative(alternative, operationMaterialId);
//            }
//        }
//    }
//
//    private void importAlternative(JSONObject alternative, int operationMaterialId) throws SQLException {
//        int alternativeMaterialId = getOrCreateId("materials", "name", alternative.getString("name"),
//                "type", alternative.getString("type"));
//
//        String insertAlternative = """
//            INSERT INTO material_alternatives
//            (operation_material_id, material_id, norm, warning, comment)
//            VALUES (?, ?, ?, ?, ?)
//            """;
//
//        try (PreparedStatement stmt = connection.prepareStatement(insertAlternative)) {
//            stmt.setInt(1, operationMaterialId);
//            stmt.setInt(2, alternativeMaterialId);
//            stmt.setString(3, alternative.getString("norm"));
//            stmt.setString(4, alternative.getString("warning"));
//            stmt.setString(5, alternative.optString("comment", ""));
//            stmt.executeUpdate();
//        }
//    }
//
//    private int getOrCreateId(String table, String column, String value) throws SQLException {
//        return getOrCreateId(table, column, value, null, null);
//    }
//
//    private int getOrCreateId(String table, String column1, String value1,
//                              String column2, String value2) throws SQLException {
//
//        String cacheKey = table + ":" + value1 + (value2 != null ? ":" + value2 : "");
//        if (cache.containsKey(cacheKey)) {
//            return cache.get(cacheKey);
//        }
//
//        // Пытаемся найти существующую запись
//        String findSql = "SELECT id FROM " + table + " WHERE " + column1 + " = ?";
//        if (column2 != null) {
//            findSql += " AND " + column2 + " = ?";
//        }
//
//        try (PreparedStatement stmt = connection.prepareStatement(findSql)) {
//            stmt.setString(1, value1);
//            if (column2 != null) {
//                stmt.setString(2, value2);
//            }
//
//            ResultSet rs = stmt.executeQuery();
//            if (rs.next()) {
//                int id = rs.getInt(1);
//                cache.put(cacheKey, id);
//                log("   ✅ Найден существующий: " + table + " = " + value1 +
//                        (value2 != null ? " (" + value2 + ")" : "") + " → ID=" + id);
//                return id;
//            }
//        }
//
//        // Создаем новую запись
//        String insertSql = "INSERT INTO " + table + " (" + column1;
//        String values = "?)";
//        if (column2 != null) {
//            insertSql += ", " + column2;
//            values = "?, ?)";
//        }
//        insertSql += ") VALUES (" + values + " RETURNING id";
//
//        try (PreparedStatement stmt = connection.prepareStatement(insertSql)) {
//            stmt.setString(1, value1);
//            if (column2 != null) {
//                stmt.setString(2, value2);
//            }
//
//            ResultSet rs = stmt.executeQuery();
//            rs.next();
//            int id = rs.getInt(1);
//            cache.put(cacheKey, id);
//            log("   ➕ Создан новый: " + table + " = " + value1 +
//                    (value2 != null ? " (" + value2 + ")" : "") + " → ID=" + id);
//            return id;
//        }
//    }
//
//    /**
//     * Проверка соответствия после импорта
//     */
//    private void verifyAfterImport(JSONObject originalJson, String originalJsonStr) {
//        try {
//            log("🔍 Начинаем проверку данных после импорта...");
//
//            JSONObject exportedJson = exportToJson(
//                    originalJson.getString("culture"),
//                    originalJson.getString("region"),
//                    originalJson.getString("area"),
//                    originalJson.getString("soil_type")
//            );
//
//            String exportedJsonStr = exportedJson.toString(2);
//
//            int originalLength = originalJsonStr.length();
//            int exportedLength = exportedJsonStr.length();
//
//            log("📊 Сравнение размеров:");
//            log("   Оригинальный JSON: " + originalLength + " символов");
//            log("   Экспортированный JSON: " + exportedLength + " символов");
//
//            // Сохраняем оба JSON в лог
//            log("\n=== ОРИГИНАЛЬНЫЙ JSON (из файла) ===");
//            logJsonWithLineNumbers(originalJsonStr, "ОРИГИНАЛЬНЫЙ");
//
//            log("\n=== ЭКСПОРТИРОВАННЫЙ JSON (из БД) ===");
//            logJsonWithLineNumbers(exportedJsonStr, "ЭКСПОРТИРОВАННЫЙ");
//
//            if (originalLength != exportedLength) {
//                log("❌ Обнаружены расхождения в размерах!");
//                logDetailedComparison(originalJson, exportedJson);
//            } else {
//                log("✅ Размеры JSON совпадают");
//                // Все равно делаем детальную проверку на случай скрытых расхождений
//                logDetailedComparison(originalJson, exportedJson);
//            }
//
//        } catch (Exception e) {
//            log("⚠️ Ошибка при проверке после импорта: " + e.getMessage());
//        }
//    }
//
//    /**
//     * Логирование JSON с нумерацией строк
//     */
//    private void logJsonWithLineNumbers(String jsonStr, String prefix) {
//        String[] lines = jsonStr.split("\n");
//        for (int i = 0; i < lines.length; i++) {
//            log(prefix + " [" + (i + 1) + "]: " + lines[i]);
//        }
//    }
//
//    /**
//     * Детальное сравнение JSON объектов
//     */
//    private void logDetailedComparison(JSONObject original, JSONObject exported) {
//        log("\n=== ДЕТАЛЬНОЕ СРАВНЕНИЕ ДАННЫХ ===");
//
//        List<String> differences = new ArrayList<>();
//        compareObjects("", original, exported, differences);
//
//        if (differences.isEmpty()) {
//            log("✅ Все данные полностью соответствуют!");
//        } else {
//            log("❌ Найдено расхождений: " + differences.size());
//            for (String diff : differences) {
//                log(diff);
//            }
//
//            // Сохраняем различия в отдельный файл
//            saveDifferencesToFile(differences, original, exported);
//        }
//
//        log("=== КОНЕЦ СРАВНЕНИЯ ===");
//    }
//
//    private void compareObjects(String path, Object expected, Object actual, List<String> differences) {
//        if (expected instanceof JSONObject && actual instanceof JSONObject) {
//            compareJsonObjects(path, (JSONObject) expected, (JSONObject) actual, differences);
//        } else if (expected instanceof JSONArray && actual instanceof JSONArray) {
//            compareJsonArrays(path, (JSONArray) expected, (JSONArray) actual, differences);
//        } else if (!String.valueOf(expected).equals(String.valueOf(actual))) {
//            differences.add("❌ РАСХОЖДЕНИЕ: " + path +
//                    " | Ожидалось: '" + expected +
//                    "' | Получено: '" + actual + "'");
//        }
//    }
//
//    private void compareJsonObjects(String path, JSONObject expected, JSONObject actual, List<String> differences) {
//        // Проверяем поля из expected
//        for (String key : expected.keySet()) {
//            String currentPath = path.isEmpty() ? key : path + "." + key;
//
//            if (actual.has(key)) {
//                compareObjects(currentPath, expected.get(key), actual.get(key), differences);
//            } else {
//                differences.add("⚠️ ОТСУТСТВУЕТ ПОЛЕ: " + currentPath +
//                        " | Значение: '" + expected.get(key) + "'");
//            }
//        }
//
//        // Проверяем дополнительные поля в actual
//        for (String key : actual.keySet()) {
//            if (!expected.has(key)) {
//                String currentPath = path.isEmpty() ? key : path + "." + key;
//                differences.add("➕ ДОПОЛНИТЕЛЬНОЕ ПОЛЕ: " + currentPath +
//                        " | Значение: '" + actual.get(key) + "'");
//            }
//        }
//    }
//
//    private void compareJsonArrays(String path, JSONArray expected, JSONArray actual, List<String> differences) {
//        if (expected.length() != actual.length()) {
//            differences.add("❌ РАЗЛИЧАЕТСЯ КОЛИЧЕСТВО ЭЛЕМЕНТОВ: " + path +
//                    " | Ожидалось: " + expected.length() +
//                    " | Получено: " + actual.length());
//            return;
//        }
//
//        for (int i = 0; i < expected.length(); i++) {
//            compareObjects(path + "[" + i + "]", expected.get(i), actual.get(i), differences);
//        }
//    }
//
//    /**
//     * Сохранение различий в отдельный файл
//     */
//    private void saveDifferencesToFile(List<String> differences, JSONObject original, JSONObject exported) {
//        try {
//            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
//            String diffFileName = "differences_report_" + dateFormat.format(new Date()) + ".txt";
//
//            FileWriter writer = new FileWriter(diffFileName);
//
//            writer.write("ОТЧЕТ О РАСХОЖДЕНИЯХ МЕЖДУ JSON И БД\n");
//            writer.write("=======================================\n\n");
//
//            writer.write("ВРЕМЯ ПРОВЕРКИ: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "\n\n");
//
//            writer.write("КОЛИЧЕСТВО РАСХОЖДЕНИЙ: " + differences.size() + "\n\n");
//
//            writer.write("ОРИГИНАЛЬНЫЙ JSON (из файла):\n");
//            writer.write("=============================\n");
//            writer.write(original.toString(2));
//            writer.write("\n\n");
//
//            writer.write("ЭКСПОРТИРОВАННЫЙ JSON (из БД):\n");
//            writer.write("==============================\n");
//            writer.write(exported.toString(2));
//            writer.write("\n\n");
//
//            writer.write("ДЕТАЛЬНЫЙ СПИСОК РАСХОЖДЕНИЙ:\n");
//            writer.write("=============================\n");
//
//            for (String diff : differences) {
//                writer.write(diff + "\n");
//            }
//
//            writer.close();
//
//            log("📄 Детальный отчет о расхождениях сохранен в: " + diffFileName);
//
//        } catch (Exception e) {
//            log("❌ Ошибка при сохранении отчета о расхождениях: " + e.getMessage());
//        }
//    }
//
//    /**
//     * Экспорт данных из БД
//     */
//    public JSONObject exportToJson(String culture, String region, String area, String soilType) throws SQLException {
//        String sql = """
//            WITH rec_data AS (
//                SELECT
//                    c.name as culture, r.name as region, a.name as area, s.name as soil_type,
//                    ot.name as operation_type, f.name as fase,
//                    rec.description, rec.period, rec.trigger_condition as trigger,
//                    rec.application_condition,
//                    m.name as material_name, m.type as material_type, om.norm,
//                    am.name as method, om.frequency, om.warning,
//                    alt_m.name as alt_name, alt_m.type as alt_type, alt.norm as alt_norm,
//                    alt.warning as alt_warning, alt.comment as alt_comment,
//                    rec.id as rec_id, om.id as mat_id
//                FROM recommendations rec
//                JOIN cultures c ON rec.culture_id = c.id
//                JOIN regions r ON rec.region_id = r.id
//                JOIN areas a ON rec.area_id = a.id
//                JOIN soil_types s ON rec.soil_type_id = s.id
//                JOIN operation_types ot ON rec.operation_type_id = ot.id
//                JOIN fases f ON rec.fase_id = f.id
//                JOIN operation_materials om ON rec.id = om.recommendation_id
//                JOIN materials m ON om.material_id = m.id
//                JOIN application_methods am ON om.method_id = am.id
//                LEFT JOIN material_alternatives alt ON om.id = alt.operation_material_id
//                LEFT JOIN materials alt_m ON alt.material_id = alt_m.id
//                WHERE c.name = ? AND r.name = ? AND a.name = ? AND s.name = ?
//                ORDER BY rec.id, om.id
//            )
//            SELECT
//                culture, region, area, soil_type,
//                operation_type, fase, description, period, trigger,
//                application_condition,
//                material_name, material_type, norm, method, frequency, warning,
//                alt_name, alt_type, alt_norm, alt_warning, alt_comment,
//                rec_id, mat_id
//            FROM rec_data
//            """;
//
//        JSONObject result = new JSONObject();
//        JSONArray operations = new JSONArray();
//        Map<Integer, JSONObject> operationMap = new HashMap<>();
//        Map<String, JSONArray> materialMap = new HashMap<>();
//
//        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
//            stmt.setString(1, culture);
//            stmt.setString(2, region);
//            stmt.setString(3, area);
//            stmt.setString(4, soilType);
//
//            ResultSet rs = stmt.executeQuery();
//            while (rs.next()) {
//                int recId = rs.getInt("rec_id");
//                int matId = rs.getInt("mat_id");
//                String key = recId + "_" + matId;
//
//                JSONObject operation = operationMap.get(recId);
//                if (operation == null) {
//                    operation = new JSONObject();
//                    operation.put("type", rs.getString("operation_type"));
//                    operation.put("fase", rs.getString("fase"));
//                    operation.put("description", rs.getString("description"));
//                    operation.put("period", rs.getString("period"));
//                    operation.put("trigger", rs.getString("trigger"));
//                    operation.put("application_condition", rs.getString("application_condition"));
//                    operation.put("materials", new JSONArray());
//                    operationMap.put(recId, operation);
//                    operations.put(operation);
//                }
//
//                JSONArray materials = materialMap.get(key);
//                if (materials == null) {
//                    JSONObject material = new JSONObject();
//                    material.put("name", rs.getString("material_name"));
//                    material.put("type", rs.getString("material_type"));
//                    material.put("norm", rs.getString("norm"));
//                    material.put("method", rs.getString("method"));
//                    material.put("frequency", rs.getString("frequency"));
//                    material.put("warning", rs.getString("warning"));
//                    material.put("alternatives", new JSONArray());
//
//                    JSONArray operationMaterials = operation.getJSONArray("materials");
//                    operationMaterials.put(material);
//                    materialMap.put(key, material.getJSONArray("alternatives"));
//                }
//
//                String altName = rs.getString("alt_name");
//                if (altName != null) {
//                    JSONObject alternative = new JSONObject();
//                    alternative.put("name", altName);
//                    alternative.put("type", rs.getString("alt_type"));
//                    alternative.put("norm", rs.getString("alt_norm"));
//                    alternative.put("warning", rs.getString("alt_warning"));
//                    alternative.put("comment", rs.getString("alt_comment"));
//
//                    JSONArray alternatives = materialMap.get(key);
//                    alternatives.put(alternative);
//                }
//            }
//        }
//
//        result.put("culture", culture);
//        result.put("region", region);
//        result.put("area", area);
//        result.put("soil_type", soilType);
//        result.put("operations", operations);
//
//        return result;
//    }
//
//    @Override
//    public void close() throws SQLException {
//        if (connection != null && !connection.isClosed()) {
//            // Сохраняем лог перед закрытием
//            saveLogToFile();
//            connection.close();
//            log("🔌 Подключение к БД закрыто");
//        }
//    }
//
//    public static void main(String[] args) {
//        String jsonFilePath = "C:\\Users\\dmitr\\IdeaProjects\\agriculture-importer\\src\\main\\resources\\tomat_MO.json";
//
//        try (AgricultureDataImporter importer = new AgricultureDataImporter()) {
//            // Импорт данных
//            importer.importJsonFile(jsonFilePath);
//
//        } catch (Exception e) {
//            System.err.println("💥 Ошибка: " + e.getMessage());
//            e.printStackTrace();
//        }
//    }
//}