package com.blochstech.bitcoincardterminal.Utils;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.List;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;

import com.blochstech.bitcoincardterminal.Model.Communication.MerkleBranchElement;

public class ByteConversionUtil {
	private static ByteBuffer buffer8 = ByteBuffer.allocate(8); 
	private static MessageDigest digest = null;
	final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
	final protected static String base58Array = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"; //.toCharArray();
	
	public static String bytesToString(byte[] bytes, int startIndex, int length){
		byte[] nBytes;
		nBytes = new byte[length];
		
		for(int i = startIndex; i < startIndex+length; i++){
			nBytes[i-startIndex] = bytes[i];
		}
		
		try{
			return new String(nBytes, "UTF-8");
		}catch(UnsupportedEncodingException eEnc){
			return null;
		}
	}
	
	public static byte[] fromUnsigned(int[] ubytes){
		if (ubytes == null)
			return null;
		
		byte[] res = new byte[ubytes.length];
		int ubyte = 0;
		
		for(int i = 0; i < ubytes.length; i++){
			ubyte = Math.abs(ubytes[i]) % 256;
			res[i] = (byte) (ubyte >= 128 ? ubyte - 256 : ubyte);
		}
		
		return res;
	}
	public static int[] toUnsigned(byte[] bytes) {
		if (bytes == null)
			return null;
		
		int[] res = new int[bytes.length];
		
		for(int i = 0; i < bytes.length; i++){
			res[i] = (int) (bytes[i] < 0 ? bytes[i] + 256 : bytes[i]);
		}
		
		return res;
	}
	public static int toUnsigned(byte singlebyte) {
		return (int) (singlebyte < 0 ? singlebyte + 256 : singlebyte);
	}
	
	public static byte[] hexStringToByteArray(String s) {
		if(RegexUtil.isMatch(s, RegexUtil.CommonPatterns.BYTEHEX)){
		    int len = s.length();
		    byte[] data = new byte[len / 2];
		    for (int i = 0; i < len; i += 2) {
		        data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
		                             + Character.digit(s.charAt(i+1), 16));
		    }
		    return data;
		}else{
			return null;
		}
	}
	
	public static MerkleBranchElement[] MerkleListToArray(LinkedList<MerkleBranchElement> list){
		if(list == null)
			return null;
		
		MerkleBranchElement[] result = new MerkleBranchElement[list.size()];
		for(int i = 0; i < result.length; i++){
			result[i] = list.get(i);
		}
		return result;
	}
	
	public static String byteArrayToHexString(Byte[] bytes){
		if(bytes == null)
			return null;
		
		byte[] nBytes = new byte[bytes.length];
		for(int i = 0; i < bytes.length; i++)
		{
			nBytes[i] = (byte) bytes[i];
		}
		
		return byteArrayToHexString(nBytes);
	}
	
	public static String byteArrayToHexString(byte[] bytes) {
		if(bytes == null)
			return null;
		
	    char[] hexChars = new char[bytes.length * 2];
	    for ( int j = 0; j < bytes.length; j++ ) {
	        int v = bytes[j] & 0xFF;
	        hexChars[j * 2] = hexArray[v >>> 4];
	        hexChars[j * 2 + 1] = hexArray[v & 0x0F];
	    }
	    return new String(hexChars);
	}
	
	public static byte[] sha256(byte[] data){
		if(digest == null)
			try {
				digest = MessageDigest.getInstance("SHA-256");
			} catch (NoSuchAlgorithmException e) {
				return null;
			}
		
		return digest.digest(data);
	}
	public static byte[] doubleSHA256(byte[] data){
		if(digest == null)
			try {
				digest = MessageDigest.getInstance("SHA-256");
			} catch (NoSuchAlgorithmException e) {
				return null;
			}
		
		return digest.digest(digest.digest(data));
	}
	
	public static byte[] base58ToBytes(String value){
		BigInteger bigNum58 = new BigInteger("58");
		BigInteger tempBigValue = new BigInteger("0");
		int leadingZeroes = 0;
		boolean justStarted = true;
		for(int i = 0; i < value.length(); i++){
			if(justStarted && value.toCharArray()[i] == '1'){
				leadingZeroes++;
			}else{
				justStarted = false;
				tempBigValue = tempBigValue.add(
					bigNum58.pow(
						value.length() - i - 1
					).multiply(
						new BigInteger(
							"" + base58Array.indexOf(value.toCharArray()[i])
						)
					)
				);
			}
		}
		byte[] bigValue = tempBigValue.toByteArray();
		int bigValueStart = 0;
		for(int j = 0; j < bigValue.length; j++){
			if(bigValue[j] != 0){
				bigValueStart = j;
				break;
			}
		}
		byte[] byteResult = new byte[bigValue.length + leadingZeroes - bigValueStart];
		for(int i = 0; i < byteResult.length; i++){
			if(i-leadingZeroes+bigValueStart < bigValue.length && i-leadingZeroes+bigValueStart >= 0)
				byteResult[i] = i < leadingZeroes ? 0 : bigValue[i-leadingZeroes+bigValueStart];
		}
		return byteResult;
	}
	public static String BytesToBase58(byte[] value){
		//Add 1 for each 00 byte.
		//From lowest base58 fill with division remainders.
		String returnValue = "";
		boolean justStarted = true;
		BigInteger bigValue = new BigInteger(value); //TODO: Check that it works as it should.
		BigInteger base58 = new BigInteger("58");
		BigInteger zero = new BigInteger("0");
		BigInteger[] divisionResult;
		while(bigValue.compareTo(zero) == 1){ //Means greater than.
			divisionResult = bigValue.divideAndRemainder(base58);
			bigValue = divisionResult[0];
			returnValue = base58Array.toCharArray()[divisionResult[1].intValue()] + returnValue;
		}
		for(int i = 0; i < value.length; i++){
			if(value[i] == 0 && justStarted){
				returnValue = "1" + returnValue;
			}else{
				break;
			}
			justStarted = false;
		}
		return returnValue;
	}
	
	public static LinkedList<Byte> toList(byte[] array){
		LinkedList<Byte> resList = new LinkedList<Byte>();
		for(int i = 0; i < array.length; i++){
			resList.add(array[i]);
		}
		return resList;
	}
	
	public static Byte[] toBytes(List<Byte> listBytes){
		return toBytes(new LinkedList<Byte>(listBytes));
	}
	
	public static Byte[] toBytes(LinkedList<Byte> listBytes){
		Byte[] result = new Byte[listBytes.size()];
		for(int i = 0; i < result.length; i++){
			result[i] = listBytes.get(i);
		}
		return result;
	}
	
	public static Double[] toArray(LinkedList<Double> list){
		Double[] result = new Double[list.size()];
		for(int i = 0; i < result.length; i++){
			result[i] = list.get(i);
		}
		return result;
	}
	
	@SuppressWarnings("unchecked")
	public static <T,R> R safeAssign(T value, R defaultValue, T disAllow){
		try{
			if(value != null || value == disAllow)
				return (R)value;
		}catch(Exception ex){}
		
		return defaultValue;
	}
	
	public static <T,R> R safeAssign(T value, R defaultValue){
		return safeAssign(value, defaultValue, null);
	}
	
	public static <T> LinkedList<T> toList(T[] array){
		
		LinkedList<T> result = new LinkedList<T>();
		for(int i = 0; i < array.length; i++){
			result.add(array[i]);
		}
		return result;
	}
	
	public static byte[] toSimpleBytes(LinkedList<Byte> listBytes){
		byte[] result = new byte[listBytes.size()];
		for(int i = 0; i < result.length; i++){
			result[i] = listBytes.get(i);
		}
		return result;
	}
	
	public static byte[] toSimpleBytes(Byte[] bytes){
		byte[] simpleBytes = new byte[bytes.length];
		for(int i = 0; i < bytes.length; i++){
			simpleBytes[i] = bytes[i];
		}
		return simpleBytes;
	}

	public static int[][] getSignatureIndexesAndLengths(LinkedList<Byte> PaymentTXBytes) throws Exception{
		return getSignatureIndexesAndLengths(toSimpleBytes(PaymentTXBytes));
	}
	
	public static int[][] getSignatureIndexesAndLengths(Byte[] PaymentTXBytes) throws Exception{
		return getSignatureIndexesAndLengths(toSimpleBytes(PaymentTXBytes));
	}
	
	//OBSOLETE! NOT USED! DER encoding turned out to be easy and is done in card now.
	public static int[][] getSignatureIndexesAndLengths(byte[] PaymentTXBytes) throws Exception{
		int sigIndex;
		int[] UPayTXBytes = ByteConversionUtil.toUnsigned(PaymentTXBytes);
		
		sigIndex = 4; //Version
		int txIns = UPayTXBytes[sigIndex]; //TXIns
		sigIndex += 1; //TXIns.
		int scriptLength = 0;
		int[][] result = new int[txIns][2];
		for(int i = 0; i < txIns; i++){
			sigIndex += 36; //TXInHash and index.
			scriptLength = UPayTXBytes[sigIndex]; //VarInt with length of script.
			result[i][0] = sigIndex+3;
			result[i][1] = UPayTXBytes[sigIndex+1]; //+1 due to varint and using push byte. TODO: Dangerous assumption that its one only byte.
			sigIndex += scriptLength + 4; //+4 for the 4 "FF" sequence bytes.
		}
		
		return result;
	}
	
	public static byte intToByte(int value){
		return leIntToBytes(value)[0];
	}
	
    public synchronized static byte[] leIntToBytes(long x) { //Little endian.
        buffer8.putLong(0, x);
        byte[] res = new byte[4];
        for(int i = 7; i > 3; i--){
        	res[7-i] = buffer8.array()[i];
        }
        return res;
    }
    
    public static long UByteArrayToBENumber(int[] ubytes){
    	long p = 1;
		long res = 0;
		for(int i = 0; i < ubytes.length; i++){
			res = res + p * ubytes[i];
			p = p * 256;
		}
		return res;
    }
    
    public static long UByteArrayToLENumber(int[] ubytes){
    	long p = 1;
		long res = 0;
		for(int i = ubytes.length-1; i >= 0; i--){
			res = res + p * ubytes[i];
			p = p * 256;
		}
		return res;
    }
    
    public static int[] subBytes(int[] bytes, int start, int length){
    	if(start + length > bytes.length || bytes == null || start < 0){
    		return null;
    	}
    	int[] nbytes = new int[length];
    	int j = 0;
    	for(int i = start; i < (start+length); i++){
    		nbytes[j] = bytes[i];
    		j++;
    	}
    	return nbytes;
    }
    
    public static byte[] subBytes(byte[] bytes, int start, int length){
    	if(start + length > bytes.length || bytes == null || start < 0){
    		return null;
    	}
    	byte[] nbytes = new byte[length];
    	int j = 0;
    	for(int i = start; i < (start+length); i++){
    		nbytes[j] = bytes[i];
    		j++;
    	}
    	return nbytes;
    }
    
    public static String reverseByteOrder(String hash){
    	if(RegexUtil.isMatch(hash, RegexUtil.CommonPatterns.BYTEHEX)){
    		String result = "";
    		for(int i = 0; i < hash.length(); i=i+2){
    			result = hash.substring(i,i+2) + result;
    		}
    		return result;
    	}else{
    		return null;
    	}
    }
    
    public static int merkleLevels(long x){
    	Double tmp = (Math.log(x) / Math.log(2));
    	if(tmp-((int)(0+tmp)) > 0.0){
    		return (int) (tmp+1);
    	}else{
    		return (int) (0+tmp);
    	}
    }
}
