import javax.crypto.*;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;
import java.util.Scanner;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class PasswordManager {

    private static Cipher cipher;
    private static String myToken = "kakorrhaphiophobia";//
    private static String filepathString = "storage.txt";

    private static byte[] salt;
    private static SecretKeySpec key;

    public static void main(String[] args) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException,
            IllegalBlockSizeException, BadPaddingException, InvalidKeySpecException, IOException {

        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter the passcode to access your passwords: ");
        String passcode = scanner.nextLine();

        File storage = new File(filepathString);

        // debug code
        // storage.delete();

        salt = new byte[16];
        cipher = Cipher.getInstance("AES");
        key = setKey(passcode); // code breaks without this

        if (!storage.exists()) {
            makeNewFile(passcode);
        } else if (!checkAcess(passcode)) {
            System.out.println("ACCESS DENIED: wrong passcode!");
            System.exit(1);
        }

        String choice = "c";
        do {
            System.out.print("a : Add Password\nr: Read Password\nq : Quit\nEnter choice: ");
            choice = scanner.nextLine();

            if (choice.toLowerCase().charAt(0) == 'q') {
                // case only happens if you quit immediately
                break;
            } else if (choice.toLowerCase().charAt(0) == 'a') {
                // add passcode
                try {
                    //FileWriter writer = new FileWriter(storage, true);
                    System.out.print("Enter label for password: ");
                    String label = scanner.nextLine();

                    // I FORGOT WE NEEDED TO REPLACE ALREADY THERE PASSWORDS. man.
                    Scanner fileReader = new Scanner(new File(filepathString));

                    String[] looker = fileReader.nextLine().split(": ");
                    while (fileReader.hasNextLine() && !label.equals(looker[0])) {
                        looker = fileReader.nextLine().split(": ");
                    }
                    fileReader.close();

                    //FileWriter writer = new FileWriter(storage, true);

                    if (looker[0].equals(label)) {
                        //if it exists, just rewrite the whole dang file.
                        //System.out.println(looker[0]);

                        File orig = new File( filepathString);
                        File temp = new File("temp.txt");
                        temp.delete();
                        temp.createNewFile();

                        BufferedReader in = new BufferedReader(new FileReader(orig));
                        PrintWriter pw = new PrintWriter(new FileWriter(temp)) ;

                        for (String data; (data = in.readLine()) != null; ) {
                            if (!data.split(":")[0].equals(looker[0])) pw.println(data);
                        }

                        in.close();
                        pw.close();

                        //System.out.println( 
                        storage.delete() ;
                        //System.out.println( 
                        temp.renameTo(storage) ;


                    }

                    // String label = scanner.nextLine();
                    FileWriter writer = new FileWriter(storage, true);

                    if (looker[0].equals(label)) {
                        writer.append( label);

                    } else {
                        writer.append("\n" + label);
                    }




                    System.out.print("Enter password to store: ");
                    String password = scanner.nextLine();

                    String encodedString = encode(password);
                    writer.append(": " + encodedString);
                    writer.close();

                    System.out.println();

                } catch (IOException e) {
                    System.out.println("An error occurred.");
                    e.printStackTrace();
                }

            } else if (choice.toLowerCase().charAt(0) == 'r') {
                /// read a preexising password. inefficiaently open a new filereader and scan
                /// all lines every time :(
                // could porbably be refactored into it's own method...
                System.out.print("Enter label for password: ");
                String label = scanner.nextLine();

                Scanner fileReader = new Scanner(new File(filepathString));

                String[] looker = fileReader.nextLine().split(": ");

                while (fileReader.hasNextLine() && !label.equals(looker[0])) {
                    // System.out.println(looker);
                    looker = fileReader.nextLine().split(": ");
                }

                // if label == null: print error, break
                if (!looker[0].equals(label)) {
                    System.out.printf("label %s not found!\n", label);
                } else
                    System.out.printf("Found: %s\n\n", decode(looker[1]));

                fileReader.close();

            } else {
                System.out.println("Please enter a valid choice: ");
            }

        } while (choice.toLowerCase().charAt(0) != 'q');

        scanner.close();
        System.out.println("Quitting");
        System.exit(0);

    }


    /*
     * this method is only called if a file exists.
     *
     * it takes a passcode and checks to see if the existing file was made with that
     * passcode
     * it also initializes the global variable Salt.
     *
     * it returns true if the password matches the password used to encode the
     * token.
     * else, return false.
     */
    private static boolean checkAcess(String passcode) throws FileNotFoundException, InvalidKeyException,
            IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, InvalidKeySpecException {

        Scanner fr = new Scanner(new File(filepathString));

        String[] data = fr.nextLine().split(": ");
        fr.close();

        // set salt
        salt = Base64.getDecoder().decode(data[0]);
        // System.out.println(salt);//debug code

        String tokenCheck = encode(myToken);

        return tokenCheck.equals(data[1]);

    }

    /*
     * Takes passcode and makes a new file.
     * this method is only called when the file doesn't already exist, so there's no
     * need to nest the creatnewfile()
     * it also creates the salt to be stored in the file
     * lastly, it encodes a token with the SALT and the PASSCODE.
     * the salt and token are written into the file.
     *
     */
    private static void makeNewFile(String passcode) throws NoSuchAlgorithmException, NoSuchPaddingException,
            InvalidKeySpecException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, IOException {

        System.out.println("No password file detected. Creating a new password file.");
        File storage = new File(filepathString); // new pointer
        storage.createNewFile();

        // make the salt
        SecureRandom random = new SecureRandom();
        // byte[] sal = new byte[16];
        random.nextBytes(salt);
        String saltString = Base64.getEncoder().encodeToString(salt); // this is what we gotta write in

        // System.out.println(salt);//debug code

        salt = Base64.getDecoder().decode(saltString);

        setKey(passcode);

        cipher.init(Cipher.ENCRYPT_MODE, key);
        String encryptedEncodedToken = encode(myToken);

        FileWriter fw;
        try {
            fw = new FileWriter(filepathString, true);
            // System.out.println(saltString + ": " + encryptedEncodedToken);//debug code
            fw.write(saltString + ": " + encryptedEncodedToken);
            fw.close();

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    /*
     * this method sets the global variable key.
     * it has been abtracted into a method because else it would repeat a few times
     *
     */
    private static SecretKeySpec setKey(String passcode) throws NoSuchAlgorithmException, InvalidKeySpecException {
        KeySpec spec = new PBEKeySpec(passcode.toCharArray(), salt, 600000, 128);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        SecretKey sharedKey = factory.generateSecret(spec);
        return new SecretKeySpec(sharedKey.getEncoded(), "AES");

    }

    // encode adn decode have been quarentined into their own methods to prevent
    // their chain of functions from sitting around in main.
    private static String encode(String message)
            throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException {

        cipher.init(Cipher.ENCRYPT_MODE, key);
        return new String(Base64.getEncoder().encode(cipher.doFinal(message.getBytes())));
    }

    private static String decode(String message)
            throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        cipher.init(Cipher.DECRYPT_MODE, key);

        return new String(cipher.doFinal(Base64.getDecoder().decode(message)));

    }

}