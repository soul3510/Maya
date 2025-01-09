package org.automation;

import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class IsraelTime {
    public static String getCurrentTime() {
        // Specify Israel's timezone
        ZoneId israelZone = ZoneId.of("Asia/Jerusalem");

        // Get the current date and time in Israel
        ZonedDateTime israelTime = ZonedDateTime.now(israelZone);

        // Format the date and time
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        String formattedTime = israelTime.format(formatter);

        return formattedTime;
    }
}
