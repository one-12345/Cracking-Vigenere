import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

class VigenereCracker {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Please enter the filename of the encrypted text as a command line argument.");
            return;
        }

        Path filePath = Paths.get(args[0]);
        String text;
        HashMap<Integer, Integer> keyLengthFrequency = new HashMap<>();

        try {
            text = Files.readString(filePath).toUpperCase();
            if (text.isEmpty()) {
                System.out.println("File is empty: " + args[1]);
                return;
            }
            text = text.replaceAll("[^A-Z]", ""); // Sanitizes text (in case of something weird)
            keyLengthFrequency = keyLengthFinder(text);
            ArrayList<String> possibleKeys = new ArrayList<String>();
            for (Integer keyLength : keyLengthFrequency.keySet()) {
                possibleKeys.add(keyFinder(text,keyLength));
            }
            String bestKey = possibleKeys.get(0);
            for (String key : possibleKeys) {
                if (englishSimilarity(text, key, keyLengthFrequency) > englishSimilarity(text, bestKey, keyLengthFrequency)) {
                    bestKey = key;
                }
            }
            System.out.println(bestKey);
        }
        catch (IOException e) {
            System.out.println("File does not exist: " + filePath);
            return;
        }
        
    }

    static HashMap<Integer,Integer> keyLengthFinder(String text) {
        HashMap<Integer, Integer> keyLengthFrequency = new HashMap<>(); // Map to count frequency of each key length in our GCD calculations
        HashMap<String, ArrayList<Integer>> trigramPositions = new HashMap<>(); // Map for all trigrams and their positions. Most common trigrams will be used.
        

        // Find all trigrams and differences in positions in text
        for (int i = 0; i < text.length() - 2; i++) {
            String trigram = text.substring(i, i + 3);
            trigramPositions.putIfAbsent(trigram, new ArrayList<>());
            trigramPositions.get(trigram).add(i);
        }

        HashMap<String, Integer> trigramFrequency = new HashMap<>();
        for (String trigram : trigramPositions.keySet()) {
            trigramFrequency.put(trigram, trigramPositions.get(trigram).size());
        }

        HashMap<String, ArrayList<Integer>> frequentTrigramPositions = new HashMap<>();
        for (String trigram : trigramPositions.keySet()) {
            frequentTrigramPositions.put(trigram, trigramPositions.get(trigram));
        }

        // Uses 10 trigrams with most appearances in text to find likely key lengths w/ GCD of pairwise position differences
        for (String trigram : frequentTrigramPositions.keySet()) {
            ArrayList<Integer> positions = frequentTrigramPositions.get(trigram);

            for (int i = 0; i < positions.size(); i++) {
                for (int j = i + 1; j < positions.size(); j++) {
                    int distance = positions.get(j) - positions.get(i);

                    for (int k = 2; k <= 16; k++) {
                        if (distance % k == 0) {
                            keyLengthFrequency.put(
                                k,
                                keyLengthFrequency.getOrDefault(k, 0) + 1
                            );
                        }
                    }
                }
            }
        }
        // Weight frequencies by multiplying by sqrt(keylength) (b/c more gcd false positives for smaller key lengths, and by key length alone would make multiples always override their factors)
        for (Map.Entry<Integer, Integer> entry : keyLengthFrequency.entrySet()) {
            keyLengthFrequency.put(entry.getKey(), (int) (entry.getValue() * Math.sqrt(entry.getKey())));
        }

        // Sort key lengths by frequency and return the top 4 most likely key lengths
        keyLengthFrequency = keyLengthFrequency.entrySet().stream()
            .sorted(Map.Entry.<Integer, Integer>comparingByValue().reversed())
            .limit(4)
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (e1, e2) -> e1,
                LinkedHashMap::new
            ));
        
        return keyLengthFrequency;
    }

    static String keyFinder(String text, int keyLength) { 
        // common letters: E, T, A, O, I, N, S, H, R, D, L, C, U, M, W, F, G, Y, P, B, V, K, J, X, Q, Z
        char[] common = {'E', 'T', 'A', 'O', 'I'}; // try 5 most common
        StringBuilder key = new StringBuilder(); // can change
        
        for (int i = 0; i < keyLength; i++) {
            StringBuilder group = new StringBuilder(); 
            for (int j = i; j < text.length(); j += keyLength) {
                group.append(text.charAt(j)); 
            } 

            int[] freq = new int[26]; 
            for (int j = 0; j < group.length(); j ++) {
                freq[group.charAt(j) - 'A'] ++; 
            } 
            
            int[] top = new int[5];  
            for (int t = 0; t < 5; t ++) { 
                int maxIdx = -1; 
                for (int k = 0; k < 26; k ++) {
                    if (maxIdx == -1 || freq[k] > freq[maxIdx]) {
                        maxIdx = k;
                    }
                }
                top[t] = maxIdx; 
                freq[maxIdx] = -1; 
            } 

            int bestShift = 0; // going to see which shift matches 5 most common letters best
            double bestScore = Double.MAX_VALUE; 

            for (int c = 0; c < 5; c ++) {
                for (int p = 0; p < 5; p ++) {
                    int shift = (top[c] - (common[p] - 'A') + 26) % 26;
                    int[] testFreq = new int[26];
                
                    for (int j = 0; j < group.length(); j ++) { // decrypt each one to check
                        int ch = group.charAt(j) - 'A';
                        int dec = (ch - shift + 26) % 26;
                        testFreq[dec] ++;
                    }

                    double score = 0;
                    int total = group.length();
                    for (int k = 0; k < 26; k ++) { // score the result
                        double observed = (double) testFreq[k] / total;
                        if (k == ('E' - 'A')) {
                            score -= observed;
                        }
                    } 

                    if (score < bestScore) { // replace current score with best shift score
                        bestScore = score; 
                        bestShift = shift; 
                    } 
                } 
            } 
            key.append((char) ('A' + bestShift)); 
        }
        return key.toString();
    }

    public static int englishSimilarity(String text, String key, HashMap<Integer, Integer> keyLengthFrequency) {
        int bigramScore = 0;
        int trigramScore = 0;
        String decryptedText = decrypt(text, key);
        for (int i = 0; i < decryptedText.length() - 1; i++) {
            String bigram = decryptedText.substring(i, i + 2);
            if (bigram.equals("TH")) bigramScore++;
            else if (bigram.equals("HE")) bigramScore++;
            else if (bigram.equals("IN")) bigramScore++;
            else if (bigram.equals("ER")) bigramScore++;
            else if (bigram.equals("AN")) bigramScore++;
        }
        for (int i = 0; i < decryptedText.length() - 2; i++) {
            String trigram = decryptedText.substring(i, i + 3);
            if (trigram.equals("THE")) trigramScore++;
            else if (trigram.equals("AND")) trigramScore++;
            else if (trigram.equals("ING")) trigramScore++;
            else if (trigram.equals("HER")) trigramScore++;
            else if (trigram.equals("HIS")) trigramScore++;
        }
        return bigramScore * trigramScore; // weight by frequency of key length from keyLengthFinder
    }

    // Helper functions below:

    // GCD function for finding likely key lengths
    static int gcd(int a, int b) {
        if (b == 0) {
            return a;
        }
        return gcd(b, a % b);
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
