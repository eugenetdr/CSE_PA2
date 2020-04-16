import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.net.Socket;
import java.util.Arrays;
import java.security.*;
import javax.crypto.Cipher;

public class ClientCP1 {

	public static void main(String[] args) {

    	String filename = "100000.txt";
    	if (args.length > 0) filename = args[0];

    	String serverAddress = "localhost";
    	if (args.length > 1) filename = args[1];

    	int port = 4321;
    	if (args.length > 2) port = Integer.parseInt(args[2]);

		int numBytes = 0;

		Socket clientSocket = null;

        DataOutputStream toServer = null;
        DataInputStream fromServer = null;

    	FileInputStream fileInputStream = null;
        BufferedInputStream bufferedFileInputStream = null;

		long timeStarted = System.nanoTime();

		try {

			System.out.println("Establishing connection to server...");

			// Connect to server and get the input and output streams
			clientSocket = new Socket(serverAddress, port);
			toServer = new DataOutputStream(clientSocket.getOutputStream());
			fromServer = new DataInputStream(clientSocket.getInputStream());

			System.out.println("Sending file...");

			// Send the filename
			toServer.writeInt(0);
			toServer.writeInt(filename.getBytes().length);
			toServer.write(filename.getBytes());
			//toServer.flush();

			// Open the file
			fileInputStream = new FileInputStream(filename);
			bufferedFileInputStream = new BufferedInputStream(fileInputStream);

	        byte [] fromFileBuffer = new byte[117];
	        
	        // Set up encryption
	        PublicKey key = AuthenticationProtocol.getPubKey("../cacse.crt", "../server.crt");
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
	        

	        bufferedFileInputStream.close();
	        fileInputStream.close();

			System.out.println("Closing connection...");

		} catch (Exception e) {e.printStackTrace();}

		long timeTaken = System.nanoTime() - timeStarted;
		System.out.println("Program took: " + timeTaken/1000000.0 + "ms to run");
	}
}
