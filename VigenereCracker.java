import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

class VigenereCracker {
    static Object[][] letterFrequency = {{'A',8.17},{'B',1.49},{'C',2.78},{'D',4.25},{'E',12.70},{'F',2.23},{'G',2.02},{'H',6.09},{'I',6.97},{'J',0.15},{'K',0.77},{'L',4.03},{'M',2.41},{'N',6.75},{'O',7.51},{'P',1.93},{'Q',0.10},{'R',5.99},{'S',6.33},{'T',9.06},{'U',2.76},{'V',0.98},{'W',2.36},{'X',0.15},{'Y',1.97},{'Z',0.07}};
    static Object[][] bigramFrequency = {{"TH",3.56},{"HE",3.07},{"IN",1.92},{"ER",2.77},{"AN",1.73},{"RE",2.41},{"ED",2.67},{"ND",1.68},{"HA",1.16},{"EN",1.44}};
    static Object[][] trigramFrequency = {{"THE",1.81},{"AND",0.73},{"THA",0.33},{"ENT",0.42},{"INT",0.72},{"ION",0.42},{"FOR",0.34},{"tio",0.31}};

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
                System.out.println("File is empty: " + filePath);
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
                // System.out.println(key);
                // System.out.println(englishSimilarity(text, key));
                if (englishSimilarity(text, key) > englishSimilarity(text, bestKey)) {
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
        HashMap<String, ArrayList<Integer>> bigramPositions = new HashMap<>(); // Map for all trigrams and their positions. Most common trigrams will be used.
        

        // Find all bigrams and differences in positions in text
        for (int i = 0; i < text.length() - 1; i++) {
            String bigram = text.substring(i, i + 2);
            bigramPositions.putIfAbsent(bigram, new ArrayList<>());
            bigramPositions.get(bigram).add(i);
        }

        HashMap<String, Integer> bigramFrequency = new HashMap<>();
        for (String bigram : bigramPositions.keySet()) {
            bigramFrequency.put(bigram, bigramPositions.get(bigram).size());
        }

        //TODO: sort and use the bigramFrequency
        bigramFrequency=bigramFrequency.entrySet().stream()
            .sorted(Map.Entry.<String,Integer>comparingByValue().reversed())
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (e1,e2)->e1,
                LinkedHashMap::new
            ));

        HashMap<String, ArrayList<Integer>> frequentBigramPositions = new HashMap<>();
        int cnt=0;
        for (String bigram : bigramFrequency.keySet()) {
            cnt++;
            frequentBigramPositions.put(bigram, bigramPositions.get(bigram));
            if(cnt==10) break;
        }

        // Uses 10 bigrams with most appearances in text to find likely key lengths w/ GCD of pairwise position differences
        ArrayList<Integer> distances = new ArrayList<>();
        for (String bigram : frequentBigramPositions.keySet()) {
            ArrayList<Integer> positions = frequentBigramPositions.get(bigram);

            for (int i = 0; i < positions.size(); i++) {
                for (int j = i + 1; j < positions.size(); j++) {
                    int distance = positions.get(j) - positions.get(i);
                    distances.add(distance);
                }
            }
        }

        // Perform pairwise GCD on collected distances and count divisors
        HashMap<Integer, Integer> divisorCounts = new HashMap<>();
        if(text.length()>1500){
            for (int i = 0; i < distances.size(); i++) {
                    for (int k = 1; k <=16; k++) {
                        if (distances.get(i) % k == 0) {//in reality, you don't know if there's a specific upperbound to the key.
                            divisorCounts.put(k, divisorCounts.getOrDefault(k, 0) + 1);
                        }
                    }
            }
        } else {
            for (int i = 0; i < distances.size(); i++) {
                for (int j = i + 1; j < distances.size(); j++) {
                    int gcdValue = gcd(distances.get(i), distances.get(j));
                    
                    // Count all divisors of the GCD
                    for (int k = 1; k <= gcdValue; k++) {
                        if (gcdValue % k == 0 && k<=16) {//in reality, you don't know if there's a specific upperbound to the key.
                            divisorCounts.put(k, divisorCounts.getOrDefault(k, 0) + 1);
                        }
                    }
                }
            }
        }
        //pairwise gcd is for demonstration; the original method is the total gcd for all distances
        //However, repeating bigrams inside the key and unintended bigrams in cipher can cause great issues
        
        // Transfer divisor counts to keyLengthFrequency
        for (Map.Entry<Integer, Integer> entry : divisorCounts.entrySet()) {
            keyLengthFrequency.put(
                entry.getKey(),
                keyLengthFrequency.getOrDefault(entry.getKey(), 0) + entry.getValue()
            );
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
            //TODO: where is the standard english frequency for comparison?
            int bestShift = 0; // going to see which shift matches 5 most common letters best
            double bestScore = -Double.MAX_VALUE; 
            for (int c = 0; c < 5; c ++) {
                    int shift = (top[c] - ('E' - 'A') + 26) % 26;
                    int[] testFreq = new int[26];
                
                    for (int j = 0; j < group.length(); j ++) { // decrypt each one to check
                        int ch = group.charAt(j) - 'A';
                        int dec = (ch - shift + 26) % 26;
                        testFreq[dec] ++;
                    }

                    double score = 0;
                    int total = group.length();
                    for (int k = 0; k < 26; k ++) { // score the result
                        double observed = (double) testFreq[k] / total * 100;
                        score-=((double)letterFrequency[k][1]-observed)*((double)letterFrequency[k][1]-observed);
                        // System.out.print(" "+k+" "+observed);
                    } 
                    if (score > bestScore) { // replace current score with best shift score
                        bestScore = score; 
                        bestShift = shift; 
                        // System.out.println(" "+shift+" ");
                    // System.out.println(score);
                } 
            } 
            key.append((char) ('A' + bestShift)); 
        }
        return key.toString();
    }

    public static double englishSimilarity(String text, String key) {
        String decryptedText = decrypt(text, key);
        
        // Count bigram frequencies in decrypted text
        HashMap<String, Integer> decryptedBigramFreq = new HashMap<>();
        for (int i = 0; i < decryptedText.length() - 1; i++) {
            String bigram = decryptedText.substring(i, i + 2);
            decryptedBigramFreq.put(bigram, decryptedBigramFreq.getOrDefault(bigram, 0) + 1);
        }
        
        // Count trigram frequencies in decrypted text
        HashMap<String, Integer> decryptedTrigramFreq = new HashMap<>();
        for (int i = 0; i < decryptedText.length() - 2; i++) {
            String trigram = decryptedText.substring(i, i + 3);
            decryptedTrigramFreq.put(trigram, decryptedTrigramFreq.getOrDefault(trigram, 0) + 1);
        }
        
        // Calculate bigram score based on frequency difference squared
        double bigramScore = 0;
        int totalBigrams = decryptedText.length() - 1;
        for (Object[] bigramPair : bigramFrequency) {
            String bigram = (String) bigramPair[0];
            if (decryptedBigramFreq.containsKey(bigram)) {
                double expectedFreq = ((double) bigramPair[1] / 100.0);
                double observedFreq = (double) decryptedBigramFreq.get(bigram) / totalBigrams;
                bigramScore += (expectedFreq - observedFreq) * (expectedFreq - observedFreq);
            }
        }
        
        // Calculate trigram score based on frequency difference squared
        double trigramScore = 0;
        int totalTrigrams = decryptedText.length() - 2;
        for (Object[] trigramPair : trigramFrequency) {
            String trigram = (String) trigramPair[0];
            if (decryptedTrigramFreq.containsKey(trigram)) {
                double expectedFreq = ((double) trigramPair[1] / 100.0);
                double observedFreq = (double) decryptedTrigramFreq.get(trigram) / totalTrigrams;
                trigramScore += (expectedFreq - observedFreq) * (expectedFreq - observedFreq);
            }
        }
        
        // Return combined score (negative because lower is better)
        return (double) (-(bigramScore + trigramScore));
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
