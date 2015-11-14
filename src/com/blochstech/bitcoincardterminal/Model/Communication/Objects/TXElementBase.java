package com.blochstech.bitcoincardterminal.Model.Communication.Objects;

import com.blochstech.bitcoincardterminal.Utils.ByteConversionUtil;

public class TXElementBase {
	public int ByteLength;
	public String HexValue;
	public byte[] Bytes;
	
	public void SetBytes(byte[] bytes)
	{
		ByteLength = bytes.length;
		Bytes = bytes;
		HexValue = ByteConversionUtil.byteArrayToHexString(bytes);
	}
}
