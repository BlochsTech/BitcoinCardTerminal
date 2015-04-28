package com.blochstech.bitcoincardterminal.Model.Communication;

public enum NetworkPublishResults{
	OK(0), //Remove from send queue, payment complete.
	Retry(1), //Try again sending later.
	Invalid(2);
	
	private int value;
	public int Value(){
		return value;
	}
	private NetworkPublishResults(int value) {
	  this.value = value;
	}
	public static NetworkPublishResults Convert(int value){
		return NetworkPublishResults.values()[value];
	}
}
