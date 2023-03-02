package com.example.demo.impl;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

public class Test {
    public static void main(String[] args) {
        try {
            long timestamp = 1679760000000L; // 假设时间戳为2022-03-25 00:00:00
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d/MMM/yy");
            Instant instant = Instant.ofEpochMilli(timestamp);
            LocalDate localDate = instant.atZone(ZoneId.systemDefault()).toLocalDate();
            System.out.println(localDate.format(formatter));
        } catch (DateTimeParseException ex) {
            ex.printStackTrace();
        }
    }
}
