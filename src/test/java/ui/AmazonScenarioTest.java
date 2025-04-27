package ui;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.openqa.selenium.Keys;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class AmazonScenarioTest {

    private WebDriver driver;
    private WebDriverWait wait;

    private final String userEmail      = "Edit the userEmail String ";
    private final String userPassword   = "Edit the userPassword String";
    private final double maxPriceLimit  = 15_000.0;            // EGP

    private int           expectedAddedCount = 0;
    private final List<Double> itemPrices    = new ArrayList<>();

    @BeforeClass
    public void setup() {
        driver = new ChromeDriver();
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(4));
        wait   = new WebDriverWait(driver, Duration.ofSeconds(10));

        driver.get("https://www.amazon.eg/-/en");
        System.out.println("Browser launched and navigated to Amazon.eg");
    }

    @Test
    public void testVideoGameScenario() throws InterruptedException {

        openVideoGamesPage();
        applyFiltersAndSort();
        collectAndAddItems();
        verifyCartCount();

        proceedToCheckout();
        login();
        handlePossible500();

        addDeliveryAddress();
        selectCashOnDelivery();

        validateOrderTotal();
    }



    private void openVideoGamesPage() {
        System.out.println("Opening menu");
        WebElement hamburger = wait.until(
                ExpectedConditions.elementToBeClickable(By.id("nav-hamburger-menu")));
        hamburger.click();

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("hmenu-content")));

        // See all
        driver.findElements(By.xpath("//a[contains(@class,'hmenu-item') and contains(.,'See all')]"))
                .stream().findFirst()
                .ifPresent(WebElement::click);

        // Video Games category
        wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector("a.hmenu-item[data-menu-id='16']"))).click();

        // “All Video Games” page
        WebElement allGames = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.cssSelector("a.hmenu-item[href*='node=18022560031']")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", allGames);
    }

    //filters

    private void applyFiltersAndSort() {
        System.out.println("Applying filters");

        // Free shipping
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("p_n_free_shipping_eligible-title")));
        driver.findElement(By.xpath("//div[@id='p_n_free_shipping_eligible-title']/following-sibling::ul[1]//a"))
                .click();

        // Condition = New
        wait.until(ExpectedConditions.presenceOfElementLocated(
                        By.xpath("//a[contains(@href,'p_n_condition-type') and ." +
                                "//span[contains(text(),'New') or contains(text(),'جديد')]]")))
                .click();

        // Sort: high-to-low
        wait.until(ExpectedConditions.elementToBeClickable(By.id("a-autoid-0-announce"))).click();
        wait.until(ExpectedConditions.elementToBeClickable(By.id("s-result-sort-select_2"))).click();
    }

    // add-to-cart

    private void collectAndAddItems() throws InterruptedException {

        boolean hasNextPage = true;

        while (hasNextPage) {
            Thread.sleep(1000); // wait page
            System.out.println("Scanning current results…");

            List<WebElement> products = driver.findElements(
                    By.cssSelector("div.s-main-slot div[data-asin][data-component-type='s-search-result']"));

            boolean addedSomething = false;

            for (WebElement product : products) {
                Double price = extractPrice(product);
                if (price == null || price > maxPriceLimit) continue;

                WebElement addBtn = product.findElements(
                                By.cssSelector("div.atc-btn-container button[name='submit.addToCart']"))
                        .stream().findFirst().orElse(null);

                if (addBtn != null) {
                    ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", addBtn);
                    wait.until(ExpectedConditions.elementToBeClickable(addBtn)).click();

                    expectedAddedCount++;
                    itemPrices.add(price);
                    System.out.println("Added product to cart with price: " + price);
                    Thread.sleep(300);
                    addedSomething = true;
                }
            }

            if (addedSomething) {
                hasNextPage = false; // stop after first successful page
            } else {
                // go to next results page (if any)
                WebElement next = driver.findElements(By.cssSelector("a.s-pagination-next"))
                        .stream().filter(WebElement::isDisplayed).findFirst().orElse(null);
                if (next != null) {
                    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", next);
                    System.out.println("Moved to next page…");
                } else {
                    hasNextPage = false;
                }
            }
        }
    }

    private Double extractPrice(WebElement product) {
        String integer = product.findElements(By.cssSelector("span.a-price-whole"))
                .stream().findFirst().map(WebElement::getText).orElse("").replace(",", "").trim();
        if (integer.isEmpty()) return null;

        String fraction = product.findElements(By.cssSelector("span.a-price-fraction"))
                .stream().findFirst().map(WebElement::getText).orElse("00").trim();
        return Double.valueOf(integer + "." + fraction);
    }

    //cart verification

    private void verifyCartCount() throws InterruptedException {
        Thread.sleep(2000);
        int actual = Integer.parseInt(driver.findElement(By.id("nav-cart-count")).getText().trim());

        System.out.printf("Expected Added Items: %d%n", expectedAddedCount);
        System.out.printf("Actual Cart Items   : %d%n", actual);

        if (expectedAddedCount == actual) {
            System.out.println("SUCCESS: Cart count matches expected added items.");
        } else {
            System.out.println("mISMATCH: counts differ.");
        }
    }

    //checkout

    private void proceedToCheckout() {
        System.out.println("Navigating to Checkout…");
        WebElement cartBtn = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.id("nav-cart")));

        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", cartBtn);
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", cartBtn);

        wait.until(ExpectedConditions.elementToBeClickable(By.id("sc-buy-box-ptc-button"))).click();
    }


    private void login() {
        System.out.println("Signing-in");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("ap_email_login")))
                .sendKeys(userEmail);
        driver.findElement(By.id("continue")).click();

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("ap_password")))
                .sendKeys(userPassword);
        driver.findElement(By.id("signInSubmit")).click();
    }

    private void handlePossible500() {
        if (driver.getCurrentUrl().contains("/errors/500")) {
            System.out.println("Amazon 500 error page detected – retrying");
            wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//a[contains(@href,'ref=cs_503_link')]"))).click();

            wait.until(ExpectedConditions.urlContains("amazon.eg"));
            wait.until(ExpectedConditions.elementToBeClickable(By.id("nav-cart"))).click();
            wait.until(ExpectedConditions.elementToBeClickable(By.name("proceedToRetailCheckout"))).click();
        }
    }

    //address

    private void addDeliveryAddress() throws InterruptedException {
        System.out.println("Adding new delivery address…");
        wait.until(ExpectedConditions.elementToBeClickable(By.id("add-new-address-desktop-sasp-tango-link"))).click();

        driver.findElement(By.id("address-ui-widgets-enterAddressFullName")).sendKeys("Your Full Name");
        driver.findElement(By.id("address-ui-widgets-enterAddressPhoneNumber")).sendKeys("1034543456");
        driver.findElement(By.id("address-ui-widgets-enterAddressLine1")).sendKeys("Talaat");
        driver.findElement(By.id("address-ui-widgets-enter-building-name-or-number")).sendKeys("Tower");

        // FILL CITY / AREA
        WebElement cityField = driver.findElement(
                By.id("address-ui-widgets-enterAddressCity"));
        cityField.clear();
        cityField.sendKeys("مدينة الفيوم الجديدة");

        try {
            WebElement citySuggestion = new WebDriverWait(driver, Duration.ofSeconds(6))
                    .until(ExpectedConditions.elementToBeClickable(
                            By.id("address-ui-widgets-autoCompleteResult-0")));
            citySuggestion.click();
            System.out.println("City suggestion clicked.");
        } catch (TimeoutException e) {
    //fall back
            cityField.sendKeys(Keys.ARROW_DOWN, Keys.ENTER);
        }


        WebElement district = driver.findElement(By.id("address-ui-widgets-enterAddressDistrictOrCounty"));
        district.sendKeys("مدينة الفيوم الجديدة");
        autoCompleteSelect();

        WebElement cont = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("input[data-testid='bottom-continue-button']")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", cont);

        System.out.println("Address submitted.");
    }

    private void autoCompleteSelect() {
        try {
            WebElement suggestion = new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(ExpectedConditions.visibilityOfElementLocated(
                            By.id("address-ui-widgets-autoCompleteResult-0")));
            suggestion.click();
        } catch (TimeoutException ignored) {}
    }

    //payment

    private void selectCashOnDelivery() {
        try {
            WebElement cod = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector("input[name='ppw-instrumentRowSelection'][value*='Cash']")));

            if (cod.isEnabled()) {
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", cod);
                System.out.println("Cash on Delivery selected.");
            } else {
                System.out.println("Cash on Delivery option is disabled.");
            }
        } catch (TimeoutException e) {
            System.out.println("Cash on Delivery option not found.");
        }
    }

    //total validation

    private void validateOrderTotal() {
        System.out.println("Validating total cart price…");
        double expectedTotal = itemPrices.stream().mapToDouble(Double::doubleValue).sum();

        WebElement totalEl = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//span[contains(text(),'السلع')]/ancestor::div[@class='order-summary-line-term']" +
                        "/following-sibling::div/span")));

        String totalText = totalEl.getText()
                .replace("‏", "").replace(",", "")
                .replace("جنيه", "").trim();
        double actualTotal = Double.parseDouble(totalText);

        System.out.printf("Expected total: %.2f  |  Actual total: %.2f%n", expectedTotal, actualTotal);

        if (Math.abs(expectedTotal - actualTotal) <= 1.0) {
            System.out.println("SUCCESS: Total matches (within 1 EGP).");
        } else {
            System.out.printf("MISMATCH: Difference %.2f EGP%n", expectedTotal - actualTotal);
        }
    }
}
