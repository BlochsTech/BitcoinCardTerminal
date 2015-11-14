package com.blochstech.bitcoincardterminal.Stratum;

import java.util.LinkedList;

//Class for storing and finding valid Stratum server URLs automatically.
public class StratumServerManager {
	private static StratumServerManager instance;
	public static StratumServerManager Instance(){
		if(instance == null)
			instance = new StratumServerManager();
		return instance;
	}
	
	//Simple add/remove at failures/server gets etc..
	private LinkedList<String> serverUrls = new LinkedList<String>();
	private int indexUsed = 0;
	
	//TODO: Persist server urls to disk later.
	
	public StratumServerManager(){
		serverUrls = new LinkedList<String>();
		//serverUrls.add("tobias-neumann.eu"); Gone
		//serverUrls.add("electrum.mindspot.org"); SLOOOW disconnect
		//serverUrls.add("wirerocks.infoha.us"); Does not exist
		serverUrls.add("electrum.bitfuzz.nl"); //OK!
	}
	
	public String GetServer(){
		String tempRes = null;
		
		if(serverUrls.size() > indexUsed)
			tempRes = serverUrls.get(indexUsed);
		
		indexUsed = (indexUsed+1) % serverUrls.size();
		
		return tempRes;
	}
	
	public void RemoveServer(){
		serverUrls.removeFirst();
	}
	
	public String StringRemoveAndGet(){
		RemoveServer();
		return GetServer();
	}
}
