package com.blochstech.bitcoincardterminal.Model.Communication;

public class Callback {
	public Object[] Response;
	public int MethodId;
	
	public Callback(int methodId, Object... response){
		Response = response;
		MethodId = methodId;
	}
}
