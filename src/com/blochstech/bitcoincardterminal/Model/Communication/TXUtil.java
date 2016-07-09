package com.blochstech.bitcoincardterminal.Model.Communication;

import java.math.BigInteger;
import java.util.LinkedList;

import android.util.Log;

import com.blochstech.bitcoincardterminal.Model.Communication.Objects.*;
import com.blochstech.bitcoincardterminal.Utils.ByteConversionUtil;
import com.blochstech.bitcoincardterminal.Utils.Tags;

public class TXUtil {
	public static TXObject ParseTXToObjectForm(String txHex) throws Exception{
		if(txHex == null || txHex.length() == 0)
			return null;
		
		TXObject result = new TXObject();
		result.RawTX = txHex;
		result.TXBytes = ByteConversionUtil.hexStringToByteArray(txHex);
		
		result.Version = new TXVersion(ByteConversionUtil.subBytes(result.TXBytes, 0, 4));
		
		int readIndex = 4;
		result.TXInCount = ReadVarInt(ByteConversionUtil.subBytes(result.TXBytes, readIndex, 9));
		readIndex += result.TXInCount.ByteLength;
		
		TXInObject txIn;
		byte[] prevHash, prevIndex;
		VarInt scriptLength;
		result.TXIns = new LinkedList<TXInObject>();
		for(int i = 0; i < result.TXInCount.Value; i++){
			prevHash = ByteConversionUtil.subBytes(result.TXBytes, readIndex, 32);
			readIndex += 32;
			prevIndex = ByteConversionUtil.subBytes(result.TXBytes, readIndex, 4);
			readIndex += 4;
			scriptLength = ReadVarInt(ByteConversionUtil.subBytes(result.TXBytes, readIndex, 9));
			
			txIn = new TXInObject(ByteConversionUtil.subBytes(result.TXBytes, readIndex-36, 36+4+scriptLength.ByteLength+(int)scriptLength.Value), 
					prevHash, 
					prevIndex, 
					scriptLength,
					ByteConversionUtil.subBytes(result.TXBytes, readIndex+scriptLength.ByteLength, (int)scriptLength.Value), 
					ByteConversionUtil.subBytes(result.TXBytes, readIndex+scriptLength.ByteLength+(int)scriptLength.Value, 4));
			
			readIndex += scriptLength.ByteLength + scriptLength.Value + 4;
			
			//Null pointer below
			result.TXIns.add(txIn);
		}
		
		result.TXEnd = new TXElementBase();
		result.TXEnd.SetBytes(ByteConversionUtil.subBytes(result.TXBytes, readIndex, result.TXBytes.length-readIndex));
		
		result.UpdateBase();
		
		return result;
	}
	
	public static TXObject CorrectSignatureS(TXObject txObj) throws Exception{
		int[] unsignedScript;
		int opPush; //Index 0
		int readIndex;
		int derSeqLength; //Index 2
		int derIntLength1; //Index 4
		int derIntLength2, derIntLength2Index;
		int indexOffSet = 0;
		byte[] S;
		TXInObject newTXIn;
		
		for(int i = 0; i < txObj.TXInCount.Value; i++){
			readIndex = 0;
			//1. Get push byte/DER lengths remember value and index.
			unsignedScript = ByteConversionUtil.toUnsigned(txObj.TXIns.get(i).Script);
			opPush = unsignedScript[readIndex];
			if(opPush > 75)
				throw new Exception("Can not parse TX - not standard/simple.");
			readIndex += 2;
			
			derSeqLength = unsignedScript[readIndex];
			if(derSeqLength > 70)
				throw new Exception("Can not parse TX - not standard/simple.");
			readIndex += 2;
			
			derIntLength1 = unsignedScript[readIndex];
			derIntLength2Index = readIndex+1+derIntLength1+1;
			readIndex += derIntLength1+2;
			
			derIntLength2 = unsignedScript[readIndex];
			readIndex += 1;
			
			//2. Correct S:
			S = ByteConversionUtil.subBytes(txObj.TXIns.get(i).Script, readIndex, derIntLength2);
			S = CorrectS(S);
			indexOffSet = S.length - derIntLength2;
			
			//3. Insert into script:
			byte[] preSection = ByteConversionUtil.subBytes(txObj.TXIns.get(i).Script, 0, readIndex);
			byte[] postSection = ByteConversionUtil.subBytes(txObj.TXIns.get(i).Script, readIndex + derIntLength2,
					txObj.TXIns.get(i).Script.length - readIndex - derIntLength2);
			byte[] newScript = ByteConversionUtil.concat(preSection, S);
			newScript = ByteConversionUtil.concat(newScript, postSection);
			
			//4. Update lengths (script len, op_push length, DER seq length and DERInt2Length):
			if(indexOffSet != 0){
				newScript[0] = ByteConversionUtil.fromUnsigned(indexOffSet + ByteConversionUtil.toUnsigned(newScript[0])); //OPPush
				newScript[2] = ByteConversionUtil.fromUnsigned(indexOffSet + ByteConversionUtil.toUnsigned(newScript[2])); //DER Seq length
				newScript[derIntLength2Index] = ByteConversionUtil.fromUnsigned(indexOffSet + ByteConversionUtil.toUnsigned(newScript[derIntLength2Index])); //Length of int 2 (S)
				
			}
			newTXIn = txObj.TXIns.get(i);
			newTXIn.Script = newScript;
			newTXIn.ScriptLength = new VarInt((int)newTXIn.ScriptLength.Value+indexOffSet);
			newTXIn.UpdateBase();
			txObj.TXIns.set(i, newTXIn);
		}
		
		txObj.UpdateBase();
		
		return txObj;
	}
	
	//https://github.com/bitcoin/bips/blob/master/bip-0062.mediawiki#low-s-values-in-signatures
	private static BigInteger maxValue = new BigInteger(ByteConversionUtil.hexStringToByteArray("7FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF5D576E7357A4501DDFE92F46681B20A0"));
	private static BigInteger pivotValue = new BigInteger(ByteConversionUtil.hexStringToByteArray("00FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEBAAEDCE6AF48A03BBFD25E8CD0364141"));
	private static byte[] CorrectS(byte[] sBytes) throws Exception{
		//byte[] sBytesReversed = ByteConversionUtil.reverseByteOrder(sBytes);
		//Remove stupid DER padding:
		/*if(sBytes.length > 32){
			sBytes = ByteConversionUtil.subBytes(sBytes, 1, 32);
		}*/ //ACTUALLY: Keep stupid padding, java BigInteger works the same way.
		
		BigInteger S = new BigInteger(sBytes);
		if(S.compareTo(maxValue) > 0){
			if(Tags.DEBUG)
				Log.w(Tags.APP_TAG, "S value above max, terminal had to correct it.");
			
			//Do change:
			if(S.compareTo(pivotValue) > 0)
				throw new Exception("New S value would be negative.");
			
			S = pivotValue.subtract(S);
			sBytes = S.toByteArray();
			sBytes = ByteConversionUtil.normalizeArray(sBytes, 32);
			
			//Add stupid DER padding again if high byte more than/equal to 0x80:
			if(sBytes[31] < 0)
				sBytes = ByteConversionUtil.preAppendHighZeroPadding(sBytes, 1);
			
			return sBytes;
		}else{
			return sBytes;
		}
	}
	
	//TODO: To var int method
	public static VarInt ReadVarInt(byte[] bytes) throws Exception{
		int firstByte = ByteConversionUtil.toUnsigned(bytes[0]);
		if(firstByte < 253)
			return new VarInt(firstByte);
		
		int byteLength;
		if(firstByte < 254)
			byteLength = 2;
		if(firstByte < 255)
			byteLength = 4;
		else
			byteLength = 8;
		
		return new VarInt(ByteConversionUtil.subBytes(bytes, 0, byteLength+1), 
				new BigInteger(ByteConversionUtil.subBytes(bytes, 1, byteLength)).longValue());
	}
	
	public static int VarIntLength(long value){
		if(value < 253)
			return 1;
		if(value < 65536)
			return 3;
		BigInteger bytes5 = new BigInteger("4294967296");
		if(value < bytes5.longValue())
			return 5;
		return 9;
	}
}
