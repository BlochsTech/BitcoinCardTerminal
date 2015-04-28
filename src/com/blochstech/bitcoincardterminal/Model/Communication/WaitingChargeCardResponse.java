package com.blochstech.bitcoincardterminal.Model.Communication;

public class WaitingChargeCardResponse {
	Double WaitingFee; //Miners fee
	Double WaitingAmount;
	Double WaitingTerminalAmount;
	Double WaitingCardFee; //So terminal knows exactly when enough sources have been sent.
	int[] WaitingAddress; //20 byte hash160
	int[] WaitingTerminalAddress; //20 byte hash160
	String WaitingVignereCode;
	boolean WaitingRequiresPin;
	boolean WaitingIsResetRequest;
}
