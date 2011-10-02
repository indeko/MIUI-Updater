package org.yaosupdater.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.util.Log;

public class MD5 {
	
	static String TAG = "MD5";
	
	public static boolean comprobarMD5(String md5, File archivo){
		String md5Calculado = calcularMD5(archivo);
		Log.d(TAG, "MD5SUM provided "+md5);
		Log.d(TAG, "MD5SUM calculated "+md5Calculado);
		if(md5Calculado.equalsIgnoreCase(md5)){
			return true;
		}
		return false;
	}

	public static String calcularMD5(File archivo){
		MessageDigest digest;
		try {
			 digest = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		InputStream is;
		try {
			is = new FileInputStream(archivo);
		} catch (FileNotFoundException e) {
			Log.e(TAG, "File "+archivo.getName()+" could not be found.", e);
			return null;
		}
		byte[] buffer = new byte[8192];
		int read;
		try {
			while((read = is.read(buffer)) > 0){
				digest.update(buffer, 0, read);
			}
			byte[] md5 = digest.digest();
			BigInteger bigInt = new BigInteger(1, md5);
			String resultado = bigInt.toString(16);
			resultado = String.format("%32s", resultado).replace(' ', '0');
			return resultado;
		} catch (IOException e) {
			Log.e(TAG, "Exception IOException on File "+ archivo.getName());
			return null;
		}finally{
			try {
				is.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}