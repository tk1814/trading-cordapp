package net.corda.samples.trading.notaries;


import org.bouncycastle.math.ec.rfc8032.Ed25519;

import java.security.PublicKey;

// fixme: we can't use the `Ed25519PrivateKey` type in the Hedera SDK
// because that uses `Ed25519PrivateKeyParameters` which don't appear in the version of
// BouncyCastle (1.60) that Corda *insists* on using (dependency on a newer version is overridden).
public class SigningUtils {

    static byte[] privateKeyBytes = null;
    static PublicKey publicKey = null;
    static byte[] publicKeyBytes = null;

    static byte[] sign(byte[] privateKeyBytes, byte[] message) {
        byte[] signature = new byte[Ed25519.SIGNATURE_SIZE];
        Ed25519.sign(privateKeyBytes, 0, message, 0, message.length, signature, 0);
        return signature;
    }

//    static TransactionSignature sign(SecureHash txId, Party node){
//        byte[] signature = new byte[Ed25519.SIGNATURE_SIZE];
//        Ed25519.sign(privateKeyBytes, 0, txId.getBytes(), 0, txId.getBytes().length, signature, 0);
//        return new TransactionSignature(publicKeyBytes,publicKey,new SignatureMetadata(txId..platformVersion, Crypto.findSignatureScheme(publicKey).schemeNumberID));
//    }

}
