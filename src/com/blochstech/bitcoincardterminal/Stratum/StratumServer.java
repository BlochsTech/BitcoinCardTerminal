package com.blochstech.bitcoincardterminal.Stratum;

import java.util.LinkedList;

public class StratumServer {
	public static final int DISCONNECT_MAX = 3;
	public static final int EXCEPTION_MAX = 2;
	public static final int EXPIRE_AFTER_FAILING_AFTER_TIME_INTERVAL_DAYS = 7;
	public static final int SERVER_PORT = 50001;
	
	public String url;
	public int noConnectionCount = 0;
	public int exceptionCount = 0;
	public long lastUpTime = 0; //From System.currentMillis();
	
	public StratumServer(String url){
		this.url = url;
		lastUpTime = System.currentTimeMillis();
	}
	
	public String toString(){
		return url+","+noConnectionCount+","+exceptionCount+","+lastUpTime;
	}
	
	public static StratumServer fromString(String value){
		String[] values = value.split(",");
		
		StratumServer result = new StratumServer(values[0]);
		result.noConnectionCount = Integer.parseInt(values[1]);
		result.exceptionCount = Integer.parseInt(values[2]);
		result.lastUpTime = Long.parseLong(values[3]);
		
		return result;
	}
	
	public static LinkedList<StratumServer> convertServersFromStrings(LinkedList<String> rawData){
		LinkedList<StratumServer> result = new LinkedList<StratumServer>();
		
		if(rawData == null)
			return result;
		
		for(int i = 0; i < rawData.size(); i++){
			result.add(StratumServer.fromString(rawData.get(i)));
		}
		
		return result;
	}
	
	public static LinkedList<String> convertServersToStrings(LinkedList<StratumServer> data){
		LinkedList<String> result = new LinkedList<String>();
		
		if(data == null)
			return result;
		
		for(int i = 0; i < data.size(); i++){
			result.add(data.get(i).toString());
		}
		
		return result;
	}
}
