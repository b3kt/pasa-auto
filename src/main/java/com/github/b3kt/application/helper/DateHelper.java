package com.github.b3kt.application.helper;

import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class DateHelper {

    public static final String DATE_FORMAT_SLASH_YYYYMMDD = "yyyy/MM/dd";

    public static Date parse(String input) {
        if(StringUtils.isBlank(input)){
            return null;
        }

        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_FORMAT_SLASH_YYYYMMDD);
            LocalDate localDate = LocalDate.parse(input, formatter);

            return Date.from(localDate
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant());
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to parse date: " + input + ". Expected format: yyyy/MM/dd", e);
        }
    }
}
