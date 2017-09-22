package com.synochip.sdk.ukey;

import javax.crypto.Cipher;
import javax.crypto.spec.DESKeySpec; 
import javax.crypto.spec.DESedeKeySpec;
import javax.crypto.SecretKeyFactory;  
import javax.crypto.SecretKey;

public class DESUtil {
	
	public static final String ALGORITHM_DES = "DES";
	public static final String ALGORITHM_TDES = "DESede";
	
 
    public static byte [] encryptByte( byte [] byteS,byte []key) {
    	
       byte [] byteFina = null ;
       byteFina = null ;
       Cipher cipher;
       SecretKey secretKey;
       
       try {
    	   //
    	   DESKeySpec dks = new DESKeySpec(key);  
           SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(ALGORITHM_DES);  
           secretKey = keyFactory.generateSecret(dks);           
       } catch (Exception e) {
    	   //
           throw new RuntimeException(
        		 //  
                  "Error initializing SqlMap class. Cause: " + e);           
       }
       
       try {
    	   //
           cipher = Cipher.getInstance ("DES/ECB/NoPadding");
           cipher.init(Cipher. ENCRYPT_MODE , secretKey );
           byteFina = cipher.doFinal(byteS);
           
       } catch (Exception e) {
           throw new RuntimeException(
                  "Error initializing SqlMap class. Cause: " + e);
       } finally {
           cipher = null ;
       }
       return byteFina;
    }
 
    /**
      * 解密以 byte[] 密文输入 , 以 byte[] 明文输出
      *
      * @param byteD
      * @return
      */
    public static byte [] decryptByte( byte [] byteD,byte []key) {
    	
       Cipher cipher;
       byte [] byteFina = null ;
       SecretKey secretKey;
       try {
    	   //
    	   DESKeySpec dks = new DESKeySpec(key);  
           SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(ALGORITHM_DES);  
           secretKey = keyFactory.generateSecret(dks); 
       } catch (Exception e) {
    	   //
           throw new RuntimeException(
                  "Error initializing SqlMap class. Cause: " + e);
       }
       
       try {
    	   //
           cipher = Cipher.getInstance ( "DES/ECB/NoPadding" );
           cipher.init(Cipher. DECRYPT_MODE , secretKey );
           byteFina = cipher.doFinal(byteD);
       } catch (Exception e) {
           throw new RuntimeException(
                  "Error initializing SqlMap class. Cause: " + e);
       } finally {
           cipher = null ;
       }
       return byteFina;
    }
    
    public static byte [] triEncryptByte( byte [] byteS,byte []key) {
    	
        byte [] byteFina = null ;
        byteFina = null ;
        Cipher cipher;
        SecretKey secretKey;
        
        try {
        	DESedeKeySpec dks = new DESedeKeySpec(key);  
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(ALGORITHM_TDES);  
            secretKey = keyFactory.generateSecret(dks); 
        } catch (Exception e) {
            throw new RuntimeException(
                   "Error initializing SqlMap class. Cause: " + e);
        }
        
        try {
            cipher = Cipher.getInstance ( "DESede/ECB/NoPadding" );
            cipher.init(Cipher. ENCRYPT_MODE , secretKey );
            byteFina = cipher.doFinal(byteS);
        } catch (Exception e) {
            throw new RuntimeException(
                   "Error initializing SqlMap class. Cause: " + e);
        } finally {
            cipher = null ;
        }
        return byteFina;
     } 
 
    public static byte [] triDecryptByte( byte [] byteS,byte []key) {
    	
        byte [] byteFina = null ;
        byteFina = null ;
        Cipher cipher;
        SecretKey secretKey;
        
        try {
     	   DESKeySpec dks = new DESKeySpec(key);  
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");  
            secretKey = keyFactory.generateSecret(dks); 
        } catch (Exception e) {
            throw new RuntimeException(
                   "Error initializing SqlMap class. Cause: " + e);
        }
        
        try {
            cipher = Cipher.getInstance ( "DES/ECB/NoPadding" );
            cipher.init(Cipher. ENCRYPT_MODE , secretKey );
            byteFina = cipher.doFinal(byteS);
        } catch (Exception e) {
            throw new RuntimeException(
                   "Error initializing SqlMap class. Cause: " + e);
        } finally {
            cipher = null ;
        }
        return byteFina;
     }  
} 