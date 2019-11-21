package org.example.liao;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.SignatureException;
import java.security.KeyStore.Entry;
import java.security.KeyStore.ProtectionParameter;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Enumeration;

import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class GenFakedCert {
  
	private static final String CERT_FILE = "target/user.cer";
	private static final String P12_FILE = "target/user.p12";
	private static final String JKS_FILE = "target/user.jks";
	private static final String ALIAS = "myalias";
	private static char[] PWD = "12345".toCharArray();
	
	//The first one issues the user cert.
	X509Certificate[] cacerts;
	int keysize;
	PrivateKey privKey;
	
	FakedX509V3CertGenerator certGen;

	public static void main(String[] args) {
		String cacert = 
			// SHA1WithRSA, e=3, 1024 bit
			//"certwithExp3/1024/SHA1/BuiltinObjectToken:DigitalSignatureTrustCo.GlobalCA1"; 
			// SHA1WithRSA, e=3, 2048 bit
			"data/certwithExp3/2048/ChambersofCommerceRoot";
		
			
		args = new String[]{
			cacert,
			"1",
			"cn=Bill Gates, o=Microsoft, c=US, e=bill.gates@microsoft.com"};
		
		
		if(args == null || args.length < 3){
			System.err.println("Usage: GenFakedCert cacertfile beginSN subject");
			System.err.println("       An example of subject is");
			System.err.println("       cn=Alice Mustermann, o=Example, c=DE, e=alice.mustermann@example.org");
			return;
		}
		
	    try{		
		   String ca = args[0];
		   BigInteger beginSN = new BigInteger(args[1]);
		   String subject = args[2];
		
		   Security.addProvider(new BouncyCastleProvider());
		   GenFakedCert gen = new GenFakedCert(ca, beginSN, subject);
		   gen.generate();
	    }catch(Exception e){
		   e.printStackTrace();
        }
	}
	
	public  GenFakedCert(
			String caCertFile,
			BigInteger beginSN,
			String subject) 
	throws Exception{
		this(new X509Certificate[]{loadCert(caCertFile)}, 
				beginSN, subject, 1024, 730);
	}

	public  GenFakedCert(
			X509Certificate cacert,
			BigInteger beginSN,
			String subject) 
	throws NoSuchAlgorithmException, NoSuchProviderException{
		this(new X509Certificate[]{cacert}, 
				beginSN, subject, 1024, 730);
	}
	
	public GenFakedCert(
			X509Certificate[] cacerts,
			BigInteger beginSN,
			String subject,			
			int keysize, 
			int validDays) 
	throws NoSuchAlgorithmException, NoSuchProviderException
	{
		this.cacerts = cacerts;
		this.keysize = keysize;

		certGen = new FakedX509V3CertGenerator(cacerts[0], beginSN);
		certGen.setSubjectDN(new X509Name(subject));
		
		Date notBefore = new Date();
		Date notAfter = new Date(notBefore.getTime() + validDays * 24L * 3600 * 1000);
		certGen.setNotBefore(notBefore);
		certGen.setNotAfter(notAfter);
		
		certGen.setSignatureAlgorithm("SHA1WithRSA");		
		
		KeyPairGenerator kpgen = KeyPairGenerator.getInstance("RSA", "BC");
		kpgen.initialize(keysize);
		KeyPair kp = kpgen.genKeyPair();
		
		certGen.setPublicKey(kp.getPublic());
		privKey = kp.getPrivate();
	}
			
	public void generate()
	throws Exception
	{
        X509Certificate cert = certGen.generate();

        try {
          cert.verify(cacerts[0].getPublicKey(), "BC");
          System.out.println("Signature of certificate is valid");
        } catch (SignatureException ex) {
          throw new Exception("Signature of certificate is NOT valid");
        }
        
		//------------------------------------
		// Store in the keystore
        // initializing and clearing keystore 
        KeyStore ks = KeyStore.getInstance("JKS", "SUN");
        ks.load( null , PWD);
        
        ks.store(new FileOutputStream ( JKS_FILE ), PWD);
        ks.load(new FileInputStream ( JKS_FILE ), PWD);
        
        // storing keystore
        Certificate[] certs = new Certificate[cacerts.length + 1];
        certs[0] = cert;
        System.arraycopy(cacerts, 0, certs, 1, cacerts.length);
        
        ks.setKeyEntry(ALIAS, privKey, PWD, certs);
        ks.store(new FileOutputStream ( JKS_FILE ), PWD);

        System.out.println("Created JKS keystore file " + JKS_FILE);
        
        // Convert the Keystore to P12 Format
        jks2Pkcs12();
        System.out.println("Created PKCS#12 keystore file " + P12_FILE);

        exportCert();
        System.out.println("Created certificate file " + CERT_FILE);
	}

	private Certificate exportCert()
	throws Exception
	{		
		KeyStore ks = KeyStore.getInstance("JKS", "SUN");
		ks.load(new FileInputStream(JKS_FILE), PWD);
		Certificate cert = ks.getCertificate(ALIAS);
		
        // Get the encoded form which is suitable for exporting
        byte[] buf = cert.getEncoded();
        File file = new File(CERT_FILE);
        FileOutputStream os = new FileOutputStream(file);
        os.write(buf);
        os.close();

		return cert;
	}
	
	private void jks2Pkcs12()
	throws Exception
	{		
		KeyStore jks = KeyStore.getInstance("JKS");
		jks.load(new FileInputStream(JKS_FILE), PWD);
		Enumeration<String> aliases = jks.aliases();
		
		KeyStore pks = KeyStore.getInstance("PKCS12", "BC");
		pks.load( null , PWD);		
		pks.store(new FileOutputStream ( P12_FILE), PWD);
        pks.load(new FileInputStream ( P12_FILE), PWD);

        while(aliases.hasMoreElements()){
        	String alias = (String) aliases.nextElement();

        	ProtectionParameter protect = null;
        	if(jks.isKeyEntry(alias))
        		protect = new KeyStore.PasswordProtection(PWD);
        	Entry entry = jks.getEntry(alias, protect);
        	pks.setEntry(alias, entry, protect);
        }
        
        OutputStream os = new FileOutputStream ( P12_FILE ); 
        pks.store(os, PWD);
        os.close();        
	}

	public static X509Certificate loadCert(String certFile) throws Exception
	{
		//Security.addProvider(new BouncyCastleProvider());
        CertificateFactory fact = CertificateFactory.getInstance("X509");
    	
       	return (X509Certificate)
       	      fact.generateCertificate(new FileInputStream(certFile));
	}
	
}
