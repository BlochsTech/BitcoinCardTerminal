package com.blochstech.bitcoincardterminal.Model.Communication;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Locale;

import com.blochstech.bitcoincardterminal.Utils.ShortNumberConverter;
import com.blochstech.bitcoincardterminal.Utils.SyntacticSugar;

//Will hold various values and support methods for quick manipulation.
//Used only by BitcoinCard - hence package access.
//Especially addresses and sources.
class BitcoinCardStateObject {
	//TERMINAL CHARGE INFO (will NOT clear on connect, only invalidation or success):
	//Once data below is given simply retry until invalidity is revealed, cancel event from Model or success clears it:
	Double fee = 0.0;
	Double amount = 0.0;
	Double terminalAmount = 0.0;
	
	private String address = null;
	private int[] addressBytes = null;
	private String terminalAddress = null;
	private int[] terminalAddressBytes = null;
	
	int pinCode = -1; //0 -> 9999 (Also for Ok, though card won't care.)
	int requiresPin = 0; //-1 for no, 0 for undetermined and 1 for true.
	LinkedList<Byte> paymentTxBytes = new LinkedList<Byte>();
	boolean claimIsDEREncoded;
	boolean paymentComplete;
	
	//SPECIFIC CARD STATE (will clear on connect):
	//This data may change in the actual card and should be checked/updated at certain points.
	boolean isConnected;
	BitcoinCallbackMethods commandInProgress = BitcoinCallbackMethods.None; //Only one. None same as "ProcessingTask = false".
	
	int cardNetworkType; //0 = Bitcoin network
	int cardProtocolType; //0-5 reserved for BlochsTech Open Bitcoin Card Protocol (BOBC) #0 is base version and #5 in 20 years minimum. Rest just reserved for now.
	
	int[] waitingAddress;
	int[] waitingTerminalAddress;
	
	Double waitingFee; //Informing the card of the miners fee
	Double waitingAmount;
	Double waitingTerminalAmount;
	Double waitingCardFee;
	boolean waitingIsResetRequest;
	
	boolean waitingKnown;
	
	int timeLockSeconds; //Not 0 means it gets checked.

	ArrayList<String> addresses = new ArrayList<String>();
	
	private ArrayList<TXSourceId> knownSources = new ArrayList<TXSourceId>();
	private boolean knownSourcesSynched; //Set to false at default and at sent TX info.
	int sourceIndex = 0;
	
	byte[] blockHeader;
	MerkleBranchElement[] merkleBranch;
	int merkleIndex;
	private byte[] txRaw;
	TXSourceId txSource = new TXSourceId();
	
	private int txPackets;
	byte packetNumber;
	boolean waitingForNetwork; //Else PIN/other message.
	boolean hasNetwork; //TODO: Set and use. (Is set, use missing?)
	boolean noMoreUnknownSources; //Used for no funds case.
	
	int maxCardSources;
	Double maxCardCharge;
	
	BitcoinCardStateObject(){
		connectClear(false);
	}
	
	byte[] getTXRaw(){
		return txRaw;
	}
	
	void setTXRaw(byte[] value){
		txRaw = value;
		txPackets = value != null ? value.length / 246 : 0;
	}
	
	int getTXPackets(){
		return txPackets;
	}
	
	ArrayList<TXSourceId> getKnownSources(){
		return knownSources;
	}
	
	boolean KnownSourcesSynched(){
		return knownSourcesSynched;
	}
	
	String getAddress(){
		return address;
	}
	void setAddress(String value){
		addressBytes = TypeConverter.fromBase58CheckToData(value);
		if(addressBytes != null){
			address = value;
		}else{
			address = null;
		}
	}
	
	String getTerminalAddress(){
		return terminalAddress;
	}
	void setTerminalAddress(String value){
		terminalAddressBytes = TypeConverter.fromBase58CheckToData(value);
		if(terminalAddressBytes != null){
			terminalAddress = value;
		}else{
			terminalAddress = null;
		}
	}
	
	boolean ChargeSynched(){
		boolean waitingAddressMatch=true, waitingTerminalAddressMatch=true;
		boolean go1 = false, go2 = false;
		for(int i = 0; i < 21; i++){
			if(go1 == true && go2 == true)
				break;
			if(go1 == false){
				if(addressBytes != null && waitingAddress != null)
				{
					if(addressBytes[i] != waitingAddress[i]){
						waitingAddressMatch = false;
						go1 = true;
					}
				}else{
					waitingAddressMatch = amount <= 0.0;
					go1 = true;
				}
			}
			if(go2 == false){
				if(terminalAddressBytes != null && waitingTerminalAddress != null)
				{
					if(terminalAddressBytes[i] != waitingTerminalAddress[i]){
						waitingTerminalAddressMatch = false;
						go1 = true;
					}
				}else{
					waitingAddressMatch = terminalAmount <= 0.0;
					go2 = true;
				}
			}
		}
		return waitingKnown
		   && waitingAddressMatch
		   && waitingTerminalAddressMatch
		   && SyntacticSugar.EqualWithinMargin(amount, waitingAmount, 0.001)
		   && SyntacticSugar.EqualWithinMargin(terminalAmount, waitingTerminalAmount, 0.001)
		   && SyntacticSugar.EqualWithinMargin(fee, waitingFee, 0.001);
	}
	
	void KnownSourcesSynched(boolean value){
		if(knownSourcesSynched && !value){
			sourceIndex = 0;
		}
		knownSourcesSynched = value;
	}
	
	boolean knownSource(TXSourceId source){
		if(source == null || knownSources == null)
			return false;
		
		for(int i = 0; i < knownSources.size(); i++){
			if(((source.OutIndex != null && knownSources.get(i).OutIndex.toLowerCase(Locale.ENGLISH).equals(source.OutIndex.toLowerCase(Locale.ENGLISH))) 
					|| knownSources.get(i).OutIndex == null )
				&& knownSources.get(i).TXHash.toLowerCase(Locale.ENGLISH).equals(source.TXHash.toLowerCase(Locale.ENGLISH)))
				return true;
		}
		return false;
	}
	
	boolean knownSource(String txHash){ //TODO: This version is wrong, you have to check both hash and index. Allowed for proto terminal.
		if(txHash == null || knownSources == null)
			return false;
		
		for(int i = 0; i < knownSources.size(); i++){
			if(knownSources.get(i).TXHash.toLowerCase(Locale.ENGLISH).equals(txHash.toLowerCase(Locale.ENGLISH)))
				return true;
		}
		return false;
	}
	
	long totalKnownAmountAsSatoshi(){
		if(!KnownSourcesSynched())
			return 0;
		
		long totalSatoshis = 0;
		for(int i = 0; i < knownSources.size(); i++){
			totalSatoshis = totalSatoshis + knownSources.get(i).Satoshi;
		}
		return totalSatoshis;
	}
	Double totalKnownAmountAsBitcoin(){
		return (Double) (totalKnownAmountAsSatoshi() / 100000000.0);
	}
	
	Double totalChargeAsBitcoin(){
		return fee + terminalAmount + amount;
	}
	
	Double totalWaitingAmount(){
		return waitingFee + waitingTerminalAmount + waitingAmount + waitingCardFee;
	}
	
	//TODO: What if TX contains two sources?
	//1. Both should be added by the card already. 2. Card should change status to verified for both. <-- Only TODO
	//IsNewCheck? Also looks at index so its already compatible.
	boolean isVerified(String txHash){
		if(txHash == null || knownSources == null)
			return false;
		
		for(int i = 0; i < knownSources.size(); i++){
			if(knownSources.get(i).TXHash.toLowerCase(Locale.ENGLISH).equals(txHash.toLowerCase(Locale.ENGLISH)))
				return knownSources.get(i).Verified;
		}
		return false;
	}
	//TODO: This is becomming too complex to understand, consider moving logic into separated self-contained "steps"...
	//basically what we are doing with ProcessStateChange... CONSIDER solutions
	
	void addKnownSource(TXSourceId source){
		if(!knownSource(source)){
			knownSources.add(source);
		}else{
			for(int i = 0; i < knownSources.size(); i++){
				if(knownSources.get(i).TXHash.toLowerCase(Locale.ENGLISH).equals(source.TXHash.toLowerCase(Locale.ENGLISH))){
					source.Unusable = knownSources.get(i).Unusable || source.Unusable;
					knownSources.set(i, source);
				}
			}
		}
	}
	
	//This method does not handle the "Purchase complete."-message. TODO: Make sure the charge gets cleared etc. to prevent accidental auto double-charge..
	String updateCardMessageFromState(){
		if(!isConnected || cardNetworkType != 0 || cardProtocolType != 0)
			return "No card.";
		
		if (paymentComplete){
			return "Payment complete.";
		}
		
		if(maxCardCharge != null && totalChargeAsBitcoin() != null && maxCardCharge < totalChargeAsBitcoin())
			return "Over max " + ShortNumberConverter.ToShort(maxCardCharge) + ".";
		
		if(ChargeSynched() && knownSourcesSynched && totalWaitingAmount() > totalKnownAmountAsBitcoin()){
			
			if(!hasNetwork)
				return "No network.";
			
			if(noMoreUnknownSources)
				return "Not enough funds.";
			
			if(waitingForNetwork)
				return "Waiting for Network.";
		}else if(requiresPin == 1 && timeLockSeconds > 0){
			return "Card unlocked in: " + timeLockSeconds + "s.";
		}else if(pinCode == -1 && timeLockSeconds == 0 && (ChargeSynched() || waitingIsResetRequest)){
			return "Waiting for PIN.";
		}else {
			if(timeLockSeconds == 0)
				return "Card unlocked.";
		}
		
		return "Card connected.";
	}
	
	void connectClear(boolean connected){
		//Clear things that become unknown as a card reconnects/new card is introduced:
		pinCode = -1;
		paymentTxBytes = new LinkedList<Byte>();
		claimIsDEREncoded = false;
		paymentComplete = false;
		
		cardNetworkType = -1;
		cardProtocolType = -1;
		timeLockSeconds = 1;
		
		waitingAddress = null;
		waitingTerminalAddress = null;
		
		requiresPin = 0;
		waitingAmount = 0.0;
		waitingFee = 0.0;
		waitingTerminalAmount = 0.0;
		waitingCardFee = 0.0;
		waitingKnown = false;
		
		isConnected = connected;
		commandInProgress = BitcoinCallbackMethods.None;
		
		addresses = new ArrayList<String>();
		
		knownSources = new ArrayList<TXSourceId>();
		knownSourcesSynched = false;
		sourceIndex = 0;
		
		blockHeader = null;
		merkleBranch = null;
		merkleIndex = 0;
		txRaw = null;
		txSource = new TXSourceId();
		packetNumber = 0;
		
		waitingForNetwork = false;
		hasNetwork = true; //Set to true until otherwise proven (network connector fails).
		noMoreUnknownSources = false;
		
		maxCardSources = 0;
		maxCardCharge = null;
	}
	
	void finishedSendingSourceToCard_Clear(){
		txSource = new TXSourceId();
		merkleBranch = null;
		merkleIndex = 0;
		blockHeader = null;
		packetNumber = 0;
		txRaw = null;
		waitingKnown = false;
		knownSourcesSynched = false;
		knownSources = new ArrayList<TXSourceId>();
		sourceIndex = 0;
		
		maxCardCharge = null; //MaxCharge can change, so we clear post charges.
	}
	
	void sourceRejectedByCard_Clear(){
		txSource = new TXSourceId();
		merkleBranch = null;
		merkleIndex = 0;
		blockHeader = null;
		packetNumber = 0;
		txRaw = null;
	}
}