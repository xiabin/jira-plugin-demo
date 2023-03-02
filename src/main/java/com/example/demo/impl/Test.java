package com.example.demo.impl;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

public class Test {
    public static void main(String[] args) {
        try {
            String dateString = "Sun Mar 26 2023 00:00:00 GMT+0800 (China Standard Time)";
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE MMM dd yyyy HH:mm:ss 'GMT'Z '('z')'", Locale.ENGLISH);
            LocalDate localDate = LocalDate.parse(dateString, formatter);

            System.out.printf(localDate.toString());
        } catch (DateTimeParseException ex) {
            ex.printStackTrace();
        }
    }
}
