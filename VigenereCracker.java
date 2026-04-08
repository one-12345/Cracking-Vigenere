import java.io.IOException;
import java.nio.file.*;
import java.io.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.Collectors;

class VigenereCracker {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Please enter the filename of the encrypted text as a command line argument.");
            return;
        }

        Path filePath = Paths.get(args[0]);
        String text;
        ArrayList<Integer> likelyKeyLengths = new ArrayList<>();

        try {
            text = Files.readString(filePath).toUpperCase();
            if (text.isEmpty()) {
                System.out.println("File is empty: " + args[1]);
                return;
            }
            text = text.replaceAll("[^A-Z]", ""); // Sanitizes text (in case of something weird)
            likelyKeyLengths = keyLengthFinder(text);
            System.out.println("Likely key lengths: ");
            for (int keyLength : likelyKeyLengths) {
                System.out.println(keyLength);
            }
        }
        catch (IOException e) {
            System.out.println("File does not exist: " + filePath);
            return;
        }
        
    }

    static ArrayList<Integer> keyLengthFinder(String text) {
        ArrayList<Integer> likelyKeyLengths = new ArrayList<>();
        HashMap<Integer, Integer> keyLengthFrequency = new HashMap<>(); // Map to count frequency of each key length in our GCD calculations
        HashMap<String, ArrayList<Integer>> bigramPositions = new HashMap<>(); // Map for all bigrams and their positions. Most common bigrams will be used.
        Pattern pat = Pattern.compile(".."); // Pattern to find bigrams
        Matcher mat = pat.matcher(text);

        // Find all bigrams and differences in positions in text
        while (mat.find()) {
            String bigram = mat.group();
            int pos = mat.start();
            if (!bigramPositions.containsKey(bigram)) {
                bigramPositions.put(bigram, new ArrayList<>());
            }
            else {
                bigramPositions.get(bigram).add(pos - bigramPositions.get(bigram).get(bigramPositions.get(bigram).size() - 1)); // Store position differences for GCD calculation
            }
        }

        // Sort bigrams by frequency and get the 10 most common bigrams

        HashMap<HashMap<String, ArrayList<Integer>>, Integer> bigramFrequency = new HashMap<>();
        String[] topTen = new String[10];
        for (String bigram : bigramPositions.keySet()) {
            bigramFrequency.put(bigramPositions, bigramPositions.get(bigram).size());
            topTen = bigramFrequency.entrySet().stream()
                .sorted(Map.Entry.<HashMap<String, ArrayList<Integer>>, Integer>comparingByValue().reversed())
                .limit(10)
                .map(Map.Entry::getKey)
                .toArray(String[]::new);
        }

        HashMap<String, ArrayList<Integer>> frequentBigramPositions = new HashMap<>();
        for (String bigram : topTen) {
            frequentBigramPositions.put(bigram, bigramPositions.get(bigram));
        }

        // Uses 10 bigrams with most appearances in text to find likely key lengths w/ GCD of pairwise position differences
        for (String bigram : frequentBigramPositions.keySet()) {
            ArrayList<Integer> positions = frequentBigramPositions.get(bigram);
            for (int i = 0; i < positions.size(); i++) {
                for (int j = i + 1; j < positions.size(); j++) {
                    int gcd = gcd(positions.get(i), positions.get(j));
                    if (gcd > 1 && !likelyKeyLengths.contains(gcd) && gcd <= 16) { // Only considers key lengths at most 16 due to assignment parameters
                        likelyKeyLengths.add(gcd);
                        keyLengthFrequency.put(gcd, 1); // Initialize count of how many times this key length appears
                    }
                    else if (gcd > 1) {
                        keyLengthFrequency.put(gcd, keyLengthFrequency.get(gcd) + 1); // Increment count of how many times this key length appears
                    }
                }
            }
        }


        // Returns likely key lengths sorted by frequency of appearance in position differences, but weight towards longer key lengths (since they are more likely to be correct)
        // WIP, currently just returns key lengths sorted by frequency of appearance in position differences
        return likelyKeyLengths;
    }

    static String keyFinder(String text, int keyLength) { 
        // E, T, A, O, I, N, S, H, R, D, L, C, U, M, W, F, G, Y, P, B, V, K, J, X, Q, Z
        char[] common = {'E', 'T', 'A', 'O', 'I'};
        StringBuilder key = new StringBuilder(); 
        
        for (int i = 0; i < keyLength; i++) {
            StringBuilder group = new StringBuilder(); 
            for (int j = i; j < text.length(); j += keyLength) {
                group.append(text.charAt(j)); 
            } 

            int[] freq = new int[26]; 
            for (int j = 0; j < group.length(); j ++) {
                freq[group.charAt(j) - 'A'] ++; 
            } 
            
            int[] top = new int[5];  // try the top 5 most common letters and test those
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

            int bestShift = 0; 
            double bestScore = Double.MAX_VALUE; 

            for (int c = 0; c < 5; c ++) {
                for (int p = 0; p < 5; p ++) {
                    int shift = (top[c] - (common[p] - 'A') + 26) % 26;
                    int[] testFreq = new int[26];
                
                    for (int j = 0; j < group.length(); j ++) {
                        int ch = group.charAt(j) - 'A';
                        int dec = (ch - shift + 26) % 26;
                        testFreq[dec] ++;
                    }

                    double score = 0;
                    int total = group.length();
                    for (int k = 0; k < 26; k ++) {
                        double observed = (double) testFreq[k] / total;
                        if (k == ('E' - 'A')) {
                            score -= observed;
                        }
                    } 

                    if (score < bestScore) { // replace with best shift
                        bestScore = score; 
                        bestShift = shift; 
                    } 
                } 
            } 
            key.append((char) ('A' + bestShift)); 
        }
        return key.toString();
    }

    static int gcd(int a, int b) {
        if (b == 0) {
            return a;
        }
        return gcd(b, a % b);
    }
}
