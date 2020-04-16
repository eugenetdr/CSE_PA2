import java.io.*;
import java.nio.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.*;


public class AuthenticationProtocol {
	public static PrivateKey getPteKey(String filename) throws Exception {
	
		byte[] keyBytes = Files.readAllBytes(Paths.get(filename));
	 
		PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
		KeyFactory kf = KeyFactory.getInstance("RSA");
		
		return kf.generatePrivate(spec);
		
	  }

	public static PublicKey getPubKey(String caCertFilename, String serverCertFilename) throws Exception {
		
		PublicKey SERVERpubkey = null;
		
		// Extract public key from CAcert
		InputStream fis = new FileInputStream(caCertFilename);
		CertificateFactory cf = CertificateFactory.getInstance("X.509");
		X509Certificate CAcert =(X509Certificate)cf.generateCertificate(fis);
		PublicKey CApubkey = CAcert.getPublicKey();
		
		try {
			
			// Extract Server cert
			InputStream serverFis = new FileInputStream(serverCertFilename);
			CertificateFactory SERVERcf = CertificateFactory.getInstance("X.509");
			X509Certificate SERVERcert =(X509Certificate)cf.generateCertificate(serverFis);
		
			// Checking of CA public key with Server cert
			SERVERcert.checkValidity();
			SERVERcert.verify(CApubkey);
			
			// Extract Server public key if check passes
			SERVERpubkey = SERVERcert.getPublicKey();
			
		} catch (InvalidKeyException ek) {
			System.out.println("Invalid Key. Closing Connection.");
		} catch (Exception e) {
			System.out.println("Error Occurred. Closing Connection.");
		}
		
		return SERVERpubkey;

	  }
	


}
