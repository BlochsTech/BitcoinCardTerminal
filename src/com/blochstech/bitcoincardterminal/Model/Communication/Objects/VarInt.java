package com.blochstech.bitcoincardterminal.Model.Communication.Objects;

import com.blochstech.bitcoincardterminal.Utils.ByteConversionUtil;

public class VarInt extends TXElementBase {
	public long Value;
	
	public VarInt(byte[] bytes, long value){
		this.SetBytes(bytes);
		Value = value;
	}
	
	public VarInt(int value) throws Exception{
		if(value >= 253)
			throw new Exception("Invalid use of constructor.");
		
		byte[] bytes = new byte[1];
		bytes[0] = ByteConversionUtil.intToByte(value);
		
		this.SetBytes(bytes);
		Value = value;
	}
}
