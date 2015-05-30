package com.blochstech.bitcoincardterminal.Model.Communication;

public class NetworkPublishResults{
	public SendStatus Status;
	public String Message;
	
	public NetworkPublishResults(){
		Status = SendStatus.OK;
		Message = "";
	}
	
	public NetworkPublishResults(SendStatus status, String message){
		Status = status;
		Message = message;
	}
	
	public enum SendStatus{
		OK(0), //Remove from send queue, payment complete.
		Retry(1), //Try again sending later.
		Invalid(2);
		
		private int value;
		public int Value(){
			return value;
		}
		private SendStatus(int value) {
		  this.value = value;
		}
		public static SendStatus Convert(int value){
			return SendStatus.values()[value];
		}
	}
}
