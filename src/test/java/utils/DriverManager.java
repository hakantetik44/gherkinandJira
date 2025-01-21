package utils;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

public class DriverManager {
    private static ThreadLocal<WebDriver> driver = new ThreadLocal<>();
    private static ChromeOptions options;

    public static WebDriver getDriver() {
        if (driver.get() == null) {
            initDriver();
        }
        return driver.get();
    }

    public static void setOptions(ChromeOptions chromeOptions) {
        options = chromeOptions;
    }

    private static void initDriver() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions localOptions = options != null ? options : new ChromeOptions();
        localOptions.addArguments("--start-maximized");
        localOptions.addArguments("--disable-notifications");
        driver.set(new ChromeDriver(localOptions));
    }

    public static void quitDriver() {
        if (driver.get() != null) {
            driver.get().quit();
            driver.remove();
        }
    }
} 