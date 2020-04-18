import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.*;
import javax.crypto.*;
import java.util.Base64;
import javax.crypto.spec.SecretKeySpec;

public class ServerCP2 {

	public static void main(String[] args) {

    	int port = 4321;
    	if (args.length > 0) port = Integer.parseInt(args[0]);

		ServerSocket welcomeSocket = null;
		Socket connectionSocket = null;
		DataOutputStream toClient = null;
		DataInputStream fromClient = null;

		FileOutputStream fileOutputStream = null;
		BufferedOutputStream bufferedFileOutputStream = null;



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
					continue;
				} else if (packetType == 4) {
					System.out.println("Closing connection...");
					fromClient.close();
					toClient.close();
					connectionSocket.close();
				}

			}
		} catch (Exception e) {e.printStackTrace();}

	}

}
