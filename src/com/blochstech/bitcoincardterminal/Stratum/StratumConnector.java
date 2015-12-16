package com.blochstech.bitcoincardterminal.Stratum;

import java.net.InetSocketAddress;
import java.net.Socket;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.blochstech.bitcoincardterminal.Model.Communication.BlockResponse;
import com.blochstech.bitcoincardterminal.Model.Communication.MerkleBranchElement;
import com.blochstech.bitcoincardterminal.Utils.ByteConversionUtil;
import com.blochstech.bitcoincardterminal.Utils.Event;
import com.blochstech.bitcoincardterminal.Utils.SimpleWebResponse;
import com.blochstech.bitcoincardterminal.Utils.Tags;
import com.blochstech.bitcoincardterminal.Utils.TcpUtil;

import android.util.Log;

public class StratumConnector {
	
	private Event<BlockResponse> BlockResponseEvent;
	Event<BlockResponse> BlockResponseEventReference() {
		return BlockResponseEvent;
	}
	
	private Object fireKey = new Object();
		
	//Client/worker:
	private static StratumServerManager serverManager;
	
	//TCP stuff:
	private static Socket socket;
	
	
	/*private StratumWebTask getWorker(){
		return new StratumWebTask();
	}*/
	
	StratumConnector(){
		BlockResponseEvent = new Event<BlockResponse>(fireKey, 1);
		serverManager = new StratumServerManager();
	}
	
	//Do not call on main thread.
	public synchronized static BlockResponse GetMekleBranch(String txHash, String blockHeight){
		if(serverManager == null)
			serverManager = new StratumServerManager();

		String revTxHash = ByteConversionUtil.reverseByteOrder(txHash);
		
		try{
			SimpleWebResponse res = null;
			SimpleWebResponse headerRes = null;
			
			BlockResponse result = new BlockResponse(false, false);
			
			int tries = 0;
			boolean retry = true;
			
			boolean hasConnection = false, txInBlock = false, wasExeption;
			
			while(tries < 3 && retry){
				tries++;
				retry = false;
				wasExeption = false;
				try{
					if(socket == null){ //This fails on bad servers, handle it.
						socket = new Socket();
						socket.connect(new InetSocketAddress(serverManager.GetServer(), StratumServer.SERVER_PORT), 4000);
					}
					
					res = TcpUtil.SendReceiveTcpMessage(socket, 
							"{ \"id\": 1, \"method\": \"blockchain.transaction.get_merkle\", \"params\": [ \""+revTxHash+"\", "+blockHeight+" ] }\n");
					
					headerRes = TcpUtil.SendReceiveTcpMessage(socket, 
							"{ \"id\": 1, \"method\": \"blockchain.block.get_header\", \"params\": [ "+blockHeight+" ] }\n");
					
					hasConnection = res != null && res.IsConnected && res.Response != null && res.Response.length() > 0
							&& headerRes != null && headerRes.IsConnected && headerRes.Response != null && headerRes.Response .length() > 0;

					txInBlock = hasConnection && res.Response.contains("merkle") &&
							headerRes.Response.contains("prev_block_hash") && !res.Response.contains("error") && !headerRes.Response.contains("error");
					result = new BlockResponse(hasConnection, txInBlock);
	
					retry = !hasConnection || !txInBlock;
				}catch(Exception ex){
					if(Tags.DEBUG)
						Log.w(Tags.APP_TAG, "StratumConnector.GetMekleBranch failed, retrying with new socket. Ex: " + ex.toString());
					
					wasExeption = true;
					retry = true;
				}
				
				if(retry){
					if(socket != null)
						socket.close();
					serverManager.ServerIssue(wasExeption);
					socket = null;
				}
			}
			if(!hasConnection || !txInBlock || headerRes == null || res == null)
				return result;
			
			JSONObject json = new JSONObject(headerRes.Response);
			json = json.getJSONObject("result");
			result.Bits = json.getString("bits");
			result.MerkleRoot = json.getString("merkle_root");
			result.Nonce = json.getString("nonce");
			result.PreviousBlock = json.getString("prev_block_hash");
			result.Time = json.getString("timestamp");
			result.Version = json.getString("version");
			
			json = new JSONObject(res.Response);
			json = json.getJSONObject("result");
			result.MerkleBranch = ConvertToBobcMerkleBranch(revTxHash, result.MerkleRoot, json.getInt("pos"), json.getJSONArray("merkle"));
			if(result.MerkleBranch == null)
				result.TXIsInABlock = false;
			
			return result;
		}catch(Exception ex){
			if(Tags.DEBUG)
				Log.e(Tags.APP_TAG, "StratumConnector.GetMekleBranch call failed. Ex: " + ex.toString());
		}
		
	    return new BlockResponse(false, false);
	}
	
	//Returns null if tx is not in block.
	private static MerkleBranchElement[] ConvertToBobcMerkleBranch(String txHash, String merkleRoot, int pos, JSONArray merkleHashes) throws JSONException{
		MerkleBranchElement[] result = new MerkleBranchElement[merkleHashes.length()];
		
		MerkleBranchElement tmpElement = null;
		String concatHashes, lastHash = ByteConversionUtil.reverseByteOrder(txHash);
		boolean rightSide;
		int divisor, divided;
				
		for(int i = 0; i < merkleHashes.length(); i++){
			divisor = purePow(2, i);
			divided = pos / divisor;
			rightSide = divided % 2 == 0;
			
			tmpElement = new MerkleBranchElement(ByteConversionUtil.reverseByteOrder(merkleHashes.getString(i)), rightSide, lastHash);
			result[i] = tmpElement;
			
			concatHashes = rightSide ? tmpElement.otherHash + tmpElement.hash : tmpElement.hash + tmpElement.otherHash;
			lastHash = ByteConversionUtil.byteArrayToHexString(ByteConversionUtil.doubleSHA256(ByteConversionUtil.hexStringToByteArray(concatHashes)));
		}
		
		if(tmpElement == null)
			return null;

		if(!ByteConversionUtil.reverseByteOrder(merkleRoot).equalsIgnoreCase(lastHash))
			return null;
		
		return result;
	}
	
	private static int purePow(int base, int order){
		int result = 1;
		for(int i = 0; i < order; i++)
			result = result*base;
		
		return result;
	}
	
	//START WORKER - <StartParamsType, ProgressResultType, PostExecuteType> (doInBackground returns to onPostExecute)
	/*private class StratumWebTask extends AsyncTask<String, Void, BlockResponse> {
		
		protected BlockResponse doInBackground(String... startParams) {
			return GetMekleBranch(startParams[0], startParams[1]);
		}
			
		protected void onProgressUpdate(Void... endResult) { //("..." means array) Code runs on main thread:
			//Do nothing.
		}
	
		protected void onPostExecute(BlockResponse end) { //Code runs on main thread:
			BlockResponseEvent.fire(fireKey, end);
		}
	}*/
	//END WORKER
		
	/*public void GetTXBlockInfo(String txHash) throws Exception{
		//TODO: Start web worker. Return void. result in Event.
		if(CallInProgress)
			throw new Exception("Bad StratumConnector usage");
		
		String[] params = new String[2];
		//TXId
		//BlockHeight.
		
		getWorker().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (params));
	}*/
}
