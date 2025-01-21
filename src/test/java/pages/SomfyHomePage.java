package pages;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;

public class SomfyHomePage extends BasePage {

    @FindBy(id = "popin_tc_privacy_button_2")
    private WebElement toutAccepterButton;

    @FindBy(css = "button.burger-menu")
    private WebElement menuButton;

    @FindBy(xpath = "//span[text()='Alarme et sécurité']")
    private WebElement alarmSecurityMenu;

    @FindBy(xpath = "//a[contains(text(),'Sécurité maison')]")
    private WebElement securityHomeLink;

    @FindBy(css = "span.price-wrapper")
    private WebElement productPrice;

    @FindBy(css = "button#product-addtocart-button")
    private WebElement addToCartButton;

    @FindBy(css = "div.message-success")
    private WebElement cartConfirmation;

    @FindBy(css = "span.counter-number")
    private WebElement cartAmount;

    @FindBy(xpath = "//a[contains(text(),'Produits')]")
    private WebElement produitsMenu;

    public SomfyHomePage(WebDriver driver) {
        super(driver);
    }

    public void acceptCookies() {
        try {
            wait.until(ExpectedConditions.elementToBeClickable(toutAccepterButton));
            click(toutAccepterButton);
            Thread.sleep(1000); // Attendre que le bandeau disparaisse
        } catch (Exception e) {
            System.out.println("Cookie banner not found or already accepted: " + e.getMessage());
        }
    }

    public void clickMainMenu(String menu) {
        click(menuButton);
        click(alarmSecurityMenu);
    }

    public void clickSubMenu(String subMenuName) {
        click(securityHomeLink);
    }

    public void selectProduct(String productName) {
        WebElement product = driver.findElement(
            By.xpath(String.format("//a[contains(text(),'%s')]", productName))
        );
        click(product);
    }

    public String getProductPrice() {
        return productPrice.getText().trim();
    }

    public void clickButton(String buttonName) {
        if (buttonName.equals("Ajouter au panier")) {
            click(addToCartButton);
        }
    }

    public boolean isConfirmationMessageDisplayed() {
        try {
            return cartConfirmation.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public String getCartAmount() {
        return cartAmount.getText().trim();
    }

    public void clickProduitsMenu() {
        try {
            wait.until(ExpectedConditions.elementToBeClickable(produitsMenu));
            click(produitsMenu);
        } catch (Exception e) {
            System.out.println("Error clicking Produits menu: " + e.getMessage());
        }
    }
}