package com.blochstech.bitcoincardterminal.Model.Communication;

class TXSourceId {
	String OutIndex = null; //Len 4 (8 in hex)
	String TXHash = null; //Len 32 (64 in hex)
	long Satoshi = 0;
	boolean Verified;
	boolean Unusable; //Card did not accept due to unusual script, error or block not difficult enough. //TODO: Use and handle.
}