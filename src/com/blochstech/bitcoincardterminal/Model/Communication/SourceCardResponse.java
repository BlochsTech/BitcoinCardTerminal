package com.blochstech.bitcoincardterminal.Model.Communication;

class SourceCardResponse {
	TXSourceId Source;
	int NextIndex;
	
	SourceCardResponse(){
		Source = new TXSourceId();
	}
}
