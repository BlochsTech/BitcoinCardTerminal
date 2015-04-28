package com.blochstech.bitcoincardterminal.Model.Communication;

//Now used strictly for NfcWrapper callbacks.
public enum BitcoinCallbackMethods{
	Network(0),
	Protocol(1),
	Addresses(2),
	RequestPayment(3),
	Pin(4),
	Sources(5),
	TX(6),
	Headers(7),
	Hashes(8),
	TimeUnlock(9),
	MaxAmount(10),
	WaitingCharge(11),
	DumpSources(12), //Unused in this terminal
	Decimals(13), //Unused in this terminal
	WantData(14),
	MaxSources(15),
	ResetPinCode(16),
	DebugOnce(254), //Disconnects card after getting message. (Same protocol command as Debug, 254 not a real command number)
	Debug(255), //Calls ProcessStateChange after getting message.
	None(256); //Special state not related to card commands.
	
	private int value;
	public int Value(){
		return value;
	}
	private BitcoinCallbackMethods(int value) {
	  this.value = value;
	}
	public static BitcoinCallbackMethods Convert(int value){
		return BitcoinCallbackMethods.values()[value];
	}
}
