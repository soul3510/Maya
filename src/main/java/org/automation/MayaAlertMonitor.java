package org.automation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

import java.time.format.DateTimeFormatter;

public class MayaAlertMonitor {


    // Twilio Credentials (Replace with your actual credentials)

    public static final String ACCOUNT_SID = System.getenv("TWILIO_ACCOUNT_SID");
    public static final String AUTH_TOKEN = System.getenv("TWILIO_AUTH_TOKEN");


    // WhatsApp Numbers
    public static final String FROM_WHATSAPP_NUMBER = "whatsapp:+14155238886"; // Twilio Sandbox WhatsApp number

    public static final String TO_WHATSAPP_NUMBER = System.getenv("TO_WHATSAPP_NUMBER");


    public static void main(String[] args) throws JsonProcessingException {

        // Initialize WebDriver using WebDriverManager
        WebDriverManager.chromedriver().setup();

        // Set Chrome options
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless"); // Run in headless mode
        options.addArguments("--disable-gpu"); // Applicable to Windows OS
        options.addArguments("--window-size=1920,1080");

        // Initialize WebDriver
        WebDriver driver = new ChromeDriver(options);

        // Base URL
        String baseUrl = "https://maya.tase.co.il/reports/company";

        // Initialize page number
        int pageNumber = 1;

        LocalDate todayUTC = LocalDate.now(ZoneOffset.UTC);
        ZonedDateTime startOfDayUTC = todayUTC.atStartOfDay(ZoneOffset.UTC);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        String formattedDateFrom = startOfDayUTC.format(formatter);


        // JSON parameters
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("DateFrom", formattedDateFrom);
        queryParams.put("DateTo", "2040-12-30T22:00:00.000Z");
        queryParams.put("events", Arrays.asList(200, 100, 300, 400, 1100, 1400, 500, 600, 1200));
        queryParams.put("subevents", Arrays.asList(
                201, 270, 271, 202, 203, 254, 210, 211, 212, 907, 219, 205, 206, 258, 251, 236, 255,
                240, 252, 256, 250, 227, 226, 231, 259, 229, 278, 101, 103, 104, 105, 106, 114, 113,
                233, 273, 222, 282, 109, 281, 220, 280, 228, 110, 111, 112, 283, 102, 322, 320, 310,
                316, 318, 317, 319, 326, 309, 324, 301, 302, 303, 304, 315, 327, 312, 323, 325, 313,
                401, 403, 405, 402, 404, 501, 502, 504, 2460, 204, 1102, 1103, 1104, 1105, 221, 1404,
                1401, 1402, 230, 223, 1406, 238, 1405, 215, 239, 216, 253, 245, 217, 214, 241, 249,
                243, 244, 246, 247, 248, 910, 620, 605, 603, 601, 602, 621, 604, 606, 615, 613, 611,
                612, 622, 614, 616, 308, 224, 330, 904, 1201, 311, 305, 314, 307, 306
        ));
        queryParams.put("Page", pageNumber);
        queryParams.put("IsBreakingAnnouncement", true);

        // Initialize ObjectMapper for JSON processing
        ObjectMapper mapper = new ObjectMapper();


        // Update page number in query parameters
        queryParams.put("Page", pageNumber);

        // Convert query parameters to JSON string
        String jsonParams = mapper.writeValueAsString(queryParams);

        // URL-encode the JSON string
        String encodedParams = URLEncoder.encode(jsonParams, StandardCharsets.UTF_8);

        // Construct full URL
        String fullUrl = baseUrl + "?q=" + encodedParams;

        // Navigate to the URL
        driver.get(fullUrl);

        // Wait until the reports container is loaded
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[@id=\"scrTo\"]/maya-report-actions/div[4]")));

        // Find all report entries
        List<WebElement> allDates = driver.findElements(By.xpath("//div[contains(@class,'feedItemDate hidden-xs')]"));
        List<WebElement> allCampanies = driver.findElements(By.xpath("//h2[@class='ng-binding']"));
        List<WebElement> allAnnouncements = driver.findElements(By.xpath("//a[contains(@class,'messageContent ng-binding')]"));


        for (int i = 0; i < allDates.size(); i++) {
            System.out.println("Date: " + allDates.get(i).getText());
            System.out.println("Company: " + allCampanies.get(i).getText());
            System.out.println("Message: " + allAnnouncements.get(i).getText());
            if (i==2){
                //Stop
                break;
            }
        }




        // Initialize Twilio
        Twilio.init(ACCOUNT_SID, AUTH_TOKEN);

        // StringBuilder to compile the message
        StringBuilder messageBody = new StringBuilder();

        for (int i = 0; i < allDates.size(); i++) {
            messageBody.append(allDates.get(i).getText()).append("\n");
            messageBody.append(allCampanies.get(i).getText()).append("\n");
            messageBody.append(allAnnouncements.get(i).getText()).append("\n\n");
            if (i==2){
                //Stop
                break;
            }
        }

        // Convert StringBuilder to String
        String finalMessage = messageBody.toString();


        // Send WhatsApp message via Twilio
        Message message = Message.creator(
                new PhoneNumber(TO_WHATSAPP_NUMBER), // To number
                new PhoneNumber(FROM_WHATSAPP_NUMBER), // From number (Twilio Sandbox)
                finalMessage // Message body
        ).create();

        // Print the message SID for confirmation
        System.out.println("WhatsApp Message sent with SID: " + message.getSid());



        driver.close();
        driver.quit();
    }
}
