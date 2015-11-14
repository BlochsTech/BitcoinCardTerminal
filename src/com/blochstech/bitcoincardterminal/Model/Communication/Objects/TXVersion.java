package com.blochstech.bitcoincardterminal.Model.Communication.Objects;

import java.math.BigInteger;

public class TXVersion extends TXElementBase {
	public long Value;
	
	public TXVersion(byte[] bytes) {
		Value = new BigInteger(bytes).longValue();
		this.SetBytes(bytes);
	}
}
