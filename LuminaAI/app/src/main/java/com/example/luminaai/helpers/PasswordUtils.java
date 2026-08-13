package com.example.luminaai.helpers;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class PasswordUtils {

    public static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(password.getBytes());

            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }


    public static String checkPasswordStrength(String password) {
        if (password == null || password.isEmpty()) {
            return "Password is empty";
        }

        if (password.length() < 8) {
            return "Passwords must have at least 8 characters!";
        }

        if (password.length() > 30) {
            return "Passwords must not exceed 30 characters!";
        }

        if (!password.matches("^[a-zA-Z0-9@#$%^&+=._\\-\\[\\]{};:'\",<.>/?`~\\\\|]{8,30}$")) {
            return "Passwords containing unusual characters are not allowed!";
        }

        boolean hasLetter = password.matches(".*[a-zA-Z].*");
        boolean hasDigit = password.matches(".*[0-9].*");
        if (!hasLetter || !hasDigit) {
            return "The password is too weak! It must include both letters and numbers.";
        }

        return "Ok";
    }
}
