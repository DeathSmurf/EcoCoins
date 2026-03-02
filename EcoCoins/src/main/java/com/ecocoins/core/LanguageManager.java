package com.ecocoins.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.Message;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public final class LanguageManager {
    private final HytaleLogger logger;
    private final Path langDir;
    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, Map<String, String>> messages = new HashMap<>();
    // Usa el idioma del sistema/host por defecto; luego puede ser sobreescrito por languages.json
    private String defaultLang = Locale.getDefault().toLanguageTag();
    private boolean forceLang = false;

    public LanguageManager(HytaleLogger logger, Path langDir) {
        this.logger = logger;
        this.langDir = langDir;
    }

    public void loadAll() {
        messages.clear();
        if (!Files.isDirectory(langDir)) {
            logger.at(java.util.logging.Level.WARNING).log("[EcoCoins] Languages dir no existe: %s", langDir);
            return;
        }

        // Lee languages.json si existe para default/force
        Path languagesJson = langDir.resolve("languages.json");
        if (Files.exists(languagesJson)) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> cfg = mapper.readValue(languagesJson.toFile(), Map.class);
                Object dl = cfg.get("default_language");
                Object fl = cfg.get("force_language");
                if (dl instanceof String s) defaultLang = s;
                if (fl instanceof Boolean b) forceLang = b;
            } catch (IOException e) {
                logger.at(java.util.logging.Level.WARNING).log("[EcoCoins] No pude leer languages.json: %s", e.getMessage());
            }
        }

        try (DirectoryStream<Path> ds = Files.newDirectoryStream(langDir, "*.json")) {
            for (Path p : ds) {
                String fn = p.getFileName().toString();
                if (fn.equalsIgnoreCase("languages.json")) continue;
                @SuppressWarnings("unchecked")
                Map<String, String> map = mapper.readValue(p.toFile(), Map.class);
                messages.put(stripExt(fn), map);
            }
        } catch (IOException e) {
            throw new RuntimeException("EcoCoins: error leyendo Languages/*.json", e);
        }
    }

    public int countLanguages() { return messages.size(); }

    public String resolveLang(String playerLang) {
        String fallback = normalizeLangCode(defaultLang);
        if (forceLang) return fallback;
        if (playerLang == null || playerLang.isBlank()) return fallback;

        String normalizedPlayerLang = normalizeLangCode(playerLang);
        if (messages.containsKey(normalizedPlayerLang)) {
            return normalizedPlayerLang;
        }

        // Compatibilidad entre formatos de separador (en-US <-> en_US).
        String swappedSeparator = normalizedPlayerLang.contains("-")
                ? normalizedPlayerLang.replace('-', '_')
                : normalizedPlayerLang.replace('_', '-');

        if (messages.containsKey(swappedSeparator)) {
            return swappedSeparator;
        }

        return fallback;
    }

    private static String normalizeLangCode(String lang) {
        if (lang == null) return "";
        return lang.trim().replace('_', '-');
    }

    /** Traducción plana (sin colores). */
    public String tr(String lang, String key, Map<String, Object> vars) {
        Map<String, String> table = resolveTable(lang);
        String raw = table.getOrDefault(key, key);

        if (vars != null) {
            for (var e : vars.entrySet()) {
                raw = raw.replace("{" + e.getKey() + "}", String.valueOf(e.getValue()));
            }
        }
        return raw;
    }

    private Map<String, String> resolveTable(String lang) {
        String requested = (lang == null || lang.isBlank()) ? defaultLang : lang;

        Map<String, String> exact = messages.get(requested);
        if (exact != null) return exact;

        String normalized = normalizeLangCode(requested);
        Map<String, String> normalizedHit = messages.get(normalized);
        if (normalizedHit != null) return normalizedHit;

        String swapped = normalized.contains("-")
                ? normalized.replace('-', '_')
                : normalized.replace('_', '-');
        Map<String, String> swappedHit = messages.get(swapped);
        if (swappedHit != null) return swappedHit;

        Map<String, String> defaultExact = messages.get(defaultLang);
        if (defaultExact != null) return defaultExact;

        String defaultNormalized = normalizeLangCode(defaultLang);
        Map<String, String> defaultNormalizedHit = messages.get(defaultNormalized);
        if (defaultNormalizedHit != null) return defaultNormalizedHit;

        String defaultSwapped = defaultNormalized.contains("-")
                ? defaultNormalized.replace('-', '_')
                : defaultNormalized.replace('_', '-');

        return messages.getOrDefault(defaultSwapped, Map.of());
    }

    /** Traducción como Message (con soporte de &códigos). */
    public Message trMsg(String lang, String key, Map<String, Object> vars) {
        String raw = tr(lang, key, vars);
        return legacyAmpersandToMessage(raw);
    }

    // ---------------------------
    // Color parser (&6, &l, &o, &r, etc.)
    // ---------------------------

    private static Message legacyAmpersandToMessage(String input) {
        if (input == null || input.isEmpty()) return Message.empty();

        List<Message> parts = new ArrayList<>();

        StringBuilder buf = new StringBuilder();

        String currentColor = null;
        boolean bold = false;
        boolean italic = false;
        boolean monospace = false;

        for (int i = 0; i < input.length(); i++) {
            char ch = input.charAt(i);

            if (ch == '&' && i + 1 < input.length()) {
                char code = Character.toLowerCase(input.charAt(i + 1));

                // Flush texto acumulado con el estilo actual
                if (buf.length() > 0) {
                    Message m = Message.raw(buf.toString());
                    applyStyle(m, currentColor, bold, italic, monospace);
                    parts.add(m);
                    buf.setLength(0);
                }

                // Consume el code
                i++;

                // Colores
                String mapped = mapColorCodeToHex(code);
                if (mapped != null) {
                    currentColor = mapped;
                    continue;
                }

                // Formatos
                switch (code) {
                    case 'l' -> bold = true;       // bold
                    case 'o' -> italic = true;     // italic
                    case 'p' -> monospace = true;  // (no estándar MC, pero por si alguien lo usa)
                    case 'r' -> {                  // reset
                        currentColor = null;
                        bold = false;
                        italic = false;
                        monospace = false;
                    }
                    // Ignorados: k (obfuscated), m (strikethrough), n (underline), etc.
                    default -> { /* ignore */ }
                }
                continue;
            }

            buf.append(ch);
        }

        // flush final
        if (buf.length() > 0) {
            Message m = Message.raw(buf.toString());
            applyStyle(m, currentColor, bold, italic, monospace);
            parts.add(m);
        }

        if (parts.isEmpty()) return Message.empty();
        return Message.join(parts.toArray(new Message[0]));
    }

    private static void applyStyle(Message m, String colorHex, boolean bold, boolean italic, boolean monospace) {
        if (colorHex != null) m.color(colorHex);
        if (bold) m.bold(true);
        if (italic) m.italic(true);
        if (monospace) m.monospace(true);
    }

    // Minecraft-like palette (aprox). Hytale acepta hex string tipo "#RRGGBB".
    private static String mapColorCodeToHex(char code) {
        return switch (code) {
            case '0' -> "#000000";
            case '1' -> "#0000AA";
            case '2' -> "#00AA00";
            case '3' -> "#00AAAA";
            case '4' -> "#AA0000";
            case '5' -> "#AA00AA";
            case '6' -> "#FFAA00";
            case '7' -> "#AAAAAA";
            case '8' -> "#555555";
            case '9' -> "#5555FF";
            case 'a' -> "#55FF55";
            case 'b' -> "#55FFFF";
            case 'c' -> "#FF5555";
            case 'd' -> "#FF55FF";
            case 'e' -> "#FFFF55";
            case 'f' -> "#FFFFFF";
            default -> null;
        };
    }

    private static String stripExt(String s) {
        int i = s.lastIndexOf('.');
        return i >= 0 ? s.substring(0, i) : s;
    }
}
