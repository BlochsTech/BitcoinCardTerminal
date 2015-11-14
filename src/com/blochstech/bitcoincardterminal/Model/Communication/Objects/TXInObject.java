package com.blochstech.bitcoincardterminal.Model.Communication.Objects;

import com.blochstech.bitcoincardterminal.Utils.ByteConversionUtil;

public class TXInObject extends TXElementBase {
	public byte[] PreviousHash; //32 bytes always
	public byte[] PreviousIndex; //4 bytes
	public VarInt ScriptLength; //1+ bytes / VarInt structure
	public byte[] Script;
	public byte[] Sequence; //4 bytes always
	
	public TXInObject(byte[] bytes, byte[] previousHash, byte[] previousIndex, VarInt scriptLength, byte[] script, byte[] sequence)
	{
		PreviousHash = previousHash;
		PreviousIndex = previousIndex;
		Script = script;
		ScriptLength = scriptLength;
		Sequence = sequence;
		this.SetBytes(bytes);
	}
	
	public void UpdateBase(){
		byte[] newBytes = ByteConversionUtil.concat(PreviousHash, PreviousIndex);
		newBytes = ByteConversionUtil.concat(newBytes, ScriptLength.Bytes);
		newBytes = ByteConversionUtil.concat(newBytes, Script);
		newBytes = ByteConversionUtil.concat(newBytes, Sequence);
		SetBytes(newBytes);
	}
}
