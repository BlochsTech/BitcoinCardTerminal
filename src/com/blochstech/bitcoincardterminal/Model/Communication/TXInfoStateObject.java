package com.blochstech.bitcoincardterminal.Model.Communication;

import java.util.ArrayList;

import org.json.JSONObject;

//TXBytes.
//TX branch data
//Block data
class TXInfoStateObject {
	ArrayList<String> addresses;
	ArrayList<TXSourceId> knownSources;
	boolean unspentTXDataRequested;
	
	ArrayList<String[]> txIds;
	
	String txHash;
	String txIndex;
	JSONObject txJSON;
	byte[] txRaw;
	
	BlockResponse blockData;
	byte[] blockRaw;
	MerkleBranchElement[] hashes; //From unknown "next to" txhash in merkle tree to first unknown merklebranch.
	
	//boolean readyForTX = false; //Standard is false as the other standards are already null. Ready means clearing.
	//This is always false.. what is the purpose? -> Supposed to be set to true when Card has used the provided TXData and is ready for new..
	//Renamed.
	//No longer necessary as update knownSourcesMethod already clears data and restarts process.
	boolean noUsableNewSources = false;
	
	TXInfoStateObject(){
		newCardClear();
	}
	
	void newCardClear(){
		txHash = null;
		addresses = null;
		knownSources = null;
		txIndex = null;
		txJSON = null;
		blockRaw = null;
		hashes = null;
		txRaw = null;
		//readyForTX = false;
		txIds = null;
		noUsableNewSources = false;
		unspentTXDataRequested = false;
		blockData = null;
	}
	
	void finishedGettingSourceClear(){
		txHash = null;
		txIndex = null;
		txJSON = null;
		blockRaw = null;
		hashes = null;
		txRaw = null;
		noUsableNewSources = false;
		unspentTXDataRequested = true;
		blockData = null;
	}

	public boolean waitingForCardData() {
		return (addresses != null
				&& knownSources != null
				&& !noUsableNewSources
				&& (txHash == null || txIndex == null || txJSON == null || blockRaw == null || hashes == null || txRaw == null));
	}
}
