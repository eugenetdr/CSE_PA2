import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.*;
import javax.crypto.*;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.spec.SecretKeySpec;
import java.lang.*; 

public class ServerCP2 {

	public static ServerSocket welcomeSocket = null;
	public static Socket connectionSocket = null;
	public static DataOutputStream toClient = null;
	public static DataInputStream fromClient = null;

	public static FileOutputStream fileOutputStream = null;
	public static BufferedOutputStream bufferedFileOutputStream = null;
	public static FileInputStream fileInputStream = null;
    public static BufferedInputStream bufferedFileInputStream = null;

	public static long timeStarted = System.nanoTime();

	public static int numBytes = 0;

	public static void shellDownloadFile(String filename, Key aesKey){
		System.out.println("Sending file...");

		try {
			// Open the file
			fileInputStream = new FileInputStream(filename);
			bufferedFileInputStream = new BufferedInputStream(fileInputStream);

	        byte [] fromFileBuffer = new byte[117];
	        
	        
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
				
				toClient.writeInt(encryptedNumBytes);
				toClient.write(encrypted);
				toClient.flush();
			}

			long timeTaken = System.nanoTime() - timeStarted;
			System.out.println("Program took: " + timeTaken/1000000.0 + "ms to run");
			toClient.writeInt(-1);
	        bufferedFileInputStream.close();
	        fileInputStream.close();
	    } catch (Exception e){
	    	e.printStackTrace();
	    }
	}

	public static StringBuilder shellListDir(){
		ProcessBuilder processBuilder = new ProcessBuilder();
		processBuilder.command("ls");
		StringBuilder output = new StringBuilder(); 
		try {
			Process p = processBuilder.start(); 
	     
	        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
	        String line;
			while ((line = reader.readLine()) != null) {
			 	output.append(line + "\n");
			}
	        int exitVal = p.waitFor();
	       	if (exitVal == 0) {
				System.out.println("Success!");
				return output;
		    } else {
			    //abnormal...
			    System.out.println("Error has occurred while listing directory, please try again");
		    }
		} catch (Exception e){
			System.out.println("Error found in DeleteFile");
			e.printStackTrace();
		}
		return output;
	}

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
		    } else {
			    //abnormal...
			    System.out.println("File cannot be deleted, please try again");
		    }
		} catch (Exception e){
			System.out.println("Error found in DeleteFile");
		}
	}

	public static void main(String[] args) {

    	int port = 4321;
    	if (args.length > 0) port = Integer.parseInt(args[0]);

		try {
			welcomeSocket = new ServerSocket(port);
			connectionSocket = welcomeSocket.accept();
			fromClient = new DataInputStream(connectionSocket.getInputStream());
			toClient = new DataOutputStream(connectionSocket.getOutputStream());
    		SecretKey aesKey = KeyGenerator.getInstance("AES").generateKey();

			while (!connectionSocket.isClosed()) {

				int packetType = fromClient.readInt();

				// If the packet is for transferring the filename
				if (packetType == 0) {

					System.out.println("Receiving file...");

					int numBytes = fromClient.readInt();
					byte [] filename = new byte[numBytes];
					// Must use read fully!
					// See: https://stackoverflow.com/questions/25897627/datainputstream-read-vs-datainputstream-readfully
					fromClient.readFully(filename, 0, numBytes);

					fileOutputStream = new FileOutputStream("recv_"+new String(filename, 0, numBytes));
					bufferedFileOutputStream = new BufferedOutputStream(fileOutputStream);

				// If the packet is for transferring a chunk of the file
				} else if (packetType == 1) {

					int numBytes = fromClient.readInt();
					byte [] block = new byte[numBytes];
					fromClient.readFully(block, 0, numBytes);

				    // Decrypt Line
						
				    Cipher aesCipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
			        aesCipher.init(Cipher.DECRYPT_MODE, aesKey);
			        
			        byte[] decrypted = aesCipher.doFinal(block);

				    if (decrypted.length > 0)
						bufferedFileOutputStream.write(decrypted, 0, decrypted.length);

					if (decrypted.length < 117) {

						if (bufferedFileOutputStream != null) bufferedFileOutputStream.close();
						if (bufferedFileOutputStream != null) fileOutputStream.close();
					}

				} else if (packetType == 2) {
					int keyBytes = fromClient.readInt();
					byte [] encryptedKey = new byte[keyBytes];
					fromClient.readFully(encryptedKey, 0, keyBytes);

				    // Decrypt Key
					PrivateKey pteKey = AuthenticationProtocol.getPteKey("../private_key.der");
				    Cipher rsaCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
			        rsaCipher.init(Cipher.DECRYPT_MODE, pteKey);

				    aesKey = new SecretKeySpec ( rsaCipher.doFinal(encryptedKey), "AES" );

				} else if (packetType == 3) {
					String output = shellListDir().toString();
					String[] toSend = output.split("\n");
					for (String line: toSend) {
						System.out.println(line);
						toClient.writeInt(line.getBytes().length);
						toClient.write(line.getBytes());
					}
					toClient.writeInt(-1);

				} else if (packetType == 4) {
					System.out.println("Closing connection...");
					fromClient.close();
					toClient.close();
					connectionSocket.close();

				} else if (packetType == 5) {
					int filenameBytes = fromClient.readInt();
					byte [] recv = new byte[filenameBytes];
					fromClient.readFully(recv, 0, filenameBytes);
					String filename = new String(recv);

					if (shellListDir().toString().contains(filename)) {
						try {
							toClient.writeInt(5);
							shellDownloadFile(filename, aesKey);
							toClient.writeInt(-1);
						} catch (Exception e) {
							toClient.writeInt(-1);
						}
					}
					else {
						toClient.writeInt(-2);
					}

				} else if (packetType == 6) {
					int filenameBytes = fromClient.readInt();
					byte [] recv = new byte[filenameBytes];
					fromClient.readFully(recv, 0, filenameBytes);
					String filename = new String(recv);
					if (shellListDir().toString().contains(filename)) {
						try {
							shellDeleteFile(filename);
							toClient.writeInt(6);
						} catch (Exception e) {
							System.out.println(e.toString());
							toClient.writeInt(-1);
						}
					}
					else {
						toClient.writeInt(-2);
					}
				}

			}
		} catch (Exception e) {e.printStackTrace();}

	}

}
