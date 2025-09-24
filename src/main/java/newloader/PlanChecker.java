package newloader;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * Утилита для проверки существующих планов в базе данных
 */
public class PlanChecker {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void main(String[] args) {
        try {
            Connection connection = DriverManager.getConnection(
                    "jdbc:postgresql://localhost:5432/postgres",
                    "postgres",
                    "270707"
            );

            System.out.println("Checking existing care plans in database...\n");

            // Получаем все планы
            List<PlanInfo> plans = getAllPlans(connection);
            
            System.out.println("Found " + plans.size() + " care plans in database:\n");
            
            for (PlanInfo plan : plans) {
                System.out.println("ID: " + plan.getId());
                System.out.println("Culture: " + plan.getCulture());
                System.out.println("Region: " + plan.getRegion());
                System.out.println("Garden Type: " + plan.getGardenType());
                System.out.println("Created: " + plan.getCreatedAt());
                System.out.println("-".repeat(40));
            }

            connection.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static List<PlanInfo> getAllPlans(Connection connection) throws Exception {
        String sql = "SELECT id, input_parameters, created_at FROM care_plans ORDER BY created_at DESC";
        List<PlanInfo> plans = new ArrayList<>();

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                PlanInfo plan = new PlanInfo();
                plan.setId(rs.getString("id"));
                plan.setCreatedAt(rs.getTimestamp("created_at"));
                
                // Парсим JSON параметры
                String inputParams = rs.getString("input_parameters");
                if (inputParams != null) {
                    var jsonNode = objectMapper.readTree(inputParams);
                    plan.setCulture(jsonNode.path("culture").asText());
                    plan.setRegion(jsonNode.path("region").asText());
                    plan.setGardenType(jsonNode.path("garden_type").asText());
                }
                
                plans.add(plan);
            }
        }
        
        return plans;
    }

    public static class PlanInfo {
        private String id;
        private String culture;
        private String region;
        private String gardenType;
        private java.sql.Timestamp createdAt;

        // Геттеры и сеттеры
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getCulture() { return culture; }
        public void setCulture(String culture) { this.culture = culture; }

        public String getRegion() { return region; }
        public void setRegion(String region) { this.region = region; }

        public String getGardenType() { return gardenType; }
        public void setGardenType(String gardenType) { this.gardenType = gardenType; }

        public java.sql.Timestamp getCreatedAt() { return createdAt; }
        public void setCreatedAt(java.sql.Timestamp createdAt) { this.createdAt = createdAt; }
    }
}
