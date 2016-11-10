package com.jspiker.accesscontrolsystem;


import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Random;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Created by Luke on 2016-11-08.
 * This class contains the crypto utility methods that we will use throughout the project
 */

public class CryptoUtilities {

    private static final Random RANDOM = new SecureRandom();
    private static final int ITERATIONS = 16;
    private static final int KEYSIZE = 256;

    public byte[] generateSalt(){
        byte[] salt = new byte[32];
        RANDOM.nextBytes(salt);
        return salt;
    }

    // Code adapted from https://www.owasp.org/index.php/Hashing_Java
    public byte[] hashToken (final String token, final byte[] salt){
        try {

            SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance( "PBKDF2WithHmacSHA512" );
            PBEKeySpec spec = new PBEKeySpec( token.toCharArray(), salt, ITERATIONS, KEYSIZE );
            SecretKey key = secretKeyFactory.generateSecret( spec );
            byte[] hashedToken = key.getEncoded( );
            return hashedToken;

        } catch( NoSuchAlgorithmException | InvalidKeySpecException e ) {
            throw new RuntimeException( e );
        }

    }

    public boolean verifyTokenHash(String token, byte[] salt, byte[] receivedTokenHash){

        byte [] expectedTokenHash = hashToken(token, salt);

        if(expectedTokenHash.length != receivedTokenHash.length) return false;

        for(int i = 0; i < expectedTokenHash.length; i++){
            if(expectedTokenHash[i] != receivedTokenHash[i]) return false;
        }

        return true;
    }

}
