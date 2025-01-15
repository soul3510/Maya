package org.automation;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Properties;
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
        List<WebElement> allCompanies = driver.findElements(By.xpath("//h2[@class='ng-binding']"));
        List<WebElement> allAnnouncements = driver.findElements(By.xpath("//a[contains(@class,'messageContent ng-binding')]"));

        System.out.println("All entries fetched...");
        System.out.println("Number of dates: " + allDates.size());
        System.out.println("Number of companies: " + allCompanies.size());
        System.out.println("Number of announcements: " + allAnnouncements.size());

        if (allDates.isEmpty() || allCompanies.isEmpty() || allAnnouncements.isEmpty()) {
            System.out.println("No announcements found.");
            driver.quit();
            return;
        }

        // Extract the latest announcement
        String latestDate = allDates.get(0).getText();
        String latestCompany = allCompanies.get(0).getText();
        String latestAnnouncement = allAnnouncements.get(0).getText();

        System.out.println("Latest Date: " + latestDate);
        System.out.println("Latest Company: " + latestCompany);
        System.out.println("Latest Announcement: " + latestAnnouncement);

        try (Connection conn = DBHelper.mysqlConnect()) {
            // Check if the announcement already exists in the database
            String query = "SELECT COUNT(*) FROM Maya WHERE Message = '" + latestAnnouncement + "'";
            ResultSet rs = conn.createStatement().executeQuery(query);
            rs.next();
            boolean exists = rs.getInt(1) > 0;

            if (exists) {
                System.out.println("Announcement already exists in the database.");
            } else {
                // Insert new announcement
                String insertQuery = "INSERT INTO Maya (announcement_time, Message, Company) VALUES ('" + latestDate + "', '" + latestAnnouncement + "', '" + latestCompany + "')";
                DBHelper.executeUpdate(insertQuery);
                System.out.println("New announcement inserted into the database.");


                // StringBuilder to compile the message
                StringBuilder messageBody = new StringBuilder();
                messageBody.append(latestDate).append("\n");
                messageBody.append(latestCompany).append("\n");
                messageBody.append(latestAnnouncement).append("\n\n");

                // Convert StringBuilder to String
                String finalMessage = messageBody.toString();

                // Send email
                sendEmail(finalMessage);
                System.out.println("Email sent for new announcement.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void sendEmail(String finalMessage) throws MessagingException {
        // Sender's email ID
        final String from = System.getenv("EMAIL");
        final String password = System.getenv("APP_PASSWORD"); // App password generated from Google
        // Recipient's email ID
        final String to = System.getenv("EMAIL");

        // SMTP configuration
        String host = "smtp.gmail.com";
        Properties properties = new Properties();
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.host", host);
        properties.put("mail.smtp.port", "587");

        // Create session
        Session session = Session.getInstance(properties, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(from, password);
            }
        });

        // Positive keywords in Hebrew
        String[] positiveKeywords = {
                "הצלחה", "מכירה", "נבחר ע\"י", "התחלת טיפול", "תוצאות ניסויי", "זכייה", "קבלת אישור"
        };

        // Build the table structure
        StringBuilder tableBuilder = new StringBuilder();
        tableBuilder.append("<table border='1' style='border-collapse:collapse; width:100%; text-align:right; direction:rtl;'>");
        tableBuilder.append("<tr style='background-color:#f2f2f2;'>")
                .append("<th>תאריך</th>")
                .append("<th>חברה</th>")
                .append("<th>הודעה</th>")
                .append("</tr>");

        String[] rows = finalMessage.split("\n\n");
        for (String row : rows) {
            String[] columns = row.split("\n");
            if (columns.length == 3) {
                String message = columns[2];

                // Check for positive keywords in the message
                boolean containsPositiveKeyword = false;
                for (String keyword : positiveKeywords) {
                    if (message.contains(keyword)) {
                        containsPositiveKeyword = true;
                        break;
                    }
                }

                // Highlight the row if a positive keyword is found
                String rowStyle = containsPositiveKeyword ? "background-color:#d4f4d2;" : "";

                tableBuilder.append("<tr style='").append(rowStyle).append("'>")
                        .append("<td>").append(columns[0]).append("</td>")
                        .append("<td>").append(columns[1]).append("</td>")
                        .append("<td>").append(message).append("</td>")
                        .append("</tr>");
            }
        }
        tableBuilder.append("</table>");

        // Create email message
        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(from));
        message.setRecipients(
                Message.RecipientType.TO, InternetAddress.parse(to));
        message.setSubject(IsraelTime.getCurrentTime() + " - מאיה הודעות מתפרצות");
        message.setHeader("Content-Type", "text/html; charset=UTF-8");
        message.setContent(
                "<h1 style='direction:rtl;'>התראות מאיה</h1>" + tableBuilder,
                "text/html; charset=UTF-8"
        );

        // Send the email
        Transport.send(message);
        System.out.println("Email sent successfully!");
    }
}