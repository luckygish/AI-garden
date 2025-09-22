package newloader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

//класс для пакетной обработки
public class BatchPlanLoader {

    public static void main(String[] args) {
        String directoryPath = "C:\\Users\\dmitr\\IdeaProjects\\agriculture-importer\\src\\main\\resources";

        try {
            Connection connection = DriverManager.getConnection(
                    "jdbc:postgresql://localhost:5432/postgres",
                    "postgres",
                    "270707"
            );

            PlantPlanLoader loader = new PlantPlanLoader(connection);

            // Получаем все JSON файлы из директории
            List<Path> jsonFiles = Files.list(Paths.get(directoryPath))
                    .filter(path -> path.toString().endsWith(".json"))
                    .collect(Collectors.toList());

            System.out.println("Found " + jsonFiles.size() + " JSON files");

            for (Path filePath : jsonFiles) {
                try {
                    System.out.println("Processing: " + filePath.getFileName());
                    String jsonContent = Files.readString(filePath);
                    UUID planId = loader.processJsonFile(jsonContent);

                    if (planId != null) {
                        System.out.println("✓ Success: " + filePath.getFileName());
                    } else {
                        System.out.println("ⓘ Skipped (already exists): " + filePath.getFileName());
                    }

                } catch (Exception e) {
                    System.err.println("✗ Error processing " + filePath.getFileName() + ": " + e.getMessage());
                }
            }

            connection.close();
            System.out.println("Batch processing completed");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}