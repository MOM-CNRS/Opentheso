package fr.cnrs.opentheso.utils;

import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;


public class EmailUtils {


    public static boolean isValidEmailAddress(String email) {
        try {
            InternetAddress emailAddr = new InternetAddress(email);
            emailAddr.validate();
            return true;
        } catch (AddressException ex) {
            return false;
        }
    }

}
