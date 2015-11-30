package com.blochstech.bitcoincardterminal.Stratum;

import java.util.LinkedList;

import android.util.Log;

import com.blochstech.bitcoincardterminal.DataLayer.GenericFileCache;
import com.blochstech.bitcoincardterminal.Utils.Tags;

//Class for storing and finding valid Stratum server URLs automatically.
public class StratumServerManager {
	private static StratumServerManager instance;
	public static StratumServerManager Instance(){
		if(instance == null)
			instance = new StratumServerManager();
		return instance;
	}
	
	//Simple add/remove at failures/server gets etc..
	private LinkedList<StratumServer> servers = new LinkedList<StratumServer>();
	private int indexUsed = 0;
	
	//TODO: Persist server urls to disk later.
	private static GenericFileCache<LinkedList<String>> urlCache;
	private final static String urlCachePath = "/BitcoinTerminal/Stratum/serversCache.bin";
	//TODO: Now, too big an issue.
	
	public StratumServerManager(){
		LinkedList<String> firstServers = new LinkedList<String>();
		if(urlCache == null){
			//Initial servers:
			//serverUrls.add("tobias-neumann.eu"); Gone
			firstServers.add("electrum.mindspot.org"); //SLOOOW disconnect
			//serverUrls.add("wirerocks.infoha.us"); Does not exist
			//serverUrls.add("electrum.bitfuzz.nl"); //OK! aand its gone
			firstServers.add("VPS.hsmiths.com"); //OK. Need permanent solution though...
			
			try {
				urlCache = new GenericFileCache<LinkedList<String>>(urlCachePath, firstServers);
			} catch (Exception e) {
				urlCache = null;
				if(Tags.DEBUG)
					Log.e(Tags.APP_TAG, "StratumServerManager could not instantiate cache, default "
						+ "servers will be used. " + e.getMessage());
			}
		}
		
		Object holder = new Object();
		try{
			urlCache.Open(holder);
			firstServers = urlCache.get(holder);
			urlCache.Close(holder);
		}catch (Exception ex){
			urlCache.Close(holder);
			if(Tags.DEBUG)
				Log.e(Tags.APP_TAG, "Failed to get stratum servers from disk. Hardcoded values will be used initially. Exception: " + ex.toString());
		}
		
		LoadServers(firstServers);
	}
	
	private void LoadServers(LinkedList<String> initialServers){
		for(int i = 0; i < initialServers.size(); i++){
			servers.add(new StratumServer(initialServers.get(i)));
		}
	}
	
	public String GetServer(){
		if(servers == null)
			return null;
		
		StratumServer tempRes = null;
		int minErr = 5000;
		int minErrIndex = 0;
		
		StratumServer tempSrv;
		for(int i = 0; i < servers.size(); i++){
			tempSrv = servers.get(i);
			if(...)
		}
		
		if(servers.size() > indexUsed)
			tempRes = servers.get(indexUsed);
		
		indexUsed = (indexUsed+1) % servers.size();
		
		return tempRes.url;
	}
	
	public void ServerIssue(boolean exception){
		//TODO
		indexUsed
	}
}
