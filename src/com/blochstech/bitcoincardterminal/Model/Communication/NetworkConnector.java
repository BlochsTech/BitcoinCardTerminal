package com.blochstech.bitcoincardterminal.Model.Communication;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedList;

import org.apache.http.HttpResponse;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONObject;

import com.blochstech.bitcoincardterminal.DataLayer.GenericFileCache;
import com.blochstech.bitcoincardterminal.Stratum.StratumConnector;
import com.blochstech.bitcoincardterminal.Utils.Event;
import com.blochstech.bitcoincardterminal.Utils.SimpleTuple;
import com.blochstech.bitcoincardterminal.Utils.Tags;
import com.blochstech.bitcoincardterminal.Utils.ByteConversionUtil;
import com.blochstech.bitcoincardterminal.Utils.WebUtil;

import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.util.Log;

//http://developer.android.com/reference/android/net/http/AndroidHttpClient.html#newInstance(java.lang.String)
//http://www.mkyong.com/webservices/jax-rs/restful-java-client-with-apache-httpclient/
//http://stackoverflow.com/questions/4457492/simple-http-client-example-in-android
//http://stackoverflow.com/questions/4307380/parsing-json-response-in-android

//This is a temporary solution. Connecting to blockchain in this manor is not good. However this is freeware.

//TODO: Split up the networking stuff could be done same as NFCWrapper. (whether that is a good idea depends on speed of parsing information vs. networking though)
//Also Make it all more robust, handle timeouts and exceptions.
//However, NOT in free version 0.1
class NetworkConnector {
	//Client/worker:
	private final String UserAgent = "BOBC-0 Terminal/0.0/Android";
	private AndroidHttpClient client;
	private Thread workerThreadReference = null;
	
	//Networking state:
	private TXInfoStateObject state = new TXInfoStateObject();
	
	private GenericFileCache<LinkedList<String>> txCache;
	private final String txCachePath = "/BitcoinTerminal/Transactions/txPublishQueue.bin";
	
	private Event<Callback> callbackEvent;
	private Object fireKey = new Object();
	
	NetworkConnector() {
		client = AndroidHttpClient.newInstance(UserAgent);
		callbackEvent = new Event<Callback>(fireKey);
		try {
			txCache = new GenericFileCache<LinkedList<String>>(txCachePath, new LinkedList<String>());
		} catch (Exception e) {
			txCache = null;
			if(Tags.DEBUG)
				Log.e(Tags.APP_TAG, "NetworkConnector could not instantiate cache, transactions will "
					+ "be lost if they cannot immediately be relayed. " + e.getMessage());
		}

		new NetworkingTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Object[])null);
	}
	
	void getUnspentTXs(ArrayList<String> addresses, ArrayList<TXSourceId> knownSources){
		//Update state so that networkTask sees it.
		Enter();

		boolean newCard = state.addresses == null;
		if(!newCard){
			for(int i = 0; i < addresses.size(); i++){
				if(!state.addresses.contains(addresses.get(i))){
					newCard = true;
				}
			}
		}
		if(newCard){
			state.newCardClear();
		}else if(TXSourceUtil.containsSource(knownSources, state.txHash)){ //Clear current tx data state if already known to card:
			state.finishedGettingSourceClear();
		}else{ //Let network connector resend information:
			state.unspentTXDataRequested = true;
		}
		
		state.noUsableNewSources = false; //Set to false in case a retry is wanted.
		state.addresses = addresses;
		state.knownSources = knownSources;
		
		if(workerThreadReference != null)
			workerThreadReference.interrupt();
		
		Leave();
	}
	
	/*void updateKnownSources(TXSourceId knownSource){ //TODO: NOT USED, should it be? Yah I think.
		//TODO: If used, check if it works.
		Enter();
		
		state.finishedGettingSourceClear(); //This makes readyForTX state unnecessary.
		state.knownSources.add(knownSource);
		
		if(workerThreadReference != null)
			workerThreadReference.interrupt();
		
		Leave();
	}*/
	
	void publishTX(LinkedList<Byte> txBytes){
		Enter();
		try{
			if(txCache != null){
				txCache.Open(this);
				
				LinkedList<String> txQueue = txCache.get(this);
				txQueue.add(ByteConversionUtil.byteArrayToHexString(ByteConversionUtil.toBytes(txBytes)));
				txCache.set(this, txQueue);
				
				txCache.Close(this);
			}else{
				if(Tags.DEBUG)
					Log.e(Tags.APP_TAG, "TxCache was null.");
			}
		} catch (Exception e) {
			txCache.Close(this);
			if(Tags.DEBUG)
				Log.e(Tags.APP_TAG, "NetworkConnector could not open txCache. " + e.getMessage());
		}
		Leave();
	}
	
	public Event<Callback> CallbackEventReference() {
		return callbackEvent;
	}
	
	private Thread holder = null;
	private synchronized void Enter() //Allow same thread re-entry.
	{
		long startTime = System.currentTimeMillis();
		
		while(holder != null && holder != Thread.currentThread())
		{
			try {
				this.wait();
			} catch (InterruptedException e) {}
			if(System.currentTimeMillis() - startTime > 20000)
			{
				if(Tags.DEBUG)
					Log.e(Tags.APP_TAG, "FATAL: NetworkConnector was deadlocked for 20 seconds.");
			}
		}
		holder = Thread.currentThread();
	}
	private synchronized void Leave() 
	{
		if(holder == Thread.currentThread())
		{
			holder = null;
			notifyAll();
		}
		else
		{
			if(Tags.DEBUG)
				Log.e(Tags.APP_TAG, "ERROR: Non lock holder tried to call Leave in NetworkConnector.");
		}
	}
	
	//Worker thread code:
	private class EventUpdate{
		EventUpdate(Callback callback){
			this.callback = callback;
		}
		Callback callback;
	}
	
	private class NetworkingTask extends AsyncTask<Object, EventUpdate, Object> { //Start in CardModel constructor.
		private BufferedReader br;
		private String query, line, result;
		private HttpGet getRequest;
		private HttpPost postRequest;
		private HttpResponse response;
		private JSONObject json;
		private JSONArray jsonArray;
		private SimpleTuple<Boolean,Object[]> objArray;
		private SimpleTuple<Boolean,String[]> strArray;
		
		private void NoNewSourcesCode(){
			state.noUsableNewSources = true; //Included in waitingForData check.
			publishProgress(new EventUpdate(new Callback(NetworkCallbackMethods.TXData.Value(), (Object[])null)));
			//Returns null TXData, subscriber should understand this means no new sources.
		}
		private void DisconnectedCode(){
			state.noUsableNewSources = true;
			publishProgress(new EventUpdate(new Callback(NetworkCallbackMethods.NoConnection.Value(), (Object[])null)));
		}
		
		protected Object doInBackground(Object... startParams) {
		    boolean loop = true;
		    
		    while(loop){
		    	Enter();
		    		workerThreadReference = Thread.currentThread();
			    	
		    		//Getting sources:
		    		if(state.waitingForCardData()){
		    			try{
		    				if(state.addresses != null && state.addresses.size() > 0 && state.txHash == null){
		    					strArray = getTXIds();
		    					if(strArray.SecondValue != null){
		    						state.txHash = strArray.SecondValue[0];
		    						state.txIndex = strArray.SecondValue[1];
		    					}else if (strArray.FirstValue){
		    						NoNewSourcesCode();
		    					}else{
		    						DisconnectedCode();
		    					}
		    				}
		    				
		    				if(state.txHash != null){
		    					objArray = getTX();
		    					
		    					if(objArray.SecondValue != null){
			    					state.txJSON = (JSONObject) objArray.SecondValue[0];
			    					state.txRaw = (byte[]) objArray.SecondValue[1];
			    					if(Tags.DEBUG)
			    						Log.i(Tags.APP_TAG, state.txHash);
			    					if(Tags.DEBUG)
			    						Log.i(Tags.APP_TAG, state.txJSON.toString());
		    					}else if(objArray.FirstValue){
		    						NoNewSourcesCode();
		    					}else{
		    						DisconnectedCode();
		    					}
		    				}
		    				
		    				//if(state.txJSON != null){
		    				if(state.txHash != null && state.txJSON != null)
		    				{
		    					//Do BlockResponse call (reusing current network thread)
		    					BlockResponse blockResponse = StratumConnector.GetMekleBranch(state.txHash, state.txJSON.getString("block_height"));
		    					
		    					if(blockResponse.HasConnection && blockResponse.TXIsInABlock){
		    						state.blockData = blockResponse;
			    					//1 Get bytes:
			    					state.blockRaw = getRawBlockHeader(blockResponse); //Offline method.
			    					//2 Get hashses:
			    					state.hashes = blockResponse.MerkleBranch; //Offline method.
			    				}else if(blockResponse.HasConnection){
		    						NoNewSourcesCode();
		    					}else{
		    						DisconnectedCode();
		    					}
	    					}
		    				
		    				if(state.hashes != null && state.blockRaw != null){
		    					state.unspentTXDataRequested = false;
		    					publishProgress(new EventUpdate(new Callback(NetworkCallbackMethods.TXData.Value(), new Object[]{state.blockRaw, state.hashes, state.txRaw,
		    						state.txHash})));
		    					//state.readyForTX = false; -> No longer used.
		    				}
		    			}catch(UnknownHostException netEx){
		    				DisconnectedCode();
		    			}catch(Exception ex){
		    				state.newCardClear();
		    				if(Tags.DEBUG)
		    					Log.e(Tags.APP_TAG, ex != null ? ex.toString() : "Unknown null exception in NetworkConnector - see log cat.");
		    			}
		    		}else if(state.unspentTXDataRequested && !TXSourceUtil.containsSource(state.knownSources, state.txHash)){ //In case card disconnects send same TX again:
			    		try{
			    			state.unspentTXDataRequested = false;
		    				if(state.hashes != null && state.blockRaw != null){
		    					publishProgress(new EventUpdate(new Callback(NetworkCallbackMethods.TXData.Value(), new Object[]{state.blockRaw, state.hashes, state.txRaw,
		    						state.txHash})));
		    					//state.readyForTX = false; -> No longer used.
		    				}
			    		}catch(Exception ex){
		    				state.newCardClear();
		    				if(Tags.DEBUG)
		    					Log.e(Tags.APP_TAG, ex != null ? ex.toString() : "Unknown null exception in NetworkConnector - see log cat.");
		    			}
		    		}else if(state.unspentTXDataRequested){
		    			NoNewSourcesCode();
		    		}
		    		
		    		//Publishing TXs:
		    		if(txCache != null){
		    			try {
		    				String txHex = null;
		    				NetworkPublishResults res = new NetworkPublishResults();
							
		    				txCache.Open(this);
		    				LinkedList<String> txQueue = txCache.get(this);
							if(txQueue.size() > 0){
								txHex = txQueue.remove();
								res = sendTransaction(txHex);

								publishProgress(new EventUpdate(new Callback(NetworkCallbackMethods.TXSendResult.Value(), new Object[]{res.Status, 
										res.Message})));
								
								if(res.Status != NetworkPublishResults.SendStatus.Retry)
									txCache.set(this, txQueue); //Only update queue, with head item now removed, if tx sent.
							}
							txCache.Close(this);
							
							if(res.Status == NetworkPublishResults.SendStatus.Invalid){
								if(Tags.DEBUG)
									Log.e(Tags.APP_TAG, "Published Tx was invalid, this issue will not be handled further in this"
										+ " version of the terminal program.");
							}
							
						} catch (Exception e) {
							txCache.Close(this);
							if(Tags.DEBUG)
								Log.e(Tags.APP_TAG, "NetworkConnector could not open txCache. " + e.getMessage());
						}
		    		}
		    		
	    		Leave();
		    		
		    	//If none of the above, leave and sleep.
	    		try {
	    			Thread.sleep(1000); //Interrupts will wake this up.
				} catch (InterruptedException e) { //InterruptedException
				}
		    }
		    return null;
		}
		
		protected void onProgressUpdate(EventUpdate... eventArray) { //("..." means array) Code runs on main thread:
			//Reponse/error/connection:
			if(eventArray != null && eventArray.length > 0){
				EventUpdate event = eventArray[0];
				
				if(event.callback != null){
					callbackEvent.fire(fireKey, event.callback);
				}
			}
		}
	
		protected void onPostExecute(Object endResult) { //Code runs on main thread:
			//Do nothing, for now.
		}
		
		//Return true if tx should be removed from queue - ie. either okay or invalid. Return false at no connection etc..
		private NetworkPublishResults sendTransaction(String txHex) throws IOException{
			postRequest = new HttpPost("https://blockchain.info/pushtx");
			postRequest.addHeader("accept", "text/html");

			ArrayList<BasicNameValuePair> nameValuePair = new ArrayList<BasicNameValuePair>(1);
			nameValuePair.add(new BasicNameValuePair("tx", txHex));
			
			try{
				postRequest.setEntity(new UrlEncodedFormEntity(nameValuePair));
			}catch(Exception ex){
				return new NetworkPublishResults(NetworkPublishResults.SendStatus.Invalid, "Failed to encode TX post. " + ex.getMessage());
			}
			
			response = client.execute(postRequest);
			
			boolean failed = false;
			String failString = null;
			if (response.getStatusLine().getStatusCode() != 200 && //Ok
					response.getStatusLine().getStatusCode() != 522) { //Server busy
				failString = WebUtil.HttpResponseLog(response, "NetworkConnector.sendTransaction"); //This consumes data, hence bool to avoid reuse of content.
				failed = true;
			}else if(response.getStatusLine().getStatusCode() == 522){ //Server busy
				return new NetworkPublishResults(NetworkPublishResults.SendStatus.Retry, "Server busy, will retry. "
						   + response.getStatusLine().getStatusCode());
			}
			
			String tempStr;
			if(!failed){
				br = new BufferedReader(
	                    new InputStreamReader((response.getEntity().getContent())));
	
				StringBuilder sb = new StringBuilder(500000); //0.5 megabyte, updgrade of all net code needed later - severely needed.
				sb.append("");
				while ((line = br.readLine()) != null) {
					sb.append(line);
				}
				
				tempStr = sb.toString(); //TODO
				tempStr = tempStr == null ? "" : tempStr;
			}else{
				tempStr = failString;
			}
			
			if(failed){
				return new NetworkPublishResults(NetworkPublishResults.SendStatus.Invalid, "API error: " + tempStr + " TXHex:" + txHex);
			}else{
				if(Tags.DEBUG)
					Log.i(Tags.APP_TAG, "API: " + tempStr);
			}
			
			state.newCardClear(); //Clear all data that may have been used/no longer valid.
			
			return new NetworkPublishResults(NetworkPublishResults.SendStatus.OK, null);
		}
		
		private SimpleTuple<Boolean,String[]> getTXIds() throws Exception{
			if(state.txIds == null){
				query = "";
				for(int i = 0; i < state.addresses.size(); i++){
					query = i == 0 ? state.addresses.get(i) : query + "|" + state.addresses.get(i);
				}
				getRequest = new HttpGet(
						"https://blockchain.info/unspent?active=" + URLEncoder.encode(query, "UTF-8"));
				getRequest.addHeader("accept", "application/json");
				
				int retry = 0;
				while(retry < 3){
					retry++;
					response = client.execute(getRequest);
					
					int statusCode = response.getStatusLine().getStatusCode();
					if(statusCode == 500){
						client.getConnectionManager().shutdown();
						client = AndroidHttpClient.newInstance(UserAgent);
						return new SimpleTuple<Boolean,String[]>(true, null); //Connection true for this 500 and false everywhere else because 500 is returned here if no unspent sources.
					}else if(statusCode == 522 || statusCode == 429){
						client.getConnectionManager().shutdown();
						client = AndroidHttpClient.newInstance(UserAgent);
					}else if (statusCode != 200) {
						WebUtil.HttpResponseLog(response, "NetworkConnector.getTXIds");
						return new SimpleTuple<Boolean,String[]>(false, null);
					}else{
						break;
					}
				}
				
				br = new BufferedReader(
		                         new InputStreamReader((response.getEntity().getContent())));
		 
				result = "";
				while ((line = br.readLine()) != null) {
					result = result + line;
				}

				state.txIds = new ArrayList<String[]>();
				json = new JSONObject(result);
				jsonArray = json.getJSONArray("unspent_outputs");
				for(int i = 0; i < jsonArray.length(); i++){
					state.txIds.add(new String[]{jsonArray.getJSONObject(i).getString("tx_hash"), jsonArray.getJSONObject(i).getString("tx_index")});
				}
			}
			if(state.txIds != null){
				for(int i = 0; i < state.txIds.size(); i++){
					if(!TXSourceUtil.containsSource(state.knownSources, state.txIds.get(i)[0]))
						return new SimpleTuple<Boolean,String[]>(true, state.txIds.get(i));
				}
			}
			return new SimpleTuple<Boolean,String[]>(true, null);
		}
		
		private SimpleTuple<Boolean,Object[]> getTX() throws Exception{
			query = state.txIndex;
			
			for(int i = 0; i < 2; i++){
				if(i==0){
					getRequest = new HttpGet(
						"https://blockchain.info/tx-index/" + URLEncoder.encode(query, "UTF-8") + "?format=json&scripts=true");
				}else{
					getRequest = new HttpGet(
						"https://blockchain.info/tx-index/" + URLEncoder.encode(query, "UTF-8") + "?format=hex&scripts=true");
				}
				getRequest.addHeader("accept", "application/json");
				
				int retry = 0;
				while(retry < 3){
					retry++;
					response = client.execute(getRequest);
					
					int statusCode = response.getStatusLine().getStatusCode();
					if(statusCode == 500){
						client.getConnectionManager().shutdown();
						client = AndroidHttpClient.newInstance(UserAgent);
						return new SimpleTuple<Boolean,Object[]>(false, null);
					}else if (statusCode == 522 || statusCode == 429) {
						client.getConnectionManager().shutdown();
						client = AndroidHttpClient.newInstance(UserAgent);
					}else if (statusCode != 200) {
						WebUtil.HttpResponseLog(response, "NetworkConnector.getTX");
						return new SimpleTuple<Boolean,Object[]>(false, null);
					}else{
						break;
					}
				}
		 
				br = new BufferedReader(
		                         new InputStreamReader((response.getEntity().getContent())));
		 
				result = "";
				while ((line = br.readLine()) != null) {
					result = result + line;
				}
				
				if(i==0)
					json = new JSONObject(result);
			}
			
			return new SimpleTuple<Boolean,Object[]>(true, new Object[]{json, ByteConversionUtil.hexStringToByteArray(result)});
		}
	
		/*private BlockResponse getJSONBlock() throws Exception{
			if(!state.txJSON.has("block_height")){ //Sorts away unconfirmed sources - we need at least 1 block to have merkle tree and difficulty etc.. 
				return new BlockResponse(true, false);
			}
			
			query = state.txJSON.getString("block_height");
			getRequest = new HttpGet(
					"https://blockchain.info/block-height/" + URLEncoder.encode(query, "UTF-8") + "?format=json");
			getRequest.addHeader("accept", "application/json");
			
			response = client.execute(getRequest);
			 
			if(response.getStatusLine().getStatusCode() == 500){
				client.getConnectionManager().shutdown();
				client = AndroidHttpClient.newInstance(UserAgent);
				return new BlockResponse(false, true);
			}else if (response.getStatusLine().getStatusCode() != 200) {
				if(Tags.DEBUG)
					Log.e(Tags.APP_TAG, "Failed : HTTP error code : "
				   + response.getStatusLine().getStatusCode());
				return new BlockResponse(false, true);
			}
	 
			br = new BufferedReader(
	                         new InputStreamReader((response.getEntity().getContent())));
	 
			boolean inMainBlock = false, inBlock = false;
			boolean readMore = true, endReached = false, containsBlockEnd = false, resetMerkleCreator = true;
			int textBlockSize = 120000;
			int minCount = textBlockSize, blockEndIndex;
			String tempString = "", hash1, hash2;
			LinkedList<Match> matches;
			StringBuilder sb = new StringBuilder(textBlockSize*10);
			BlockResponse blockData = new BlockResponse(true, true);
			while ((line = br.readLine()) != null || readMore) {
				//1. Add read line:
				sb.append(line);
				
				if(sb.length()>minCount || line == null){
					
					//2. Check if we have a block header and it is main:
					if(inBlock == false && sb.length() > 200 && RegexUtil.isMatch(sb.toString(), "\\{.*?prev_block[^\\[]*\\["))
						//Matches any block header until tx start.
					{
						inBlock = true;
						tempString = sb.toString();
						//RegexSubstring needed.
						if(RegexUtil.isMatch(tempString, "\"main_chain\"\\s*:\\s*true,"))
							inMainBlock = true;
						
						if(inMainBlock){
							blockData.Version = ParseHelper.RegexSubstring(tempString, "\"ver\"\\s*:\\s*", ",");
							blockData.Bits = ParseHelper.RegexSubstring(tempString, "\"bits\"\\s*:\\s*", ",");
							blockData.Time = ParseHelper.RegexSubstring(tempString, "\"time\"\\s*:\\s*", ",");
							blockData.MerkleRoot = ParseHelper.RegexSubstring(tempString, "\"mrkl_root\"\\s*:\\s*\"", "\",");
							blockData.Nonce = ParseHelper.RegexSubstring(tempString, "\"nonce\"\\s*:\\s*", ",");
							blockData.PreviousBlock = ParseHelper.RegexSubstring(tempString, "\"prev_block\"\\s*:\\s*\"", "\",");
						}
						
						//Clear block string only:
						if(RegexUtil.isMatch(tempString, "\"tx\""))
						{
							sb.delete(0, tempString.indexOf("\"tx\"")); //Avoid accidentally clearing Tx data.
						}else{
							sb.delete(0, sb.length());
						}
						minCount = 0;
					}
					
					//3. Check if we have Tx pair:
					if(inMainBlock){
						tempString = sb.toString();
	
						//BlockEnd
						blockEndIndex = RegexUtil.getIndex(tempString, "([\\s\\n\\r]*\\}{1}\\]{1}){2}", false, 0, 0);
						endReached = blockEndIndex != -1;
						
						if(endReached){ //Make sure we do not use TXs from next block:
							tempString = tempString.substring(0, blockEndIndex);
						}
						
						matches = RegexUtil.getMatches(tempString, "\"hash\"\\s*:\\s*\"[0-9a-f]{64}\"");
						//boolean twoHashes = RegexUtil.countMatches(tempString, "\"hash\":\"[0-9a-f]{64}\"") > 1; //TODO: Replace with new code, certainly should not count more than two. OR:
						//Make regex that gets all the values in one go, then loop through them.!
						if(matches.size() >= 2 || endReached)
						{ //Two Txs are in tempstring minimum or at end.
							for(int i = 0; i < matches.size(); i += 2){
								if(i < matches.size()-1){ //Hash pair:
									hash1 = matches.get(i).value;
									hash2 = matches.get(i+1).value;
								}else if(endReached){ //Last single hash:
									hash1 = matches.get(i).value;
									hash2 = hash1; //Correct?
								}else{
									break; //Leave the last hash for next round.
								}
								hash1 = hash1.substring(hash1.indexOf(":\"")+2, hash1.length()-1);
								hash2 = hash2.substring(hash2.indexOf(":\"")+2, hash2.length()-1);
								
								getHashesByStream(
										hash1, 
										hash2,
										state.txHash,
										resetMerkleCreator,
										false
										);
								resetMerkleCreator = false;
							}
							
							if(!endReached){
								if(matches.size() % 2 == 0 && matches.size()>0){
									sb.delete(0, matches.get(matches.size()-1).end);
								}else if(matches.size()>1){
									sb.delete(0, matches.get(matches.size()-2).end);
								}
								minCount = textBlockSize;
							}
							
							/*while(twoHashes) //TODO: Handle the last single hash!
							{
								getHashesByStream(
										ParseHelper.RegexSubstring(tempString, "\"hash\"\\s*:\\s*\"", "\",", true, 0), //TODO: Make new faster regex that checks and gets values at the same time.
										ParseHelper.RegexSubstring(tempString, "\"hash\"\\s*:\\s*\"", "\",", true, 1),
										state.txHash,
										RegexUtil.isMatch(tempString, "\"tx\""),
										endReached
										);
		
								//Cut used TXs:
								if(!endReached){
									sb.delete(0, RegexUtil.getIndex(tempString, "\"hash\"", true, 0, 1));
									minCount = 0;
								}
								tempString = sb.toString();
								twoHashes = RegexUtil.countMatches(tempString, "\"hash\":\"[0-9a-f]{64}\"") > 1;
							}* /
						}else{
							minCount = minCount + textBlockSize;
						}
						
						//Exit main while loop if mainBlock ended:
						readMore = !endReached;
					}else if(inBlock){ //If not MAIN block we want to make sure that we chuck the unwanted data.
						if(sb.length() > textBlockSize){
							tempString = sb.toString();
							containsBlockEnd = RegexUtil.isMatch(tempString, "([\\n\\r\\s]*\\}{1}\\]{1}[\\n\\r\\s]*){2}");
							if(RegexUtil.isMatch(tempString, "[^\\n\\r\\s\\}\\]]+(\\Z)") && !containsBlockEnd){
								sb.delete(0, sb.length()); //Chuck data.
							}else if(containsBlockEnd){
								inBlock = false; //Start cycle of reading data again.
							}
						}
					}
				}
			}
			
			if(blockData.MerkleBranch == null)
			{
				blockData.MerkleBranch = ByteConversionUtil.MerkleListToArray(getHashesByStream(null, null, state.txHash, false, true));
			}
			
			return blockData;
			
			/*json = new JSONObject(sb.toString()); // out of memory happens here -> Solved as shown above by using data as it comes in and then discarding all but the necessary.
			jsonArray = json.getJSONArray("blocks");
			for(int i = 0; i < jsonArray.length(); i++){
				if(jsonArray.getJSONObject(i).getBoolean("main_chain")){
					return new SimpleTuple<Boolean,JSONObject>(true, jsonArray.getJSONObject(i));
				}
			}
			
			throw new Exception("Unexpected outcome, check logic.");* /
		}*/
		
		private byte[] getRawBlockHeader(BlockResponse header) throws Exception{
			byte[] result = new byte[80];
			byte[] tmpByte;
			
			tmpByte = ByteConversionUtil.leIntToBytes(Long.parseLong(header.Version));
			for(int i = 0; i < 4; i++){
				result[i] = tmpByte[i];
			}
			
			tmpByte = ByteConversionUtil.hexStringToByteArray(ByteConversionUtil.reverseByteOrder(header.PreviousBlock));
			for(int i = 0; i < 32; i++){
				result[i+4] = tmpByte[i];
			}
			
			tmpByte = ByteConversionUtil.hexStringToByteArray(ByteConversionUtil.reverseByteOrder(header.MerkleRoot));
			for(int i = 0; i < 32; i++){
				result[i+36] = tmpByte[i];
			}
			
			tmpByte = ByteConversionUtil.leIntToBytes(Long.parseLong(header.Time));
			for(int i = 0; i < 4; i++){
				result[i+68] = tmpByte[i];
			}
			
			tmpByte = ByteConversionUtil.leIntToBytes(Long.parseLong(header.Bits));
			for(int i = 0; i < 4; i++){
				result[i+72] = tmpByte[i];
			}
			
			tmpByte = ByteConversionUtil.leIntToBytes(Long.parseLong(header.Nonce));
			for(int i = 0; i < 4; i++){
				result[i+76] = tmpByte[i];
			}
			
			return result;
		}
		
		//OBSOLETE METHOD, HANDLES LARGE BLOCKS BADLY.
		@SuppressWarnings("unused")
		private MerkleBranchElement[] getHashes(JSONObject block, String txhash) throws Exception{
			jsonArray = block.getJSONArray("tx");
			int levels = ByteConversionUtil.merkleLevels(jsonArray.length());
			MerkleBranchElement[] result = new MerkleBranchElement[levels];
			ArrayList<String> prevRow = new ArrayList<String>();
			ArrayList<String> nextRow = new ArrayList<String>();
			String specialHash = txhash;
			String concatHashes = "", firstHash = "", secondHash = "";
			
			if(jsonArray.length() < 2){
				return new MerkleBranchElement[0]; //The merkle root will be simply the txhash itself (case tx count = 1). There is always one at least; the coinbase tx with the miners fee.
			}
			
			for(int i = 0; i < levels; i++){
				if(i==0){
					for(int j = 0; j < jsonArray.length(); j=j+2){
						firstHash = ByteConversionUtil.reverseByteOrder(jsonArray.getJSONObject(j).getString("hash"));
						if(j+1 < jsonArray.length()){
							secondHash = ByteConversionUtil.reverseByteOrder(jsonArray.getJSONObject(j+1).getString("hash"));
						}else{
							secondHash = firstHash;
						}
						concatHashes = firstHash + secondHash;
						concatHashes = ByteConversionUtil.byteArrayToHexString(ByteConversionUtil.doubleSHA256(ByteConversionUtil.hexStringToByteArray(concatHashes)));
						
						if(firstHash.equals(specialHash)){
							result[i] = new MerkleBranchElement(secondHash, true,firstHash);
							specialHash = concatHashes;
						}else if(secondHash.equals(specialHash)){
							result[i] = new MerkleBranchElement(firstHash, false, secondHash);
							specialHash = concatHashes;
						}
						
						nextRow.add(concatHashes);
						
						//More after If clause.
					}
				}else{
					for(int j = 0; j < prevRow.size(); j=j+2){
						firstHash = prevRow.get(j);
						if(j+1 < prevRow.size()){
							secondHash = prevRow.get(j+1);
						}else{
							secondHash = firstHash;
						}
						concatHashes = firstHash + secondHash;
						concatHashes = ByteConversionUtil.byteArrayToHexString(ByteConversionUtil.doubleSHA256(ByteConversionUtil.hexStringToByteArray(concatHashes)));
						
						if(firstHash.equals(specialHash)){
							result[i] = new MerkleBranchElement(secondHash, true, firstHash);
							specialHash = concatHashes;
						}else if(secondHash.equals(specialHash)){
							result[i] = new MerkleBranchElement(firstHash, false, secondHash);
							specialHash = concatHashes;
						}
						
						nextRow.add(concatHashes);
					
						//More after If clause.
					}
				}
				prevRow = nextRow;
				nextRow = new ArrayList<String>();
			}
			//String testTxHash = ByteConversionUtil.byteArrayToHexString(ByteConversionUtil.doubleSHA256(state.txRaw));
			return result;
		}
		
		//Will be same as getHashes method with one exception:
		//1. Two hashes only are given at a time.
		//2. prevRow is built using these and txes are counted.
		//3. Once done, execute as usual.
		/*private LinkedList<MerkleBranchElement> merkleBranch = new LinkedList<MerkleBranchElement>(); //TODO: Generic array converter in utils?
		private ArrayList<String> prevRow = new ArrayList<String>();
		private ArrayList<String> nextRow = new ArrayList<String>();
		private int level;
		private String specialHash;
		private String concatHashes = "", firstHash = "", secondHash = "";
		private boolean branchComplete;
		
		private LinkedList<MerkleBranchElement> getHashesByStream(String leftHash, String rightHash, String txhash, boolean newCallStart, boolean computeBranch) throws Exception{
			try{
				if(newCallStart && (rightHash == null
						|| leftHash.equalsIgnoreCase(rightHash))){
					merkleBranch = new LinkedList<MerkleBranchElement>();
					branchComplete = true;
					return merkleBranch; //The merkle root will be simply the txhash itself (case tx count = 1). There is always one at least; the coinbase tx with the miners fee.
				}
				
				if(newCallStart)
				{
					specialHash = txhash;
					level = 0;
					branchComplete = false;
					prevRow = new ArrayList<String>();
					nextRow = new ArrayList<String>();
					merkleBranch = new LinkedList<MerkleBranchElement>();
				}
	
				if(computeBranch){ //Signals end of tx stream from block.
					prevRow = nextRow;
					nextRow = new ArrayList<String>();
					level++;
				}
				
				if(level == 0 && !branchComplete){
					
					//Hash the concatenated hashes and immediately create nextRow:
					firstHash = ByteConversionUtil.reverseByteOrder(leftHash);
					if(rightHash != null){
						secondHash = ByteConversionUtil.reverseByteOrder(rightHash);
					}else{
						secondHash = firstHash;
					}
					concatHashes = firstHash + secondHash;
					concatHashes = ByteConversionUtil.byteArrayToHexString(ByteConversionUtil.doubleSHA256(ByteConversionUtil.hexStringToByteArray(concatHashes)));
					
					if(firstHash.equals(specialHash)){
						merkleBranch.add(new MerkleBranchElement(secondHash, true, firstHash));
						specialHash = concatHashes;
					}else if(secondHash.equals(specialHash)){
						merkleBranch.add(new MerkleBranchElement(firstHash, false, secondHash));
						specialHash = concatHashes;
					}
	
					nextRow.add(concatHashes);
				}
				
				while(level > 0 && !branchComplete){
					for(int j = 0; j < prevRow.size(); j=j+2){
						level++;
						firstHash = prevRow.get(j);
						if(j+1 < prevRow.size()){
							secondHash = prevRow.get(j+1);
						}else{
							secondHash = firstHash;
						}
						concatHashes = firstHash + secondHash;
						concatHashes = ByteConversionUtil.byteArrayToHexString(ByteConversionUtil.doubleSHA256(ByteConversionUtil.hexStringToByteArray(concatHashes)));
						
						if(firstHash.equals(specialHash)){
							merkleBranch.add(new MerkleBranchElement(secondHash, true, firstHash));
							specialHash = concatHashes;
						}else if(secondHash.equals(specialHash)){
							merkleBranch.add(new MerkleBranchElement(firstHash, false, secondHash));
							specialHash = concatHashes;
						}
						
						nextRow.add(concatHashes);
					}
					
					branchComplete = nextRow.size() == 1; //Merkle root not added to branch.
					prevRow = nextRow;
					nextRow = new ArrayList<String>();
				}
				
				if (branchComplete){
					return merkleBranch;
				}else{
					return null;
				}
			}catch(Exception ex){
				if(Tags.DEBUG)
					Log.e(Tags.APP_TAG, "Creating merkle branch failed. " + ex.getMessage());
				return null;
			}
		}*/
	
		/*private byte[] getHeader(String hash) throws Exception{
			getRequest = new HttpGet(
					"https://blockchain.info/rawblock/" + URLEncoder.encode(hash, "UTF-8"));
			getRequest.addHeader("accept", "application/json");
			
			response = client.execute(getRequest);
			 
			if (response.getStatusLine().getStatusCode() != 200 && response.getStatusLine().getStatusCode() != 500) {
				Log.e(Tags.APP_TAG, "Failed : HTTP error code : "
				   + response.getStatusLine().getStatusCode());
				return null;
			}else if(response.getStatusLine().getStatusCode() == 500){
				client.getConnectionManager().shutdown();
				client = AndroidHttpClient.newInstance(UserAgent);
				return null;
			}
	 
			br = new BufferedReader(
	                         new InputStreamReader((response.getEntity().getContent())));
	 
			result = "";
			while ((line = br.readLine()) != null) {
				result = result + line;
			}
			
			json = new JSONObject(result);
			return getRawBlockHeader(json);
		} REMOVED AS NOT USED ANY MORE AS MORE DIFFICULTY DOES NOT MAKE SENSE. ... IT DOES YOU JUST WENT THE WRONG WAY!*/
	}
	//End Worker thread code.
}