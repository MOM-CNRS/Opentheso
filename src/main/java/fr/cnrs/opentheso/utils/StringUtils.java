package fr.cnrs.opentheso.utils;

import org.apache.commons.validator.routines.UrlValidator;
import java.text.Normalizer;


public class StringUtils {

    public static String clearNoteFromP(String rawNote) {
        if (rawNote.startsWith("<p>")) {
            rawNote = rawNote.substring(3); // Retire les 3 premiers caractères
        }
        if (rawNote.endsWith("</p>")) {
            rawNote = rawNote.substring(0, rawNote.length() - 4); // Retire les 4 derniers caractères
        }
        rawNote = rawNote.replaceAll("<p></p>", "");
        return rawNote;
    }

    public static String clearValue(String rawNote) {
        // enlève les code ascii non visibles
        return rawNote.replace((char) 27, ' ');
    }


    /**
     * Permet de vérifier si une URL est valide
     * @param url
     * @return
     */
    public static boolean urlValidator(String url)
    {
        // Configurer des schémas acceptés (http et https)
        String[] schemes = {"http", "https"};
        try {
            // Configurer un UrlValidator permissif
            UrlValidator defaultValidator = new UrlValidator(schemes, UrlValidator.ALLOW_LOCAL_URLS);
            return defaultValidator.isValid(url);
        } catch (Exception e) {
            System.out.println("erruer : " + e.getMessage());
        }
        return false;
    }


    /**
     * Permet de supprimer les accents d'un String
     * @param s
     * @return
     */
    public static String unaccentLowerString(String s)
    {
        if(s == null) return s;
        s = Normalizer.normalize(s, Normalizer.Form.NFD);
        s = s.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
        return s.toLowerCase();
    }

    /**
     * Cette fonction permet de caster les cotes pour les faire passer par le
     * jdbc pour échanger avec les bases de données PostgreSQL exp : l'équipe ->
     * l''équipe
     *
     * @param ligne
     * @return String une phrase avec des cotes en plus
     */
    public static String addQuotes(String ligne) {
        String ligne_prete = "";
        String ligne_temp;
        int index;

        while (true) {
            index = ligne.indexOf("'");
            if (index == -1) {
                return (ligne_prete + ligne);
            }
            ligne_temp = ligne.substring(0, index + 1);
            ligne_prete = ligne_prete + ligne_temp.concat("'");
            ligne = ligne.substring(index + 1, ligne.length());
        }
    }

    /**
     * Handle quotes and backslashes in string before database insertion.
     *
     * @param s The string to convert.
     * @return A safe string for database insertion.
     */
    public static String convertString(String s) {
        if (s == null) {
            return null;
        }
        // Handle quotes
        s = s.replaceAll("’", "\'");
        s = s.replaceAll("\\\\", "\\\\\\\\");
        return s.trim();
    }

    /**
     * Fonction qui permet de normaliser les textes pour les documents XML
     *
     * @param s
     * @return
     */
    public static String normalizeStringForXml(String s) {
        if (s == null) {
            return null;
        }
        // normalisation of words for XML
        s = s.replaceAll("&", "&amp;");
     //   s = s.replaceAll("\"", " ");
     //   s = s.replace("\"", " ");
        s = s.replaceAll("\n", " ");
        s = s.replaceAll("\t", " ");
        return s.trim();
    }

    /**
     * Fonction qui permet de normaliser les textes pour un script SQL à
     * appliquer en Java
     *
     * @param s
     * @return
     */
    public static String normalizeStringForSQL(String s) {
        if (s == null) {
            return null;
        }
        // normalisation of words for XML
        s = s.replaceAll("&", "&amp;");
        s = s.replaceAll("\"", " ");
        s = s.replace("\"", " ");
        s = s.replaceAll("\n", " ");
        s = s.replaceAll("\t", " ");
//        s = s.replaceAll("'", " ");
        return s.trim();
    }

    /**
     * Fonction qui permet de normaliser les textes pour les documents XML
     *
     * @param s
     * @return
     */
    public static String normalizeStringForIdentifier(String s) {
        if (s == null) {
            return null;
        }
        // normalisation of words for identifier
        // exp : ?idg=MT_10&idt=2
        // devient : idgMT_10idt2

        s = s.replaceAll("\\?", "");
        s = s.replaceAll("=", "");
        s = s.replaceAll("&", "");

        return s;
    }

    /**
     * Fonction qui permet de normaliser les textes pour les documents XML
     *
     * @param s
     * @return
     */
    public static String clearStringForSerach(String s) {
        if (s == null) {
            return null;
        }
        // normalisation of words for XML
        s = s.replaceAll("&", " ");
        s = s.replaceAll("-", " ");
        s = s.replaceAll("_", " ");
        s = s.replaceAll("/", " ");
        s = s.replaceAll("(", " ");
        s = s.replaceAll(")", " ");

        return s;
    }

    public static String clearAngles(String s) {
        if (s == null) {
            return null;
        }
        boolean premiereAngle = false;
        boolean deuxiemeAngle = false;
        String aux = "";
        char character;
        for (int i = 0; i < s.length(); i++) {
            character = s.charAt(i);
            if (character == '<') {
                premiereAngle = true;
            }
            if (!premiereAngle) {
                aux += character;
                deuxiemeAngle = false;
            }
            if (character == '>') {
                deuxiemeAngle = true;
                premiereAngle = false;
            }
        }
        return aux;
    }

    /**
     * permet de supprimer le retour à la ligne dans les commentaires, les notes ...
     * @param text
     * @return
     */
    public static String clearNewLine(String text) {
        text = text.replace('\r', ' ');
        text = text.replace('\n', ' ');
        text = text.replace('\t', ' ');
        return text;
    }
    
}
