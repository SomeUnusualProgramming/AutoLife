package com.lifeai.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class DateTimeParsingService {

    private static final Map<String, Integer> MONTH_MAP = new HashMap<>();
    private static final Map<String, DayOfWeek> DAY_MAP = new HashMap<>();

    static {
        MONTH_MAP.put("january", 1);
        MONTH_MAP.put("february", 2);
        MONTH_MAP.put("march", 3);
        MONTH_MAP.put("april", 4);
        MONTH_MAP.put("may", 5);
        MONTH_MAP.put("june", 6);
        MONTH_MAP.put("july", 7);
        MONTH_MAP.put("august", 8);
        MONTH_MAP.put("september", 9);
        MONTH_MAP.put("october", 10);
        MONTH_MAP.put("november", 11);
        MONTH_MAP.put("december", 12);
        MONTH_MAP.put("styczeń", 1);
        MONTH_MAP.put("luty", 2);
        MONTH_MAP.put("marzec", 3);
        MONTH_MAP.put("kwiecień", 4);
        MONTH_MAP.put("maj", 5);
        MONTH_MAP.put("czerwiec", 6);
        MONTH_MAP.put("lipiec", 7);
        MONTH_MAP.put("sierpień", 8);
        MONTH_MAP.put("wrzesień", 9);
        MONTH_MAP.put("październik", 10);
        MONTH_MAP.put("listopad", 11);
        MONTH_MAP.put("grudzień", 12);

        DAY_MAP.put("monday", DayOfWeek.MONDAY);
        DAY_MAP.put("tuesday", DayOfWeek.TUESDAY);
        DAY_MAP.put("wednesday", DayOfWeek.WEDNESDAY);
        DAY_MAP.put("thursday", DayOfWeek.THURSDAY);
        DAY_MAP.put("friday", DayOfWeek.FRIDAY);
        DAY_MAP.put("saturday", DayOfWeek.SATURDAY);
        DAY_MAP.put("sunday", DayOfWeek.SUNDAY);
        DAY_MAP.put("poniedziałek", DayOfWeek.MONDAY);
        DAY_MAP.put("wtorek", DayOfWeek.TUESDAY);
        DAY_MAP.put("środa", DayOfWeek.WEDNESDAY);
        DAY_MAP.put("czwartek", DayOfWeek.THURSDAY);
        DAY_MAP.put("piątek", DayOfWeek.FRIDAY);
        DAY_MAP.put("sobota", DayOfWeek.SATURDAY);
        DAY_MAP.put("niedziela", DayOfWeek.SUNDAY);
    }

    public LocalDateTime parseRelativeDate(String input, LocalDateTime baseTime) {
        if (input == null || input.trim().isEmpty()) {
            return baseTime;
        }

        String normalized = input.toLowerCase().trim();
        LocalDateTime result = baseTime;

        try {
            // Try exact date pattern: "December 25", "25 December", "12/25", "2024-12-25"
            result = parseExactDate(normalized, result);
            if (!result.equals(baseTime)) {
                return result;
            }

            // Try relative dates: "tomorrow", "today", "yesterday", "in 2 days"
            if (normalized.contains("tomorrow") || normalized.contains("jutro")) {
                return baseTime.plusDays(1);
            }
            if (normalized.contains("yesterday") || normalized.contains("wczoraj")) {
                return baseTime.minusDays(1);
            }
            if (normalized.contains("today") || normalized.contains("dziś") || normalized.contains("dzisiaj")) {
                return baseTime;
            }

            // Try "next Monday", "next week", etc.
            result = parseNextRelative(normalized, result);
            if (!result.equals(baseTime)) {
                return result;
            }

            // Try "in X days/weeks/hours"
            result = parseInRelative(normalized, result);
            if (!result.equals(baseTime)) {
                return result;
            }

            // Parse time of day
            result = parseTimeOfDay(normalized, result);

        } catch (Exception e) {
            log.warn("Error parsing date/time: {}", input, e);
        }

        return result;
    }

    private LocalDateTime parseExactDate(String input, LocalDateTime baseTime) {
        // Try DD/MM/YYYY format
        Pattern datePattern = Pattern.compile("(\\d{1,2})/(\\d{1,2})/(\\d{4})");
        Matcher matcher = datePattern.matcher(input);
        if (matcher.find()) {
            try {
                int day = Integer.parseInt(matcher.group(1));
                int month = Integer.parseInt(matcher.group(2));
                int year = Integer.parseInt(matcher.group(3));
                return baseTime.withYear(year).withMonth(month).withDayOfMonth(day);
            } catch (Exception e) {
                log.debug("Failed to parse date in DD/MM/YYYY format", e);
            }
        }

        // Try YYYY-MM-DD format
        datePattern = Pattern.compile("(\\d{4})-(\\d{1,2})-(\\d{1,2})");
        matcher = datePattern.matcher(input);
        if (matcher.find()) {
            try {
                int year = Integer.parseInt(matcher.group(1));
                int month = Integer.parseInt(matcher.group(2));
                int day = Integer.parseInt(matcher.group(3));
                return baseTime.withYear(year).withMonth(month).withDayOfMonth(day);
            } catch (Exception e) {
                log.debug("Failed to parse date in YYYY-MM-DD format", e);
            }
        }

        // Try "25 December" or "December 25"
        for (Map.Entry<String, Integer> monthEntry : MONTH_MAP.entrySet()) {
            if (input.contains(monthEntry.getKey())) {
                Pattern monthPattern = Pattern.compile("(\\d{1,2})\\s+" + Pattern.quote(monthEntry.getKey()) +
                        "|" + Pattern.quote(monthEntry.getKey()) + "\\s+(\\d{1,2})");
                Matcher monthMatcher = monthPattern.matcher(input);
                if (monthMatcher.find()) {
                    try {
                        int day = monthMatcher.group(1) != null ?
                                Integer.parseInt(monthMatcher.group(1)) :
                                Integer.parseInt(monthMatcher.group(2));
                        int month = monthEntry.getValue();
                        return baseTime.withMonth(month).withDayOfMonth(day);
                    } catch (Exception e) {
                        log.debug("Failed to parse month date", e);
                    }
                }
            }
        }

        return baseTime;
    }

    private LocalDateTime parseNextRelative(String input, LocalDateTime baseTime) {
        for (Map.Entry<String, DayOfWeek> dayEntry : DAY_MAP.entrySet()) {
            if (input.contains("next " + dayEntry.getKey()) || input.contains("następny " + dayEntry.getKey())) {
                return baseTime.with(TemporalAdjusters.next(dayEntry.getValue()));
            }
            if (input.contains("this " + dayEntry.getKey()) || input.contains("ten " + dayEntry.getKey())) {
                LocalDateTime nextOccurrence = baseTime.with(TemporalAdjusters.next(dayEntry.getValue()));
                if (nextOccurrence.toLocalDate().equals(baseTime.toLocalDate())) {
                    return nextOccurrence;
                }
                return baseTime.with(TemporalAdjusters.previousOrSame(dayEntry.getValue()));
            }
            if (input.contains(dayEntry.getKey()) && !input.contains("next") && !input.contains("this")) {
                DayOfWeek targetDay = dayEntry.getValue();
                DayOfWeek currentDay = baseTime.getDayOfWeek();
                if (currentDay.equals(targetDay)) {
                    return baseTime;
                }
                return baseTime.with(TemporalAdjusters.next(targetDay));
            }
        }

        if (input.contains("next week") || input.contains("przyszły tydzień")) {
            return baseTime.plusWeeks(1);
        }
        if (input.contains("next month") || input.contains("przyszły miesiąc")) {
            return baseTime.plusMonths(1);
        }

        return baseTime;
    }

    private LocalDateTime parseInRelative(String input, LocalDateTime baseTime) {
        Pattern pattern = Pattern.compile("in\\s+(\\d+)\\s+(day|week|hour|minute|hour)s?|za\\s+(\\d+)\\s+(dni|tygodni|godzin|minut)");
        Matcher matcher = pattern.matcher(input);

        if (matcher.find()) {
            String number = matcher.group(1) != null ? matcher.group(1) : matcher.group(3);
            String unit = matcher.group(2) != null ? matcher.group(2) : matcher.group(4);

            try {
                int amount = Integer.parseInt(number);
                switch (unit.toLowerCase()) {
                    case "day":
                    case "dni":
                        return baseTime.plusDays(amount);
                    case "week":
                    case "tygodni":
                        return baseTime.plusWeeks(amount);
                    case "hour":
                    case "godzin":
                        return baseTime.plusHours(amount);
                    case "minute":
                    case "minut":
                        return baseTime.plusMinutes(amount);
                }
            } catch (Exception e) {
                log.debug("Failed to parse 'in X units' format", e);
            }
        }

        return baseTime;
    }

    private LocalDateTime parseTimeOfDay(String input, LocalDateTime baseTime) {
        // Try HH:MM format
        Pattern timePattern = Pattern.compile("(\\d{1,2}):(\\d{2})");
        Matcher matcher = timePattern.matcher(input);
        if (matcher.find()) {
            try {
                int hour = Integer.parseInt(matcher.group(1));
                int minute = Integer.parseInt(matcher.group(2));
                if (hour >= 0 && hour <= 23 && minute >= 0 && minute <= 59) {
                    return baseTime.withHour(hour).withMinute(minute).withSecond(0);
                }
            } catch (Exception e) {
                log.debug("Failed to parse HH:MM format", e);
            }
        }

        // Try "2pm", "14:00", "morning", "evening", "noon", "midnight"
        if (input.contains("morning") || input.contains("rano")) {
            return baseTime.withHour(8).withMinute(0).withSecond(0);
        }
        if (input.contains("afternoon") || input.contains("popołudnie")) {
            return baseTime.withHour(14).withMinute(0).withSecond(0);
        }
        if (input.contains("evening") || input.contains("wieczór")) {
            return baseTime.withHour(18).withMinute(0).withSecond(0);
        }
        if (input.contains("night") || input.contains("noc")) {
            return baseTime.withHour(22).withMinute(0).withSecond(0);
        }
        if (input.contains("noon") || input.contains("południe")) {
            return baseTime.withHour(12).withMinute(0).withSecond(0);
        }
        if (input.contains("midnight") || input.contains("północ")) {
            return baseTime.withHour(0).withMinute(0).withSecond(0);
        }

        return baseTime;
    }
}
