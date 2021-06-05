package com.example.dsceconnect;

import java.nio.charset.StandardCharsets;

public class EncModel {
    private static String hashAlgorithm = "SHA-256";
    public static String generateEncryptionKey(String password)throws Exception{

        //primary hash for encryption, returns bytes
        byte[] primaryHash = ShaUtils.digest(password.getBytes(StandardCharsets.UTF_8), hashAlgorithm);

        //initial key for encryption, returns String
        String finalKey = CryptoUtils.encrypt(primaryHash,password);

        return finalKey;
    }

    public static String genreateIDWK(String password)throws Exception
    {
        String message=generateEncryptionKey(password);

        byte[] userIDbytes = ShaUtils.digest(message.getBytes(StandardCharsets.UTF_8), hashAlgorithm);

        String userID=String.format(ShaUtils.bytesToHex(userIDbytes));

        return userID;
    }

    public static String genreateID(String password)throws Exception
    {
        byte[] userIDbytes = ShaUtils.digest(password.getBytes(StandardCharsets.UTF_8), hashAlgorithm);

        String userID=String.format(ShaUtils.bytesToHex(userIDbytes));

        return userID;
    }

}
