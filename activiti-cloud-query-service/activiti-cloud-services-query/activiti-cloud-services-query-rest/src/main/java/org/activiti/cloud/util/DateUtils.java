package org.activiti.cloud.util;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Date;

public abstract class DateUtils {

    public static final DateTimeFormatter ISO_DATE_TIME_FORMATTER = new DateTimeFormatterBuilder()
        .append(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        .optionalStart()
        .appendOffset("+HH:MM", "+00:00")
        .optionalEnd()
        .optionalStart()
        .appendOffset("+HHMM", "+0000")
        .optionalEnd()
        .optionalStart()
        .appendOffset("+HH", "Z")
        .optionalEnd()
        .toFormatter();

    public static Date parseDate(String stringDate) {
        return Date.from(Instant.from(DateUtils.ISO_DATE_TIME_FORMATTER.parse(stringDate)));
    }
}
