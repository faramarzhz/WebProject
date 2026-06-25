package server;

import cli.AdminCLI;
import network.Server;

public class Main {
    public static void main(String[] args) {
        Server server = new Server(8080); // تعریف سرور روی پورت پیش‌فرض ۸۰۸۰
        Thread serverThread = new Thread(() -> server.start());
        serverThread.setDaemon(true); // ست کردن به عنوان دیمون برای بسته شدن خودکار با اتمام برنامه اصلی
        serverThread.start();
        System.out.println("WebChat Backend Server successfully started on port 8080");
        try {
            Thread.sleep(300); // وقفه کوتاه برای اطمینان از بالا آمدن کامل سرور سوکت
        } catch (InterruptedException ignored) {
        }

        AdminCLI cli = new AdminCLI(server); // راه‌اندازی و اجرای لایه خط فرمان مدیریت سرور
        cli.start();
        try {
            Thread.currentThread().join(); // نگه داشتن ترد اصلی برای زنده ماندن بک‌اند
        } catch (InterruptedException ignored) {
        }
    }
}