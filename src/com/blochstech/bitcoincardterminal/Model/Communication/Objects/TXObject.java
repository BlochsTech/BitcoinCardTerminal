package com.blochstech.bitcoincardterminal.Model.Communication.Objects;

import java.util.LinkedList;

import com.blochstech.bitcoincardterminal.Utils.ByteConversionUtil;

public class TXObject extends TXElementBase {
	public String RawTX;
	public byte[] TXBytes;
	
	public TXVersion Version; //4 bytes
	public VarInt TXInCount; //Varint
	public LinkedList<TXInObject> TXIns;
	
	public TXElementBase TXEnd; //String with tx end section (everything after the ins).
	
	public void UpdateBase(){
		byte[] newTXBytes = ByteConversionUtil.concat(Version.Bytes, TXInCount.Bytes);
		for(int i = 0; i < TXInCount.Value; i++){
			newTXBytes = ByteConversionUtil.concat(newTXBytes, TXIns.get(i).Bytes);
		}
		newTXBytes = ByteConversionUtil.concat(newTXBytes, TXEnd.Bytes);
		SetBytes(newTXBytes);
		RawTX = ByteConversionUtil.byteArrayToHexString(newTXBytes);
	}
}
