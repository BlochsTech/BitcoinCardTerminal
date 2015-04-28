package com.blochstech.bitcoincardterminal.Interfaces;

public class Message {
	public long CreatedMillis;
	public String Message;
	public MessageType Type;

	public Message(String message, MessageType type, long createdMillis){
		this.Type = type;
		this.Message = message;
		this.CreatedMillis = createdMillis;
	}
	public Message(String message, MessageType type){
		this.Type = type;
		this.Message = message;
		this.CreatedMillis = System.currentTimeMillis();
	}
	public Message(String message){
		this.Type = MessageType.Info;
		this.Message = message;
		this.CreatedMillis = System.currentTimeMillis();
	}
	
	public enum MessageType{
		Info,
		Warning,
		Error
	}
}
