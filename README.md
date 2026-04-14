# Cracking-Vigenere

Use the following instructions to build needed files to run the program:

javac Vigenere.java
javac VigenereCracker.java
javac v2.java

If you need to encrypt text, use the Vigenere.java program. For instance, to write encrypted text from a file such as alice.txt into a file called encryptedalice.txt, use:

java Vigenere -e alice.txt INSERTKEYHERE > encryptedalice.txt
java Vigenere -e alice.txt RCCCAHYABQWFC > encryptedalice.txt

To find the key from a text file such as encryptedalice.txt containing text encrypted by the Vigenere cipher, use:

java v2 encryptedalice.txt
java VigenereCracker encryptedalice.txt

java v2 shortencryptedalice.txt
java VigenereCracker shortencryptedalice.txt