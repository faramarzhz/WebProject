package util;

import java.io.*;

public class IdGenerator {
    private static int counter = 0;
    private static String counterFile = "data/counter.txt";
    private static boolean isLoaded = false;

    // خواندن آخرین شمارنده ذخیره‌شده از فایل تا با ری‌ استارت سرور آیدی‌ ها تکراری
    // نشوند
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
                System.err.println("Could not read counter file");
            }
        }
        isLoaded = true;
    }

    // ذخیره شمارنده در فایل تا در اجرای بعدی سرور از همین‌جا ادامه پیدا کند
    private static void saveCounter() {
        File file = new File(counterFile);
        file.getParentFile().mkdirs();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(String.valueOf(counter));
        } catch (IOException e) {
            System.err.println("Could not save counter file");
        }
    }

    public static synchronized String generateId() {
        loadCounter();
        counter++;
        saveCounter();
        return String.valueOf(counter);
    }
}