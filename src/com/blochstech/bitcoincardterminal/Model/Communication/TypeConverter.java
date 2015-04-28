package com.blochstech.bitcoincardterminal.Model.Communication;

import java.nio.ByteBuffer;

import com.blochstech.bitcoincardterminal.Utils.ByteConversionUtil;
import com.blochstech.bitcoincardterminal.Utils.RegexUtil;

public class TypeConverter {
	private static ByteBuffer buffer8 = ByteBuffer.allocate(8); 

	public static int[] fromBase58CheckToAddressBytes(String value){
		int[] data = fromBase58CheckToData(value);
		if(data == null || data.length != 21){
			int[] nullAddress = new int[21];
			for(int i = 0; i < 21; i++)
			{
				nullAddress[i] = 0;
			}
			return nullAddress;
		}
		return data;
	}
	public static int[] fromBase58CheckToData(String value) {
		byte[] rawBytes = basicBase58CheckVerification(value);
		if(rawBytes == null || rawBytes.length == 4)
			return null;
		
		int[] uRawBytes = ByteConversionUtil.toUnsigned(rawBytes);
		
		int[] data = new int[uRawBytes.length-4];
		
		//First byte is address version (normal/pay2script), next 20 bytes are raw Hash160 bytes, last 4 bytes are a checksum:
		for(int i = 0; i < uRawBytes.length; i++){
			if (i < uRawBytes.length-4){
				data[i] = uRawBytes[i];
			}
		}
		
		return data;
	}
	//Null return means false.
	private static byte[] basicBase58CheckVerification(String base58CheckData){
		//Handle case where there are no check bytes, ie length < 4/null:
		if(base58CheckData == null || base58CheckData.length() == 0)
			return null;
		//Handle invalid characters (regex):
		if(!RegexUtil.isMatch(base58CheckData, RegexUtil.CommonPatterns.BASE58_CHARS))
			return null;
		
		byte[] rawBytes = ByteConversionUtil.base58ToBytes(base58CheckData);
		if(rawBytes.length < 4)
			return null; //Hash of null is valid, but we require the checkbytes.
		
		return rawBytes;
	}
	public static boolean verifyBase58CheckSum(String base58CheckData) {
		return verifyBase58CheckSum(base58CheckData, true);
	}
	public static boolean verifyBase58CheckSum(String base58CheckData, boolean requireBitcoinLength) {
		byte[] rawBytes = basicBase58CheckVerification(base58CheckData);
		if(rawBytes == null)
			return false;
		
		int[] dataBytes = fromBase58CheckToData(base58CheckData);
		
		//Check length:
		if(requireBitcoinLength)
		{
			if(dataBytes == null || dataBytes.length != 21) //1 byte address type and 20 bytes hash160.
				return false;
		}
		
		byte[] shaBytes = ByteConversionUtil.fromUnsigned(dataBytes);
		
		shaBytes = ByteConversionUtil.doubleSHA256(shaBytes);
		for(int i = 0; i < 4; i++){
			if(shaBytes[i] != rawBytes[i+rawBytes.length-4])
				return false;
		}
		return true;
	}
	
    public synchronized static byte[] longToBytes(long x) {
        buffer8.putLong(0, x);
        return buffer8.array();
    }
    
    /*static byte[] derEncodeSigs(LinkedList<Byte> txBytes) throws Exception{
    	//String txHex = ByteConversionUtil.byteArrayToHexString(ByteConversionUtil.toSimpleBytes(txBytes));
		LinkedList<Byte> result = new LinkedList<Byte>();
    	int[][] sigLocs = ByteConversionUtil.getSignatureIndexesAndLengths(txBytes);
    	byte[] tmpBytes;
    	int tmpInt = 0;
    	
    	result = new LinkedList<Byte>(txBytes.subList(0, sigLocs[0][0]-2));
    	for(int i = 0; i < sigLocs.length; i++){
    		//Calculate encoded sig part of script.
    		tmpBytes = ByteConversionUtil.toDERBytes(ByteConversionUtil.toBytes(txBytes.subList(sigLocs[i][0], sigLocs[i][0]+sigLocs[i][1])));
    		
    		if(i > 0){
        		//Index after the last TXIn.
	    		tmpInt = sigLocs[i-1][0]+sigLocs[i-1][1]+33+4; //33 is the publickey and hashtype, 4 is FF sequence bytes
	    		//TXBytes before script added to result.
	    		result.addAll(txBytes.subList(tmpInt, tmpInt+sigLocs[i][0]-2)); //First part of TX at i=0, afte that always 36.
    		}

    		//New varint based on old varint and difference in sig length.
    		tmpInt = ByteConversionUtil.toUnsigned(txBytes.get(sigLocs[i][0]-2));
    		result.add(ByteConversionUtil.intToByte(tmpInt - (sigLocs[i][1] - tmpBytes.length)));
    		
    		//New encoded sig length.
    		result.add(ByteConversionUtil.intToByte(tmpBytes.length));
    		
    		//Add the new encoded signature.
    		result.addAll(ByteConversionUtil.toList(tmpBytes));
    		
    		//Add the TXIn bytes after the sig:
    		tmpInt = sigLocs[i][0]+sigLocs[i][1]; //After sig.
    		result.addAll(txBytes.subList(tmpInt, tmpInt + //Start after sig.
    				ByteConversionUtil.toUnsigned(txBytes.get(tmpInt))+4+1)); //End at after push, pubkey and sequence bytes.
    	}
    	
    	//Add the rest of the tx.
    	tmpInt += ByteConversionUtil.toUnsigned(txBytes.get(tmpInt))+4+1; //tmpInt has value of after last sig here.
    	result.addAll(txBytes.subList(tmpInt, txBytes.size()-tmpInt));
		
    	return ByteConversionUtil.toSimpleBytes(result);
    }*/
	
	static int[] toCardFloatType(Double value){ //Always len=3, 2 bytes for int/mantissa and 1 byte for exp.
		long satoshis = value != null ? (long) (100000000*value) : 0;
		long lastSatoshis = 0;
		
		byte exp = 0;
		while(satoshis > 32767){
			if(exp < 0){
				exp = (byte) (exp - 1);
			}else{
				exp++;
			}
			lastSatoshis = satoshis;
			satoshis = satoshis / 10;
		}
		if(satoshis < 32767 && (lastSatoshis % 10) >= 5){
			satoshis++;
		}
		
		byte[] amount = longToBytes(satoshis);
		
		int[] result = new int[3];
		result[0] = (int) (amount[6] < 0 ? amount[6] + 256 : amount[6]);
		result[1] = (int) (amount[7] < 0 ? amount[7] + 256 : amount[7]); //Switched for correct byte order.. May have to re-switch.
		result[2] = (int) (exp < 0 ? exp + 256 : exp);
		
		return result;
	}
}
