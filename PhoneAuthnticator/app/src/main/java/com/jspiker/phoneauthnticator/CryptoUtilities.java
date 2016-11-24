package com.jspiker.phoneauthnticator;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Random;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Created by Luke on 2016-11-23.
 */

public class CryptoUtilities {

    private static final Random RANDOM = new SecureRandom();
    private static final int ITERATIONS = 16;
    private static final int KEYSIZE = 256;

    // Code adapted from https://www.owasp.org/index.php/Hashing_Java
    public byte[] hashToken (final String token, final byte[] salt){
        try {

            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update((new String(salt).concat(token)).getBytes());
            return md.digest();

//            SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance( "PBKDF2WithHmacSHA512" );
//            PBEKeySpec spec = new PBEKeySpec( token.toCharArray(), salt, ITERATIONS, KEYSIZE );
//            SecretKey key = secretKeyFactory.generateSecret( spec );
//            byte[] hashedToken = key.getEncoded( );
//            return hashedToken;

        } catch( NoSuchAlgorithmException e) {
            throw new RuntimeException( e );
        }

    }
}
