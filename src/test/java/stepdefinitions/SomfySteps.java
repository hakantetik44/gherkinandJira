package stepdefinitions;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.And;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.junit.Assert;
import pages.SomfyHomePage;
import utils.DriverManager;

public class SomfySteps {
    private SomfyHomePage homePage;

    public SomfySteps() {
        homePage = new SomfyHomePage(DriverManager.getDriver());
    }

    @Given("je suis sur la page d'accueil de Somfy")
    public void jeSuisSurLaPageDAccueilDeSomfy() {
        DriverManager.getDriver().get("https://boutique.somfy.fr/");
    }

    @When("je clique sur le bouton {string}")
    public void jeCliqueSurLeBouton(String buttonText) {
        homePage.acceptCookies();
    }

    @Then("le bandeau des cookies disparaît")
    public void leBandeauDesCookiesDisparait() {
        try {
            Thread.sleep(2000); // Attendre que le bandeau disparaisse
            WebElement cookieBanner = DriverManager.getDriver().findElement(By.id("popin_tc_privacy_button_2"));
            Assert.assertFalse("Le bandeau des cookies est toujours visible", cookieBanner.isDisplayed());
        } catch (Exception e) {
            // Si on ne trouve pas le bandeau, c'est qu'il a bien disparu
            Assert.assertTrue("Le bandeau des cookies a bien disparu", true);
        }
    }

    @When("je clique sur le menu {string}")
    public void jeCliqueSurLeMenu(String menu) {
        if (menu.equals("Produits")) {
            homePage.clickProduitsMenu();
        }
    }

    @And("je sélectionne {string}")
    public void jeSelectionne(String sousMenu) {
        homePage.clickSubMenu(sousMenu);
    }

    @And("je choisis {string}")
    public void jeChoisis(String produit) {
        homePage.selectProduct(produit);
    }

    @And("je vérifie le prix {string}")
    public void jeVerifieLePrix(String prix) {
        String actualPrice = homePage.getProductPrice();
        Assert.assertEquals("Le prix ne correspond pas", prix, actualPrice);
    }

    @And("je clique sur {string}")
    public void jeCliqueSur(String bouton) {
        homePage.clickButton(bouton);
    }

    @Then("je vois le message de confirmation d'ajout au panier")
    public void jeVoisLeMessageDeConfirmation() {
        Assert.assertTrue("Message de confirmation non affiché", 
            homePage.isConfirmationMessageDisplayed());
    }

    @And("le montant du panier est {string}")
    public void leMontantDuPanierEst(String montant) {
        String actualAmount = homePage.getCartAmount();
        Assert.assertEquals("Le montant du panier ne correspond pas", 
            montant, actualAmount);
    }
} 