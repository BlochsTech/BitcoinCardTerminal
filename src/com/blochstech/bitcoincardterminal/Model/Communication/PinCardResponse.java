package com.blochstech.bitcoincardterminal.Model.Communication;

public class PinCardResponse {
	boolean PinAccepted;
	//boolean ClaimIsDEREncoded; Always in card. Very simple.
	byte[] TxDataPacket; //Multiple packets will be returned.
	boolean LastPacket;
}
