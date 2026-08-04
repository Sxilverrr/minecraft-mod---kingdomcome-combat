package com.kingdomcomecombat.hardship;

import java.util.Map;

public record HardshipConfig(
        String id,
        String title,
        String description,
        String icon,
        String translatedTitle,
        String translatedDescription,
        Map<String, Translation> translations,
        Map<String, Double> preks
) {
    public HardshipConfig {
        id = id == null ? "" : id;
        title = title == null ? "" : title;
        description = description == null ? "" : description;
        icon = icon == null ? "" : icon;
        translatedTitle = translatedTitle == null ? "" : translatedTitle;
        translatedDescription = translatedDescription == null ? "" : translatedDescription;
        translations = translations == null ? Map.of() : Map.copyOf(translations);
        preks = preks == null ? Map.of() : Map.copyOf(preks);
    }

    public String title(String language) {
        Translation text = translation(language);
        if (text != null && !text.title().isBlank()) return text.title();
        return !isChinese(language) && !translatedTitle.isBlank() ? translatedTitle : title;
    }

    public String description(String language) {
        Translation text = translation(language);
        if (text != null && !text.description().isBlank()) return text.description();
        return !isChinese(language) && !translatedDescription.isBlank() ? translatedDescription : description;
    }

    public boolean hasPrek(String key) { return preks.containsKey(key); }
    public double prek(String key, double fallback) { return preks.getOrDefault(key, fallback); }

    private Translation translation(String language) {
        if (language == null || language.isBlank()) return null;
        String normalized = language.toLowerCase();
        Translation exact = translations.get(normalized);
        if (exact != null) return exact;
        int split = Math.max(normalized.indexOf('_'), normalized.indexOf('-'));
        return split > 0 ? translations.get(normalized.substring(0, split)) : null;
    }

    private static boolean isChinese(String language) {
        return language != null && language.toLowerCase().startsWith("zh");
    }

    public record Translation(String title, String description) {
        public Translation {
            title = title == null ? "" : title;
            description = description == null ? "" : description;
        }
    }
}
