package net.fkmclane.mineproxy;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.File;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyManagementException;
import java.security.KeyPair;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Date;
import java.util.Random;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;


import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x509.SubjectKeyIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.bc.BcX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

public class CertGen {
	static {
		Security.addProvider(new BouncyCastleProvider());
	}

	private X509Certificate ca_cert;
	private PrivateKey ca_key;

	private KeyPairGenerator gen;
	private SecureRandom rand;

	public CertGen(File ca_cert_file, File ca_key_file) throws Exception {
		// initialize generators
		gen = KeyPairGenerator.getInstance("RSA");
		rand = SecureRandom.getInstance("SHA1PRNG");
		gen.initialize(2048, rand);

		// check and load key file if already generated
		if (ca_key_file.exists())
			loadCertificateAuthority(ca_cert_file, ca_key_file);
		else
			generateCertificateAuthority(ca_cert_file, ca_key_file);
	}

	public void loadCertificateAuthority(File ca_cert_file, File ca_key_file) throws Exception {
		// read certificate from file
		ca_cert = (X509Certificate)CertificateFactory.getInstance("X.509").generateCertificate(new FileInputStream(ca_cert_file));

		// read key data from file
		byte[] key_data = new byte[(int)ca_key_file.length()];
		InputStream fis = new FileInputStream(ca_key_file);
		fis.read(key_data);
		fis.close();

		// generate key object from key data
		ca_key = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(key_data));
	}

	public KeyStore generateCertificateAuthority(File ca_cert_file, File ca_key_file) throws Exception {
		// inspired by LittleProxy-mitm

		// generate a new set of keys
		KeyPair keys = gen.generateKeyPair();

		// add name details
		X500NameBuilder name = new X500NameBuilder(BCStyle.INSTANCE);
		name.addRDN(BCStyle.CN, "MineProxy");
		name.addRDN(BCStyle.O, "MineProxy");
		name.addRDN(BCStyle.OU, "MineProxy");

		// add certificate details
		X500Name issuer = name.build();
		BigInteger serial = new BigInteger(64, rand).abs();
		X500Name subject = issuer;
		PublicKey pub = keys.getPublic();

		// create builder for certificate
		X509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(issuer, serial, new Date(System.currentTimeMillis() - 365*24*60*60*1000), new Date(System.currentTimeMillis() + 365*24*60*60*1000), subject, pub);

		// generate public key info
		ByteArrayInputStream bain = new ByteArrayInputStream(keys.getPublic().getEncoded());
		ASN1InputStream ais = new ASN1InputStream(bain);
		ASN1Sequence seq = (ASN1Sequence)ais.readObject();
		SubjectPublicKeyInfo info = new SubjectPublicKeyInfo(seq);

		// add public key info and basic constraints to certificate
		builder.addExtension(Extension.subjectKeyIdentifier, false, new BcX509ExtensionUtils().createSubjectKeyIdentifier(info));
		builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(true));

		// add certificate usage
		KeyUsage usage = new KeyUsage(KeyUsage.keyCertSign | KeyUsage.digitalSignature | KeyUsage.keyEncipherment | KeyUsage.dataEncipherment | KeyUsage.cRLSign);
		builder.addExtension(Extension.keyUsage, false, usage);

		// add extended key usage
		ASN1EncodableVector purposes = new ASN1EncodableVector();
		purposes.add(KeyPurposeId.id_kp_serverAuth);
		purposes.add(KeyPurposeId.id_kp_clientAuth);
		purposes.add(KeyPurposeId.anyExtendedKeyUsage);
		builder.addExtension(Extension.extendedKeyUsage, false, new DERSequence(purposes));

		// create signer for certificate
		ContentSigner signer = new JcaContentSignerBuilder("SHA512WithRSAEncryption").setProvider(BouncyCastleProvider.PROVIDER_NAME).build(keys.getPrivate());

		// sign and generate certificate and store private key
		ca_cert = new JcaX509CertificateConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME).getCertificate(builder.build(signer));
		ca_key = keys.getPrivate();

		// write both to files
		OutputStream fos;
		fos = new FileOutputStream(ca_cert_file);
		fos.write(ca_cert.getEncoded());
		fos.close();
		fos = new FileOutputStream(ca_key_file);
		fos.write(ca_key.getEncoded());
		fos.close();

		// create keystore preloaded with certificate
		KeyStore store = KeyStore.getInstance(KeyStore.getDefaultType());
		store.load(null, null);

		// add to keystore
		store.setKeyEntry("mineproxy", keys.getPrivate(), new char[] {'m', 'i', 'n', 'e', 'p', 'r', 'o', 'x', 'y'}, new Certificate[] { ca_cert });

		return store;
	}

	public KeyStore generateCertificate(String[] names) throws Exception {
		// inspired by LittleProxy-mitm

		// generate a new set of keys
		KeyPair keys = gen.generateKeyPair();

		// add name details
		X500NameBuilder name = new X500NameBuilder(BCStyle.INSTANCE);
		name.addRDN(BCStyle.CN, names[0]);
		name.addRDN(BCStyle.O, "MineProxy");
		name.addRDN(BCStyle.OU, "MineProxy");

		// add certificate details
		X500Name issuer = new X509CertificateHolder(ca_cert.getEncoded()).getSubject();
		BigInteger serial = new BigInteger(64, rand).abs();
		X500Name subject = name.build();

		// create builder for certificate
		X509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(issuer, serial, new Date(System.currentTimeMillis() - 365*24*60*60*1000), new Date(System.currentTimeMillis() + 365*24*60*60*1000), subject, keys.getPublic());

		// generate public key info
		ByteArrayInputStream bain = new ByteArrayInputStream(keys.getPublic().getEncoded());
		ASN1InputStream ais = new ASN1InputStream(bain);
		ASN1Sequence seq = (ASN1Sequence)ais.readObject();
		SubjectPublicKeyInfo info = new SubjectPublicKeyInfo(seq);

		// add public key info and basic constraints to certificate
		builder.addExtension(Extension.subjectKeyIdentifier, false, new BcX509ExtensionUtils().createSubjectKeyIdentifier(info));
		builder.addExtension(Extension.basicConstraints, false, new BasicConstraints(false));

		GeneralName[] gennames = new GeneralName[names.length];
		for (int i = 0; i < names.length; i++)
			gennames[i] = new GeneralName(GeneralName.dNSName, names[i]);
		GeneralNames san = new GeneralNames(gennames);
		builder.addExtension(Extension.subjectAlternativeName, false, san);

		// create signer for certificate
		ContentSigner signer = new JcaContentSignerBuilder("SHA512WithRSAEncryption").setProvider(BouncyCastleProvider.PROVIDER_NAME).build(ca_key);

		// sign and generate certificate
		X509Certificate cert = new JcaX509CertificateConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME).getCertificate(builder.build(signer));

		// ensure validity
		cert.checkValidity(new Date());
		cert.verify(ca_cert.getPublicKey());

		// create keystore preloaded with certificate
		KeyStore store = KeyStore.getInstance(KeyStore.getDefaultType());
		store.load(null, null);

		// add to keystore
		store.setKeyEntry(names[0], keys.getPrivate(), new char[] {'m', 'i', 'n', 'e', 'p', 'r', 'o', 'x', 'y'}, new Certificate[] { cert, ca_cert });

		return store;
	}

	public SSLContext generatePlainContext() throws Exception {
		// create a new SSLContext
		SSLContext context = SSLContext.getInstance("TLS");
		context.init(null, null, rand);

		return context;
	}

	public SSLContext generateSSLContext(String[] names) throws Exception {
		// generate keystore for certificate
		KeyStore store = generateCertificate(names);

		// create a key manager factory with password "mineproxy"
		KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
		kmf.init(store, new char[] {'m', 'i', 'n', 'e', 'p', 'r', 'o', 'x', 'y'});

		// create a new SSLContext with KMF
		SSLContext context = SSLContext.getInstance("TLS");
		context.init(kmf.getKeyManagers(), null, rand);

		return context;
	}
}
