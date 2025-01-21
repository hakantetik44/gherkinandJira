package pages;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class LoginPage extends BasePage {
    
    @FindBy(id = "email")
    private WebElement emailInput;

    @FindBy(id = "password")
    private WebElement passwordInput;

    @FindBy(id = "login-button")
    private WebElement loginButton;

    public LoginPage(WebDriver driver) {
        super(driver);
    }

    @Step("Enter email: {0}")
    public void enterEmail(String email) {
        sendKeys(emailInput, email);
    }

    @Step("Enter password: {0}")
    public void enterPassword(String password) {
        sendKeys(passwordInput, password);
    }

    @Step("Click login button")
    public void clickLoginButton() {
        click(loginButton);
    }
} 