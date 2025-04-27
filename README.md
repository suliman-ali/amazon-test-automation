# Amazon.eg UI Automation 

##  Project Description

This project automates a user journey on [Amazon.eg] using **Selenium WebDriver**, **Java**, and **TestNG**.  
It simulates a real shopping experience — navigating categories, applying filters, adding eligible products to the cart, and validating the total price.

---

##  How to Run

1. Install **Java 17+** and **Apache Maven**.
2. Clone or download this repository.
3. Open the project in **IntelliJ IDEA** or **Eclipse** as a Maven project.
4. Ensure **ChromeDriver** is installed and matches your Chrome browser version.
5. In the terminal, run:
   ```bash
   mvn test
   ```

---

##  Customizing the Test

You can update these variables in the `AmazonScenarioTest.java` file:

 Parameter        Purpose                                           
 `userEmail`      Amazon.eg account email address                   
 `userPassword`   Amazon.eg account password                        
 `maxPriceLimit`  Maximum price (in EGP) for products to add to cart 



##  What the Test Does

The automation script covers:

1. Launch Amazon.eg and open the **hamburger menu**.
2. Navigate to **Video Games** → **All Video Games**.
3. Apply the following filters:
   - **Free Shipping**
   - **Condition = New**
4. Sort products by **Price: High to Low**.
5. Add all items priced **below 15,000 EGP** to the cart.
6. Verify that cart count matches the number of added items.
7. Proceed to **Checkout**.
8. Login with provided credentials.
9. Handle any **500 server errors** by retrying.
10. Add a **New Delivery Address**.
11. Select **Cash on Delivery** as payment method if available.
12. Validate that the **total cart amount** matches the expected total.

## Happy testing