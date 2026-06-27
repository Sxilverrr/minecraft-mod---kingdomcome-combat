package com.kingdomcomecombat.item;

import java.util.HashMap;
import java.util.Map;

public final class SkillBookTexts {
    private static final Map<String, Entry> BY_ID = new HashMap<>();

    private SkillBookTexts() {
    }

    public static void clear() {
        BY_ID.clear();
    }

    public static void put(String id, Entry entry) {
        if (id == null || id.isBlank()) {
            return;
        }

        BY_ID.put(id, entry);
    }

    public static Entry get(String id) {
        Entry entry = BY_ID.get(id);
        if (entry != null) {
            return entry;
        }

        return new Entry(
                "技能书",
                "Kingdom Come Combat",
                "这本书记录了一门战斗技巧。",
                "翻到这一页时，你会试着领会书中记载的招式。",
                "",
                Map.of()
        );
    }

    public static Map<String, Entry> all() {
        return Map.copyOf(BY_ID);
    }

    public record Entry(
            String title,
            String author,
            String firstPage,
            String secondPage,
            String illustration,
            Map<String, EntryText> translations
    ) {
        public Entry(String title, String author, String firstPage, String secondPage, String illustration) {
            this(title, author, firstPage, secondPage, illustration, Map.of());
        }

        public Entry {
            title = title == null || title.isBlank() ? "技能书" : title;
            author = author == null || author.isBlank() ? "Kingdom Come Combat" : author;
            firstPage = firstPage == null || firstPage.isBlank()
                    ? "这本书记录了一门战斗技巧。"
                    : firstPage;
            secondPage = secondPage == null || secondPage.isBlank()
                    ? "翻到这一页时，你会试着领会书中记载的招式。"
                    : secondPage;
            illustration = illustration == null ? "" : illustration;
            translations = translations == null ? Map.of() : Map.copyOf(translations);
        }

        public String title(String language) {
            EntryText text = translation(language);
            return text == null || text.title().isBlank() ? title : text.title();
        }

        public String firstPage(String language) {
            EntryText text = translation(language);
            return text == null || text.firstPage().isBlank() ? firstPage : text.firstPage();
        }

        public String secondPage(String language) {
            EntryText text = translation(language);
            return text == null || text.secondPage().isBlank() ? secondPage : text.secondPage();
        }

        public String illustration(String language) {
            EntryText text = translation(language);
            return text == null || text.illustration().isBlank() ? illustration : text.illustration();
        }

        private EntryText translation(String language) {
            if (language == null || language.isBlank() || translations.isEmpty()) {
                return null;
            }
            String normalized = language.toLowerCase();
            EntryText exact = translations.get(normalized);
            if (exact != null) {
                return exact;
            }
            int separator = normalized.indexOf('_');
            if (separator < 0) {
                separator = normalized.indexOf('-');
            }
            return separator > 0 ? translations.get(normalized.substring(0, separator)) : null;
        }
    }

    public record EntryText(
            String title,
            String firstPage,
            String secondPage,
            String illustration
    ) {
        public EntryText {
            title = title == null ? "" : title;
            firstPage = firstPage == null ? "" : firstPage;
            secondPage = secondPage == null ? "" : secondPage;
            illustration = illustration == null ? "" : illustration;
        }
    }
}
