//package com.agriculture.datachekerold;
//
//import org.json.JSONArray;
//import org.json.JSONObject;
//import java.sql.*;
//import java.nio.file.Files;
//import java.nio.file.Paths;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Properties;
//
//public class AgricultureDataChecker implements AutoCloseable {
//
//    private Connection connection;
//
//    public AgricultureDataChecker() throws SQLException {
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
//        System.out.println("✅ Успешное подключение к PostgreSQL для проверки");
//    }
//
//    /**
//     * Проверяет соответствие данных в БД исходному JSON файлу с детальным выводом расхождений
//     */
//    public VerificationResult verifyDataWithDetails(String filePath) throws Exception {
//        System.out.println("🔍 Начинаем детальную проверку данных...");
//        System.out.println("📁 Файл для проверки: " + filePath);
//
//        String jsonContent = new String(Files.readAllBytes(Paths.get(filePath)));
//        JSONObject originalJson = new JSONObject(jsonContent);
//
//        JSONObject exportedJson = exportToJson(
//                originalJson.getString("culture"),
//                originalJson.getString("region"),
//                originalJson.getString("area"),
//                originalJson.getString("soil_type")
//        );
//
//        VerificationResult result = compareJsonObjects(originalJson, exportedJson);
//
//        System.out.println("📊 Результат проверки:");
//        System.out.println("✅ Совпадений: " + result.matchingFields.size());
//        System.out.println("❌ Расхождений: " + result.differences.size());
//        System.out.println("⚠️  Отсутствующих полей: " + result.missingFields.size());
//
//        if (result.differences.isEmpty() && result.missingFields.isEmpty()) {
//            System.out.println("🎉 Все данные полностью соответствуют!");
//        } else {
//            System.out.println("\n=== ДЕТАЛЬНЫЙ ОТЧЕТ О РАСХОЖДЕНИЯХ ===");
//
//            if (!result.differences.isEmpty()) {
//                System.out.println("\n❌ РАСХОЖДЕНИЯ В ЗНАЧЕНИЯХ:");
//                for (FieldDifference diff : result.differences) {
//                    System.out.println("   Поле: " + diff.fieldPath);
//                    System.out.println("   Ожидалось: " + diff.expectedValue);
//                    System.out.println("   Получено: " + diff.actualValue);
//                    System.out.println("   ---");
//                }
//            }
//
//            if (!result.missingFields.isEmpty()) {
//                System.out.println("\n⚠️  ОТСУТСТВУЮЩИЕ ПОЛЯ:");
//                for (String missingField : result.missingFields) {
//                    System.out.println("   " + missingField);
//                }
//            }
//
//            if (!result.extraFields.isEmpty()) {
//                System.out.println("\n➕ ДОПОЛНИТЕЛЬНЫЕ ПОЛЯ В БД:");
//                for (String extraField : result.extraFields) {
//                    System.out.println("   " + extraField);
//                }
//            }
//        }
//
//        return result;
//    }
//
//    /**
//     * Сравнивает два JSON объекта и возвращает детальные результаты
//     */
//    private VerificationResult compareJsonObjects(JSONObject expected, JSONObject actual) {
//        VerificationResult result = new VerificationResult();
//        compareJson("", expected, actual, result);
//        return result;
//    }
//
//    private void compareJson(String path, Object expected, Object actual, VerificationResult result) {
//        if (expected instanceof JSONObject && actual instanceof JSONObject) {
//            compareJsonObjects(path, (JSONObject) expected, (JSONObject) actual, result);
//        } else if (expected instanceof JSONArray && actual instanceof JSONArray) {
//            compareJsonArrays(path, (JSONArray) expected, (JSONArray) actual, result);
//        } else {
//            String expectedStr = String.valueOf(expected);
//            String actualStr = String.valueOf(actual);
//
//            if (!expectedStr.equals(actualStr)) {
//                result.differences.add(new FieldDifference(
//                        path.isEmpty() ? "root" : path,
//                        expectedStr,
//                        actualStr
//                ));
//            } else {
//                result.matchingFields.add(path.isEmpty() ? "root" : path);
//            }
//        }
//    }
//
//    private void compareJsonObjects(String path, JSONObject expected, JSONObject actual, VerificationResult result) {
//
//
//        // Проверяем поля из expected
//        for (String key : expected.keySet()) {
//            String currentPath = path.isEmpty() ? key : path + "." + key;
//            if ("source".equals(key)) {
//                continue;
//            }
//            if (actual.has(key)) {
//                compareJson(currentPath, expected.get(key), actual.get(key), result);
//            } else {
//                result.missingFields.add(currentPath);
//            }
//        }
//
//        // Проверяем дополнительные поля в actual
//        for (String key : actual.keySet()) {
//            if (!expected.has(key)) {
//                String currentPath = path.isEmpty() ? key : path + "." + key;
//                result.extraFields.add(currentPath + " = " + actual.get(key));
//            }
//        }
//    }
//
//    private void compareJsonArrays(String path, JSONArray expected, JSONArray actual, VerificationResult result) {
//        if (expected.length() != actual.length()) {
//            result.differences.add(new FieldDifference(
//                    path + ".length",
//                    String.valueOf(expected.length()),
//                    String.valueOf(actual.length())
//            ));
//            return;
//        }
//
//        for (int i = 0; i < expected.length(); i++) {
//            compareJson(path + "[" + i + "]", expected.get(i), actual.get(i), result);
//        }
//    }
//
//    /**
//     * Экспортирует данные из БД в JSON格式
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
//        java.util.Map<Integer, JSONObject> operationMap = new java.util.HashMap<>();
//        java.util.Map<String, JSONArray> materialMap = new java.util.HashMap<>();
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
//    /**
//     * Класс для хранения результатов проверки
//     */
//    public static class VerificationResult {
//        public List<String> matchingFields = new ArrayList<>();
//        public List<FieldDifference> differences = new ArrayList<>();
//        public List<String> missingFields = new ArrayList<>();
//        public List<String> extraFields = new ArrayList<>();
//
//        public boolean isPerfectMatch() {
//            return differences.isEmpty() && missingFields.isEmpty() && extraFields.isEmpty();
//        }
//    }
//
//    /**
//     * Класс для хранения информации о различиях
//     */
//    public static class FieldDifference {
//        public String fieldPath;
//        public String expectedValue;
//        public String actualValue;
//
//        public FieldDifference(String fieldPath, String expectedValue, String actualValue) {
//            this.fieldPath = fieldPath;
//            this.expectedValue = expectedValue;
//            this.actualValue = actualValue;
//        }
//
//        @Override
//        public String toString() {
//            return fieldPath + ": ожидалось '" + expectedValue + "', получено '" + actualValue + "'";
//        }
//    }
//
//    @Override
//    public void close() throws SQLException {
//        if (connection != null && !connection.isClosed()) {
//            connection.close();
//            System.out.println("🔌 Подключение к БД закрыто");
//        }
//    }
//
//    /**
//     * Отдельный main метод для запуска проверки
//     */
//    public static void main(String[] args) {
//        String jsonFilePath = "C:\\Users\\dmitr\\IdeaProjects\\agriculture-importer\\src\\main\\resources\\tomat_MO.json";
//
//        System.out.println("🔍 Запуск детальной проверки данных...");
//        System.out.println("📁 Файл: " + jsonFilePath);
//
//        try (AgricultureDataChecker checker = new AgricultureDataChecker()) {
//            // Детальная проверка данных
//            VerificationResult result = checker.verifyDataWithDetails(jsonFilePath);
//
//            if (result.isPerfectMatch()) {
//                System.out.println("🎉 Все данные полностью соответствуют!");
//            } else {
//                System.out.println("⚠️  Обнаружены расхождения между БД и JSON!");
//
//                // Сохранение отчета в файл
//                saveVerificationReport(result, "verification_report.txt");
//                System.out.println("📄 Отчет сохранен в файл: verification_report.txt");
//            }
//
//        } catch (Exception e) {
//            System.err.println("💥 Ошибка при проверке: " + e.getMessage());
//            e.printStackTrace();
//        }
//    }
//
//    /**
//     * Сохраняет отчет о проверке в файл
//     */
//    private static void saveVerificationReport(VerificationResult result, String filename) {
//        try {
//            java.io.FileWriter writer = new java.io.FileWriter(filename);
//            writer.write("ОТЧЕТ О ПРОВЕРКЕ ДАННЫХ\n");
//            writer.write("=======================\n\n");
//
//            writer.write("Результат: " + (result.isPerfectMatch() ? "ПОЛНОЕ СООТВЕТСТВИЕ" : "НАЙДЕНЫ РАСХОЖДЕНИЯ") + "\n\n");
//
//            writer.write("Совпадения: " + result.matchingFields.size() + "\n");
//            writer.write("Расхождения: " + result.differences.size() + "\n");
//            writer.write("Отсутствующие поля: " + result.missingFields.size() + "\n");
//            writer.write("Дополнительные поля: " + result.extraFields.size() + "\n\n");
//
//            if (!result.differences.isEmpty()) {
//                writer.write("РАСХОЖДЕНИЯ В ЗНАЧЕНИЯХ:\n");
//                writer.write("-----------------------\n");
//                for (FieldDifference diff : result.differences) {
//                    writer.write("Поле: " + diff.fieldPath + "\n");
//                    writer.write("Ожидалось: " + diff.expectedValue + "\n");
//                    writer.write("Получено: " + diff.actualValue + "\n");
//                    writer.write("---\n");
//                }
//                writer.write("\n");
//            }
//
//            if (!result.missingFields.isEmpty()) {
//                writer.write("ОТСУТСТВУЮЩИЕ ПОЛЯ:\n");
//                writer.write("-------------------\n");
//                for (String field : result.missingFields) {
//                    writer.write(field + "\n");
//                }
//                writer.write("\n");
//            }
//
//            if (!result.extraFields.isEmpty()) {
//                writer.write("ДОПОЛНИТЕЛЬНЫЕ ПОЛЯ В БД:\n");
//                writer.write("-------------------------\n");
//                for (String field : result.extraFields) {
//                    writer.write(field + "\n");
//                }
//            }
//
//            writer.close();
//
//        } catch (Exception e) {
//            System.err.println("Ошибка при сохранении отчета: " + e.getMessage());
//        }
//    }
//}