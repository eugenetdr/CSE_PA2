import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import javax.crypto.Cipher;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Arrays;
import java.security.*;
import javax.crypto.*;
import java.lang.*; 
import java.io.*; 
import java.util.*;

public class ClientCP2 {

	public static DataOutputStream toServer = null;
    public static DataInputStream fromServer = null;

	public static FileInputStream fileInputStream = null;
    public static BufferedInputStream bufferedFileInputStream = null;
    public static Socket clientSocket = null;

	public static long timeStarted = System.nanoTime();

	public static int numBytes = 0;

	public static void shellListDir(){
		ProcessBuilder processBuilder = new ProcessBuilder();
		processBuilder.command("ls");
		try {
			Process p = processBuilder.start(); 
			StringBuilder output = new StringBuilder(); 
	     
	        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
	        String line;
			while ((line = reader.readLine()) != null) {
			 	output.append(line + "\n");
			}
	        int exitVal = p.waitFor();
	       	if (exitVal == 0) {
				System.out.println("Success!");
				System.out.println(output);
		    } else {
			    //abnormal...
			    System.out.println("Error has occurred while listing directory, please try again");
		    }
		} catch (Exception e){
			System.out.println("Error found in DeleteFile");
			e.printStackTrace();
		}
	}

	public static void shellUploadFile(String filename){
		System.out.println("Sending file...");

		try {
			// Send the filename
			toServer.writeInt(0);
			toServer.writeInt(filename.getBytes().length);
			toServer.write(filename.getBytes());
			toServer.flush();

			// Generate AES encryption key
	    	KeyGenerator keyGen = KeyGenerator.getInstance("AES");
	    	SecretKey aesKey = keyGen.generateKey();

			// Send Encrypted AES key

			PublicKey key = AuthenticationProtocol.getPubKey("../cacse.crt", "../server.crt");

	        if (key != null) {

	        	Cipher rsaCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
		        rsaCipher.init(Cipher.ENCRYPT_MODE, key);

		        byte[] encryptedKey = rsaCipher.doFinal(aesKey.getEncoded());

		        toServer.writeInt(2);
				toServer.writeInt(encryptedKey.length);
				toServer.write(encryptedKey);
				toServer.flush();

		    }

			// Open the file
			fileInputStream = new FileInputStream(filename);
			bufferedFileInputStream = new BufferedInputStream(fileInputStream);

	        byte [] fromFileBuffer = new byte[117];
	        
	        // Set up encryption
	        // PublicKey key = AuthenticationProtocol.getPubKey("../cacse.crt", "../server.crt");
	        if (key != null) {
	        	Cipher aesCipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
	    		aesCipher.init(Cipher.ENCRYPT_MODE, aesKey);

			    // Send the file
			    for (boolean fileEnded = false; !fileEnded;) {
					numBytes = bufferedFileInputStream.read(fromFileBuffer);
					fileEnded = numBytes < 117;
					
					fromFileBuffer = Arrays.copyOfRange(fromFileBuffer, 0, numBytes);

					//Encrypt Line
			        byte[] encrypted = aesCipher.doFinal(fromFileBuffer);
			        int encryptedNumBytes = encrypted.length;
					
					toServer.writeInt(1);
					toServer.writeInt(encryptedNumBytes);
					toServer.write(encrypted);
					toServer.flush();
				}
	        }
			long timeTaken = System.nanoTime() - timeStarted;
			System.out.println("Program took: " + timeTaken/1000000.0 + "ms to run");

	        bufferedFileInputStream.close();
	        fileInputStream.close();
	    } catch (Exception e){
	    	e.printStackTrace();
	    }
	}
	// public static void shellDownloadFile(){ //Stupid to have download :D wake up your idea pls.

	// }
	public static void shellDeleteFile(String filename){
		ProcessBuilder processBuilder = new ProcessBuilder();
		processBuilder.command("rm",filename);
		try {
			Process p = processBuilder.start(); 
			StringBuilder output = new StringBuilder(); 
	        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
	        String line;
			while ((line = reader.readLine()) != null) {
			 	output.append(line + "\n");
			}
	        int exitVal = p.waitFor();
	       	if (exitVal == 0) {
				System.out.println("Success!");
				System.out.println(output);
		    } else {
			    //abnormal...
			    System.out.println("File cannot be deleted, please try again");
		    }
		} catch (Exception e){
			System.out.println("Error found in DeleteFile");
			e.printStackTrace();
		}
	}

	public static int shellExecuteInput(String line){
		String[] words = line.split("\\s");
		if (words.length > 2){
			System.out.println("Invalid command, please try: EXIT, UPLD <FILENAME>, DWNLD <FILENAME>, DEL <FILENAME> or LISTDIR");
			return 1;
		}
		//:D //LISTDIR 
		else if (line.compareTo("LISTDIR") == 0){
			System.out.println("Listing directory");
			shellListDir();
			return 1;
		}
		//:D //EXIT -program took how long to run, method used was aes1
		else if (line.compareTo("EXIT") == 0){
			//kill
			try {
				toServer.writeInt(4);
		        System.out.println("Closing connection...");
				System.out.println("Exiting now");
				System.exit(0);
			} catch (Exception e) {
				System.out.println("Exception occurred");
				System.out.println("Exiting now");
				System.exit(0);

			}
		}
		//UPLD (filename)
		else if (words[0].compareTo("UPLD") == 0){
			System.out.println("Uploading mode");
			shellUploadFile(words[1]);
			return 1; 
		}
		//DWNLD (filename)
		else if (words[0].compareTo("DWNLD") == 0){
			System.out.println("Downloading mode");
			// shellDownloadFile();
			return 1;
		}
		//:D DEL (filename) DEL non-existant file
		else if (words[0].compareTo("DEL") == 0){
			System.out.println("Deleting mode");
			shellDeleteFile(words[1]);
			return 1; 
		}
		else {
			//invalid commands
			System.out.println("Invalid command, please try: EXIT, UPLD <FILENAME>, DWNLD <FILENAME>, DEL <FILENAME> or LISTDIR");
			return 1; 
		}
		return 1;
	}

	public static void shellLoop(){
		String args;
		int status;
		Scanner sc= new Scanner(System.in);
		System.out.print(">>CSESHELL>> ");
		String line= sc.nextLine();
		status = shellExecuteInput(line); 
		if (status == 1){
			shellLoop();
		}
	}

	public static void main(String[] args) {

		System.out.println("Shell has started...");
    	String filename = "100.txt";
    	if (args.length > 0) filename = args[0];

    	String serverAddress = "localhost";
    	if (args.length > 1) filename = args[1];

    	int port = 4321;
    	if (args.length > 2) port = Integer.parseInt(args[2]);

		try {

			System.out.println("Establishing connection to server...");

			// Connect to server and get the input and output streams
			clientSocket = new Socket(serverAddress, port);
			toServer = new DataOutputStream(clientSocket.getOutputStream());
			fromServer = new DataInputStream(clientSocket.getInputStream());

			shellLoop();
	        

			System.out.println("Closing connection...");

		} catch (Exception e) {e.printStackTrace();}

		long timeTaken = System.nanoTime() - timeStarted;
		System.out.println("Program took: " + timeTaken/1000000.0 + "ms to run");
	}
}
