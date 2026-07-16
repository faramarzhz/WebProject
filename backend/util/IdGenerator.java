package util;
import java.io.*;

public class IdGenerator {
    private static int counter = 0;
    private static String counterFile = "data/counter.txt";
    private static boolean isLoaded = false;

    private static void loadCounter() {
        if (isLoaded)
            return;
        File file = new File(counterFile);
        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line = reader.readLine();
                if (line != null)
                    counter = Integer.parseInt(line);
            } catch (IOException e) {
            }
        }
        isLoaded = true;
    }
    private static void saveCounter() {
        File file = new File(counterFile);
        file.getParentFile().mkdirs();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(String.valueOf(counter));
        } catch (IOException e) {
        }
    }
    public static synchronized String generateId() {
        loadCounter();
        counter++;
        saveCounter();
        return String.valueOf(counter);
    }
}