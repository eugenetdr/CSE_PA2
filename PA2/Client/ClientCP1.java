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


public class ClientCP1 {

	public static DataOutputStream toServer = null;
    public static DataInputStream fromServer = null;

	public static FileInputStream fileInputStream = null;
    public static BufferedInputStream bufferedFileInputStream = null;
    public static Socket clientSocket = null;

	public static long timeStarted = System.nanoTime();

	public static int numBytes = 0;

	public static void shellUploadFile(String filename){
		System.out.println("Sending file...");
		try{
			// Send the filename
			toServer.writeInt(0);
			toServer.writeInt(filename.getBytes().length);
			toServer.write(filename.getBytes());
			toServer.flush();

			// Open the file
			fileInputStream = new FileInputStream(filename);
			bufferedFileInputStream = new BufferedInputStream(fileInputStream);

	        byte [] fromFileBuffer = new byte[117];
	        
	        // Set up encryption
	        PublicKey key = AuthenticationProtocol.getPubKey("../../cacse.crt", "../../server.crt");
	        if (key != null) {
	        	Cipher rsaCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
		        rsaCipher.init(Cipher.ENCRYPT_MODE, key);

			    // Send the file
			    for (boolean fileEnded = false; !fileEnded;) {
					numBytes = bufferedFileInputStream.read(fromFileBuffer);
					fileEnded = numBytes < 117;
					
					fromFileBuffer = Arrays.copyOfRange(fromFileBuffer, 0, numBytes);

					//Encrypt Line
			        byte[] encrypted = rsaCipher.doFinal(fromFileBuffer);
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
	    } catch (Exception e ){
	    	e.printStackTrace();
	    }
	}

	public static int shellExecuteInput(String line){
		String[] words = line.split("\\s");
		if (words.length > 2){
			System.out.println("Invalid command, please try: EXIT, UPLD <FILENAME>, DEL <FILENAME> or LISTDIR");
			return 1;
		}
		//:D //LISTDIR 
		else if (line.compareTo("LISTDIR") == 0){
			System.out.println("Listing directory");
			try {
				toServer.writeInt(2);
				for (boolean fileEnded = false; !fileEnded;) {
					int recvLen = fromServer.readInt();
					fileEnded = recvLen == -1;
					
					if (!fileEnded) {
						byte [] recv = new byte[recvLen];
						fromServer.readFully(recv, 0, recvLen);
						String recvLine = new String(recv);
						System.out.println(recvLine);
					}
				}
			} catch (Exception e) {
				System.out.println("Exception occurred");
				System.out.println("Exiting now");
				System.exit(0);
			}
			return 1;
		}
		//:D //EXIT -program took how long to run, method used was aes1
		else if (line.compareTo("EXIT") == 0){
			//kill
			try {
				toServer.writeInt(3);
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
		//:D DEL (filename) DEL non-existant file
		else if (words[0].compareTo("DEL") == 0){
			System.out.println("Deleting mode");
			try {
				toServer.writeInt(4);
				toServer.writeInt(words[1].getBytes().length);
				toServer.write(words[1].getBytes());
				int status = fromServer.readInt();
				if (status == 4) {
					System.out.println("Delete Successful!");
				}
				else if (status == -1) {
					System.out.println("Delete Unsuccessful!");
				}
				else {
					System.out.println("File does not Exist!");
				}
			} catch (Exception e) {
				System.out.println("Exception occurred");
				System.out.println("Exiting now");
				System.exit(0);
			}
			return 1; 
		}
		else {
			//invalid commands
			System.out.println("Invalid command, please try: EXIT, UPLD <FILENAME>, DEL <FILENAME> or LISTDIR");
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

		System.out.println("Shell has started..");

    	String filename = "100000.txt";
    	if (args.length > 0) {filename = args[0];}

    	String serverAddress = "localhost";
    	if (args.length > 1) {filename = args[1];}

    	int port = 4321;
    	if (args.length > 2) {port = Integer.parseInt(args[2]);}

		System.out.println("Establishing connection to server...");
		try {
			// Connect to server and get the input and output streams
			clientSocket = new Socket(serverAddress, port);
			toServer = new DataOutputStream(clientSocket.getOutputStream());
			fromServer = new DataInputStream(clientSocket.getInputStream());

			shellLoop();


			System.out.println("Closing connection...");

	    } catch (Exception e){
	    	e.printStackTrace();
	    }


		long timeTaken = System.nanoTime() - timeStarted;
		System.out.println("Program took: " + timeTaken/1000000.0 + "ms to run");
	}
}
