package ru.redguy.testtask;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;

public class Task4 {
    public static void main(String[] args) throws Exception {
        KeyPair keyPair = generateKeyPair();

        saveKey(keyPair.getPrivate(), "privateKey.pem");
        saveKey(keyPair.getPublic(), "publicKey.pem");

        byte[] data = Files.readAllBytes(Paths.get("gradlew.bat"));

        byte[] signature = sign(data, keyPair.getPrivate());

        Files.write(Paths.get("gradlew.bat.sig"), signature);

        boolean isVerified = verify(data, signature, keyPair.getPublic());
        System.out.println("Signature verified: " + isVerified);
    }

    public static KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048); // Размер ключа 2048 бит
        return keyPairGenerator.generateKeyPair();
    }

    public static void saveKey(@NotNull Key key, String fileName) throws Exception {
        byte[] keyBytes = key.getEncoded();
        Files.write(Paths.get(fileName), keyBytes);
    }

    public static byte[] sign(byte[] data, PrivateKey privateKey) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        signature.update(data);
        return signature.sign();
    }

    public static boolean verify(byte[] data, byte[] signature, PublicKey publicKey) throws Exception {
        Signature verifier = Signature.getInstance("SHA256withRSA");
        verifier.initVerify(publicKey);
        verifier.update(data);
        return verifier.verify(signature);
    }
}
