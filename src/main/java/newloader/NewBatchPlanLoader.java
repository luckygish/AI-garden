package newloader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Новый класс для пакетной обработки JSON файлов без использования input_hash
 */
public class NewBatchPlanLoader {

    public static void main(String[] args) {
        String directoryPath = "C:\\Users\\dmitr\\IdeaProjects\\agriculture-importer\\src\\main\\resources";

        try {
            Connection connection = DriverManager.getConnection(
                    "jdbc:postgresql://localhost:5432/postgres",
                    "postgres",
                    "270707"
            );

            NewPlanLoader loader = new NewPlanLoader(connection);

            // Получаем все JSON файлы из директории
            List<Path> jsonFiles = Files.list(Paths.get(directoryPath))
                    .filter(path -> path.toString().endsWith(".json"))
                    .collect(Collectors.toList());

            System.out.println("Found " + jsonFiles.size() + " JSON files");

            int successCount = 0;
            int skippedCount = 0;
            int errorCount = 0;

            for (Path filePath : jsonFiles) {
                try {
                    System.out.println("\nProcessing: " + filePath.getFileName());
                    String jsonContent = Files.readString(filePath);
                    UUID planId = loader.processJsonFile(jsonContent);

                    if (planId != null) {
                        System.out.println("✓ Success: " + filePath.getFileName());
                        successCount++;
                    } else {
                        System.out.println("ⓘ Skipped (already exists): " + filePath.getFileName());
                        skippedCount++;
                    }

                } catch (Exception e) {
                    System.err.println("✗ Error processing " + filePath.getFileName() + ": " + e.getMessage());
                    errorCount++;
                }
            }

            connection.close();
            
            System.out.println("\n" + "=".repeat(50));
            System.out.println("BATCH PROCESSING SUMMARY:");
            System.out.println("=".repeat(50));
            System.out.println("Total files processed: " + jsonFiles.size());
            System.out.println("Successfully inserted: " + successCount);
            System.out.println("Skipped (already exist): " + skippedCount);
            System.out.println("Errors: " + errorCount);
            System.out.println("=".repeat(50));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
