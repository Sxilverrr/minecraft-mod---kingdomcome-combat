package com.kingdomcomecombat.passive;

import java.util.Map;

public record PassiveSkillConfig(
        String id,
        String name,
        String description,
        String icon,
        Source source,
        String advancement,
        int experienceCost,
        Map<String, Text> translations,
        Map<String, Double> perks
) {
    public enum Source {
        BOOK,
        ADVANCEMENT,
        EXPERIENCE
    }

    public PassiveSkillConfig(
            String id,
            String name,
            String description,
            String icon,
            Source source,
            String advancement,
            int experienceCost
    ) {
        this(id, name, description, icon, source, advancement, experienceCost, Map.of(), Map.of());
    }

    public PassiveSkillConfig {
        id = id == null ? "" : id;
        name = name == null || name.isBlank() ? id : name;
        description = description == null ? "" : description;
        icon = icon == null ? "" : icon;
        source = source == null ? Source.BOOK : source;
        advancement = advancement == null ? "" : advancement;
        experienceCost = Math.max(0, experienceCost);
        translations = translations == null ? Map.of() : Map.copyOf(translations);
        perks = perks == null ? Map.of() : Map.copyOf(perks);
    }

    public String name(String language) {
        Text text = translation(language);
        return text == null || text.name().isBlank() ? name : text.name();
    }

    public String description(String language) {
        Text text = translation(language);
        return text == null || text.description().isBlank() ? description : text.description();
    }

    public double perk(String key, double fallback) {
        return perks.getOrDefault(key, fallback);
    }

    public boolean hasPerk(String key) {
        return perks.containsKey(key);
    }

    private Text translation(String language) {
        if (language == null || language.isBlank() || translations.isEmpty()) {
            return null;
        }
        String normalized = language.toLowerCase();
        Text exact = translations.get(normalized);
        if (exact != null) {
            return exact;
        }
        int separator = normalized.indexOf('_');
        if (separator < 0) {
            separator = normalized.indexOf('-');
        }
        return separator > 0 ? translations.get(normalized.substring(0, separator)) : null;
    }

    public record Text(String name, String description) {
        public Text {
            name = name == null ? "" : name;
            description = description == null ? "" : description;
        }
    }
}
