package com.blochstech.bitcoincardterminal.Utils;

public class SimpleWebResponse {
	public String Response;
	public boolean IsConnected;
	
	public SimpleWebResponse(String response, boolean isConnected){
		Response = response;
		IsConnected = isConnected;
	}
}
