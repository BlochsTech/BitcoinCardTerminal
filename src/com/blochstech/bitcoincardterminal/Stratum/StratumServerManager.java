package com.blochstech.bitcoincardterminal.Stratum;

import java.util.LinkedList;

import org.json.JSONArray;
import org.json.JSONObject;

import android.net.http.AndroidHttpClient;
import android.util.Log;

import com.blochstech.bitcoincardterminal.DataLayer.GenericFileCache;
import com.blochstech.bitcoincardterminal.Utils.SimpleWebResponse;
import com.blochstech.bitcoincardterminal.Utils.Tags;
import com.blochstech.bitcoincardterminal.Utils.WebUtil;

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
		if(urlCache == null){
			
			try {
				urlCache = new GenericFileCache<LinkedList<String>>(urlCachePath, new LinkedList<String>());
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
			servers = StratumServer.convertServersFromStrings(urlCache.get(holder));
			urlCache.Close(holder);
		}catch (Exception ex){
			urlCache.Close(holder);
			if(Tags.DEBUG)
				Log.e(Tags.APP_TAG, "Failed to get stratum servers from disk. Hardcoded values will be used initially. Exception: " + ex.toString());
		}
		
		//Initial servers:
		if(servers == null)
			servers = new LinkedList<StratumServer>();
		if(servers.size() < 4){
			AddServersFromOnlineSource();
			
			if(servers.size() == 0){ //Hardcoded fallback
				servers.add(new StratumServer("electrum.mindspot.org")); //Up again
				servers.add(new StratumServer("electrum.bitfuzz.nl")); //Up again
				servers.add(new StratumServer("VPS.hsmiths.com")); //Up again
				servers.add(new StratumServer("electrum.no-ip.org")); //New
			}
		}
	}
	
	private void AddServersFromOnlineSource(){
		//Get updated servers list from BlochsTech.com/StratumServers.json
		try{
			AndroidHttpClient client = AndroidHttpClient.newInstance("BOBC-0 Terminal/0.0/Android");
			SimpleWebResponse resp = WebUtil.SimpleHttpGet(client, "http://blochstech.com/content/StratumServers.txt", "text/plain", "StratumServerManager_GetBlochsTechServerList");
			if(resp.IsConnected && resp.Response != null){
				JSONObject json = new JSONObject(resp.Response);
				JSONArray jsonServers = json.getJSONArray("Servers");
				String url;
				for(int i = 0; i < jsonServers.length(); i++){
					url = jsonServers.getString(i);
					if(!ContainsServer(url))
						servers.add(new StratumServer(url));
				}
			}
		}catch(Exception ex){
			if(Tags.DEBUG)
				Log.e(Tags.APP_TAG, "Failed to get StratumServers from online source. Ex: " + ex.toString());
		}
	}
	
	private boolean ContainsServer(String url){
		if(url == null || servers == null)
			return false;
		
		for(int i = 0; i < servers.size(); i++){
			if(servers.get(i).url.equalsIgnoreCase(url))
				return true;
		}
		
		return false;
	}
	
	public String GetServer(){
		if(servers == null)
			return null;
		
		if(servers.size() < 4)
			AddServersFromOnlineSource();
		
		StratumServer tempRes = null;
		int minErrOrDisconnects = 50000; //1st criteria (errors + disconnects)
		long latestUpTime = 0; //2nd criteria (because it doesn't change at failure, would loop forever...)
		
		StratumServer tempSrv;
		for(int i = 0; i < servers.size(); i++){
			tempSrv = servers.get(i);
			if(tempSrv == null)
				continue;
				
			if(tempSrv.noConnectionCount+tempSrv.exceptionCount <= minErrOrDisconnects){
				if(tempSrv.noConnectionCount+tempSrv.exceptionCount == minErrOrDisconnects 
						&& tempSrv.lastUpTime < latestUpTime)
					continue;
				
				latestUpTime = tempSrv.lastUpTime;
				minErrOrDisconnects = tempSrv.noConnectionCount+tempSrv.exceptionCount;
				tempRes = tempSrv;
				indexUsed = i;
			}
		}
		
		return tempRes.url;
	}
	
	public void ServerIssue(boolean exception){
		if(indexUsed >= servers.size())
			return;
		
		StratumServer server = servers.get(indexUsed);
		if(exception)
			server.exceptionCount++;
		else
			server.noConnectionCount++;
		
		if((server.exceptionCount > StratumServer.EXCEPTION_MAX || server.noConnectionCount > StratumServer.DISCONNECT_MAX) 
				&& System.currentTimeMillis() - server.lastUpTime > 
				StratumServer.EXPIRE_AFTER_FAILING_AFTER_TIME_INTERVAL_DAYS * 1000 * 60 * 60 * 24)
			servers.remove(indexUsed);
		else
			servers.set(indexUsed, server);
		
		SaveServers();
	}
	
	public void ServerSuccess(){
		if(indexUsed >= servers.size())
			return;
		
		StratumServer server = servers.get(indexUsed);
		server.lastUpTime = System.currentTimeMillis();
		if(server.noConnectionCount > 0)
			server.noConnectionCount--;
		if(server.exceptionCount > 0)
			server.exceptionCount--;
		
		servers.set(indexUsed, server);
		SaveServers();
	}
	
	public void SaveServers(){
		Object holder = new Object();
		try{
			urlCache.Open(holder);
			urlCache.set(holder, StratumServer.convertServersToStrings(servers));
			urlCache.Close(holder);
		}catch (Exception ex){
			urlCache.Close(holder);
			if(Tags.DEBUG)
				Log.e(Tags.APP_TAG, "Failed to save stratum servers to disk. Hardcoded or old values will be used initially. Exception: " + ex.toString());
		}
	}
}
