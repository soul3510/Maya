package org.automation;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.TimeUnit;


public class MayaAlertMonitor {


    public static void main(String[] args) throws IOException, InterruptedException {

        // Initialize WebDriver using WebDriverManager
        WebDriverManager.chromedriver().setup();

        // Set Chrome options
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--disable-gpu"); // Applicable to Windows OS
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--headless=new"); // Use '--headless=new' for newer Chrome versions
        options.addArguments("--no-sandbox"); // Bypass OS security model
        options.addArguments("--disable-dev-shm-usage"); // Overcome limited resource problems

        // Initialize WebDriver

        WebDriver driver = new ChromeDriver(options);


        LocalDate todayUTC = LocalDate.now(ZoneOffset.UTC);
        LocalDate yesterdayUTC = todayUTC.minusDays(1);
        ZonedDateTime startOfDayUTC = yesterdayUTC.atStartOfDay(ZoneOffset.UTC);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        String formattedDateFrom = startOfDayUTC.format(formatter);
        String formattedDateTo = "2027-03-30T21:00:00.000Z";

        // Base URL
        String fullUrl = "https://maya.tase.co.il/reports/company?q=%7B%22DateFrom%22:%22" + formattedDateFrom + "%22,%22DateTo%22:%22" + formattedDateTo + "%22,%22IsBreakingAnnouncement%22:true,%22Page%22:1,%22events%22:%5B200,100,300,400,1100,1400,600,1200,1500%5D,%22subevents%22:%5B201,270,271,202,203,254,210,211,212,907,219,205,206,258,251,236,255,240,252,256,250,227,226,231,259,229,278,101,103,104,105,106,114,113,233,273,222,282,109,281,220,280,228,110,111,112,283,102,322,320,310,316,318,317,319,326,309,324,301,302,303,304,315,327,312,323,325,313,401,403,405,402,404,501,502,504,2460,204,1102,1103,1104,1105,221,1404,1401,1402,230,223,1406,238,1405,620,605,603,601,602,621,604,606,615,613,611,612,622,614,616,308,224,330,904,1201,311,305,314,307,306,213,1501,1502,1503,1504%5D%7D";

        System.out.println("Starting MayaAlertMonitor...");

        System.out.println(fullUrl);

        // Navigate to the URL
        driver.get(fullUrl);

        // Wait until the reports container is loaded
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[@id=\"scrTo\"]/maya-report-actions/div[4]")));

        TimeUnit.SECONDS.sleep(5);
        System.out.println("Page is up...");

        // Find all report entries
        List<WebElement> allDates = driver.findElements(By.xpath("//div[contains(@class,'feedItemDate hidden-xs')]"));
        List<WebElement> allCampanies = driver.findElements(By.xpath("//h2[@class='ng-binding']"));
        List<WebElement> allAnnouncements = driver.findElements(By.xpath("//a[contains(@class,'messageContent ng-binding')]"));


        System.out.println("All entries fetched...");
        System.out.println("Number of dates: " + allDates.size());
        System.out.println("Number of companies: " + allCampanies.size());
        System.out.println("Number of announcements: " + allAnnouncements.size());


        for (int i = 0; i < allDates.size(); i++) {
            System.out.println("Date: " + allDates.get(i).getText());
            System.out.println("Company: " + allCampanies.get(i).getText());
            System.out.println("Message: " + allAnnouncements.get(i).getText());
            if (i == 2) {
                //Stop
                break;
            }
        }



        // StringBuilder to compile the message
        StringBuilder messageBody = new StringBuilder();
        System.out.println("Trying to send whatsup message with: " + messageBody);


        for (int i = 0; i < allDates.size(); i++) {
            messageBody.append(allDates.get(i).getText()).append("\n");
            messageBody.append(allCampanies.get(i).getText()).append("\n");
            messageBody.append(allAnnouncements.get(i).getText()).append("\n\n");
            if (i == 2) {
                //Stop
                break;
            }
        }

        // Convert StringBuilder to String
        String finalMessage = messageBody.toString();


        sendWhatapp(finalMessage);
        sendSms(finalMessage);

        driver.close();
        driver.quit();

        System.out.println("Done. ");
    }


    public static void sendSms(String finalMessage) {
         String ACCOUNT_SID = System.getenv("TWILIO_ACCOUNT_SID");
         String AUTH_TOKEN = System.getenv("TWILIO_AUTH_TOKEN");
         String FROM_WHATSAPP_NUMBER = "+12186950942"; // Twilio Sandbox WhatsApp number
         String TO_WHATSAPP_NUMBER = "+972508266273";
        Twilio.init(ACCOUNT_SID, AUTH_TOKEN);

        Message message = Message.creator(
                new com.twilio.type.PhoneNumber(TO_WHATSAPP_NUMBER),
                new com.twilio.type.PhoneNumber(FROM_WHATSAPP_NUMBER),
                finalMessage).create();
        System.out.println(message.getSid());

        // Print the message SID for confirmation
        System.out.println("WhatsApp Message sent with SID: " + message.getSid());
    }

    public static void sendWhatapp(String finalMessage) {
        String ACCOUNT_SID = System.getenv("TWILIO_ACCOUNT_SID");
        String AUTH_TOKEN = System.getenv("TWILIO_AUTH_TOKEN");
        String FROM_WHATSAPP_NUMBER = "whatsapp:+14155238886"; // Twilio Sandbox WhatsApp number
//        String FROM_WHATSAPP_NUMBER = "whatsapp:+12186950942"; // Twilio Sandbox WhatsApp number
        String TO_WHATSAPP_NUMBER = "whatsapp:+972508266273";
        Twilio.init(ACCOUNT_SID, AUTH_TOKEN);

        // Send WhatsApp message via Twilio
        Message message = Message.creator(
                new PhoneNumber(TO_WHATSAPP_NUMBER), // To number
                new PhoneNumber(FROM_WHATSAPP_NUMBER), // From number (Twilio Sandbox)
                finalMessage // Message body
        ).create();

        // Print the message SID for confirmation
        System.out.println("WhatsApp Message sent with SID: " + message.getSid());
    }

}




