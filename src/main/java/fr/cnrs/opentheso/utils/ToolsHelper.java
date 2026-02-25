package fr.cnrs.opentheso.utils;


import java.net.URI;
import java.net.URISyntaxException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Entities;

public class ToolsHelper {

    public static String getNewId(int length, boolean isUpperCase, boolean isUseNoidCheck) {
        String chars = "0123456789bcdfghjklmnpqrstvwxz";
        StringBuilder pass = new StringBuilder();
        String idArk;
        for (int x = 0; x < length; x++) {
            int i = (int) Math.floor(Math.random() * (chars.length() - 1));
            pass.append(chars.charAt(i));
        }
        idArk = pass.toString();
        if(isUseNoidCheck) {
            NoIdCheckDigit noIdCheckDigit = new NoIdCheckDigit();
            String checkCode = noIdCheckDigit.getControlCharacter(idArk);
            idArk = idArk + "-" + checkCode;             
        }
        
        if(isUpperCase)
            return idArk.toUpperCase();
        else
            return idArk;
    }

    public boolean isValidURI(String uriStr) {
        if (uriStr == null || uriStr.isBlank()) {
            return false;
        }
        try {
            URI uri = new URI(uriStr);
            return uri.isAbsolute();
        } catch (URISyntaxException e) {
            return false;
        }
    }

    public String normalizeHtml(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }

        Document doc = Jsoup.parseBodyFragment(html);

        // paramètres de sortie propres
        doc.outputSettings()
                .prettyPrint(true)               // indentation
                .indentAmount(2)
                .syntax(Document.OutputSettings.Syntax.html)
                .escapeMode(Entities.EscapeMode.base);

        return doc.body().html();
    }

}
