package com.anurag.itds.dataset;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Minimal RFC 4180 CSV reader (quoted fields, escaped quotes, embedded commas/newlines, UTF-8 BOM). */
public final class CsvParser {

    private CsvParser() {
    }

    /** Parses CSV with a header row into maps keyed by trimmed header name. Blank lines are skipped. */
    public static List<Map<String, String>> parseWithHeader(InputStream in) throws IOException {
        List<List<String>> rows = parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        List<Map<String, String>> result = new ArrayList<>();
        if (rows.isEmpty()) {
            return result;
        }
        List<String> header = rows.get(0).stream().map(String::trim).toList();
        for (int i = 1; i < rows.size(); i++) {
            List<String> row = rows.get(i);
            if (row.size() == 1 && row.get(0).isBlank()) {
                continue;
            }
            Map<String, String> record = new LinkedHashMap<>();
            for (int c = 0; c < header.size(); c++) {
                record.put(header.get(c), c < row.size() ? row.get(c) : "");
            }
            result.add(record);
        }
        return result;
    }

    public static List<List<String>> parse(String text) {
        if (text.startsWith("\uFEFF")) {
            text = text.substring(1);
        }
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (inQuotes) {
                if (ch == '"') {
                    if (i + 1 < text.length() && text.charAt(i + 1) == '"') {
                        field.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    field.append(ch);
                }
            } else if (ch == '"') {
                inQuotes = true;
            } else if (ch == ',') {
                row.add(field.toString());
                field.setLength(0);
            } else if (ch == '\n' || ch == '\r') {
                if (ch == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') {
                    i++;
                }
                row.add(field.toString());
                field.setLength(0);
                rows.add(row);
                row = new ArrayList<>();
            } else {
                field.append(ch);
            }
        }
        if (field.length() > 0 || !row.isEmpty()) {
            row.add(field.toString());
            rows.add(row);
        }
        return rows;
    }
}
