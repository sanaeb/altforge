package dev.sanaeb.altforge.lang;

/**
 * Languages supported for generated alt text. Each value carries the ISO 639-1
 * code we surface in API responses and the model prompt that instructs the
 * vision provider to answer in that language.
 */
public enum Language {

    EN("en", """
            Write a concise, accurate alt text for this image, suitable for the HTML alt attribute.
            Follow WCAG 2.1 guidance:
            - Describe what is visually meaningful; ignore decorative detail.
            - Keep it to one sentence, ideally under 125 characters.
            - Do not start with "image of" or "picture of".
            - Mention people, objects, actions, and any visible text.
            - Use present tense and a neutral tone.
            Return only the alt text in English. No quotes, no markdown, no leading or trailing punctuation other than a period.
            """),

    FR("fr", """
            Rédige un alt text concis et précis pour cette image, prêt à coller dans l'attribut HTML alt.
            Respecte les guidelines WCAG 2.1 :
            - Décris ce qui est visuellement signifiant ; ignore les détails décoratifs.
            - Une seule phrase, idéalement sous 125 caractères.
            - Ne commence pas par "image de" ou "photo de".
            - Mentionne les personnes, objets, actions et le texte visible.
            - Utilise le présent et un ton neutre.
            Retourne uniquement l'alt text en français. Pas de guillemets, pas de markdown, pas de ponctuation hormis le point final.
            """);

    private final String iso;
    private final String prompt;

    Language(String iso, String prompt) {
        this.iso = iso;
        this.prompt = prompt;
    }

    public String iso() {
        return iso;
    }

    public String prompt() {
        return prompt;
    }

    /** Lenient parser: falls back to {@link #EN} when the value is null or unknown. */
    public static Language fromString(String value) {
        if (value == null || value.isBlank()) {
            return EN;
        }
        return switch (value.trim().toLowerCase()) {
            case "fr", "fr-fr", "french", "français" -> FR;
            default -> EN;
        };
    }
}
