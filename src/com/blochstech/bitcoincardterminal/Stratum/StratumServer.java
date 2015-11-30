package com.blochstech.bitcoincardterminal.Stratum;

public class StratumServer {
	public static final int DISCONNECT_MAX = 2;
	public static final int EXCEPTION_MAX = 1;
	
	public String url;
	public int noConnectionCount = 0;
	public int exceptionCount = 0;
	
	public StratumServer(String url){
		this.url = url;
	}
}
