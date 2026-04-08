import java.util.*;
import java.io.*;
import java.nio.file.*;
public class Vigenere {
    public static void main(String[] args) throws IOException {

        // Error messages for incorrect args
        if (args.length < 3) {
            System.out.println("Please enter the following args: -e/-d file key");
            return;
        }
        if (!args[0].equals("-e") && !args[0].equals("-d")) {
            System.out.println("First arg must be -e or -d");
            return;
        }
        if (args[2].isEmpty()) {
            System.out.println("Key cannot be empty");
            return;
        }
        if (args[2].chars().mapToObj(c -> (char) c).anyMatch(c -> !Character.isLetter(c))) {
            System.out.println("Key must only contain letters");
            return;
        }

        //Define variables
        boolean encrypt = args[0].equals("-e");
        String key = args[2].toUpperCase();
        Path filePath = Paths.get(args[1]);
        String text;

        try {
            text = Files.readString(filePath).toUpperCase();
            if (text.isEmpty()) {
                System.out.println("File is empty: " + args[1]);
                return;
            }
        }
        catch (IOException e) {
            System.out.println("File does not exist: " + filePath);
            return;
        }

        if (encrypt) {
            System.out.println(encrypt(text, key));
        }
        else {
            System.out.println(decrypt(text, key));
        }
    }

    public static String encrypt(String text, String key) {
        String sanitizedText = text.replaceAll("[^A-Z]", "");
        StringBuilder result = new StringBuilder();
        int shift;
        for (int i = 0; i < sanitizedText.length(); i++) {
            shift = key.charAt(i % key.length()) - 'A';
            result.append((char) ('A' + (sanitizedText.charAt(i) - 'A' + shift) % 26));
        }
        return result.toString();
    }

    public static String decrypt(String text, String key) {
        String sanitizedText = text.replaceAll("[^A-Z]", "");
        StringBuilder result = new StringBuilder();
        int shift;
        for (int i = 0; i < sanitizedText.length(); i++) {
            shift = key.charAt(i % key.length()) - 'A';
            result.append((char) ('A' + (sanitizedText.charAt(i) - 'A' - shift + 26) % 26));
        }
        return result.toString();
    }
}