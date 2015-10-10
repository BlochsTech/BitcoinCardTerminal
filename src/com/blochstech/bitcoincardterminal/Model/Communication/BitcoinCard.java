package com.blochstech.bitcoincardterminal.Model.Communication;

import java.util.ArrayList;
import java.util.LinkedList;

import android.nfc.Tag;

import com.blochstech.bitcoincardterminal.Interfaces.Message;
import com.blochstech.bitcoincardterminal.Interfaces.Message.MessageType;
import com.blochstech.bitcoincardterminal.Utils.ByteConversionUtil;
import com.blochstech.bitcoincardterminal.Utils.Event;
import com.blochstech.bitcoincardterminal.Utils.EventListener;
import com.blochstech.bitcoincardterminal.Utils.SyntacticSugar;
import com.blochstech.bitcoincardterminal.Utils.Tags;
import com.blochstech.bitcoincardterminal.Model.AppSettings;
import com.blochstech.bitcoincardterminal.Model.Communication.NetworkCallbackMethods;
import com.blochstech.bitcoincardterminal.Model.Communication.NetworkPublishResults.SendStatus;

//Extends and uses a class that has its own worker thread, however this class runs on the main UIThread.
//Only different thing is that it will take updates and asynchronously give the responses.
public class BitcoinCard extends NFCWrapper {
	//Variables and types:
	private NetworkConnector networkConnector;

	private BitcoinCardStateObject state = new BitcoinCardStateObject();
	
	//Events
	private NetworkListener networkListener = new NetworkListener();
	private Event<Message> messageEvent;
	private Event<Callback> callbackEvent;
	private Object fireKey = new Object();
	
	//Properties:
	private String cardMessage = "No card.";
	public String CardMessage(){
		return cardMessage;
	}
	
	public String ShortCharge(){
		return state.waitingIsResetRequest ? "Card reset" : "";
		//For now this is ok: View will ignore if empty, that way we can expand functionality as needed.
	}
	
	private String vignereCode;
	public String VignereCode(){
		return vignereCode;
	}
	
	public boolean PinRequired(){
		return state.requiresPin > -1;
	}
	
	public String CardAddress(){
		if(state == null || state.addresses == null || state.addresses.size() < 1)
			return null;
		
		return state.addresses.get(0);
	}
	
	private boolean courtesyOk;
	
	//Setup and event accessing:
	public BitcoinCard(){
		super();
		messageEvent = new Event<Message>(fireKey);
		callbackEvent = new Event<Callback>(fireKey);
		networkConnector = new NetworkConnector();
		networkConnector.CallbackEventReference().register(networkListener);
	}
	public Event<Message> MessageEventReference() {
		return messageEvent;
	}
	public Event<Callback> CallbackEventReference() {
		return callbackEvent;
	}
	
	//Exposed methods:
	private boolean connectDebug;
	public void setTag(Tag tag){
		connectDebug = true;
		super.newCard(tag);
		state.connectClear(true);
		//Leads to new connect event.
	}
	
	public void setFee(Double fee){
		if(fee < AppSettings.MIN_FEE_BITCOINS){
			state.fee = AppSettings.MIN_FEE_BITCOINS;
		}else{
			state.fee = fee;
		}
	}
	
	public void setAmount(Double amount){
		state.amount = amount;
	}
	public void setTerminalAmount(Double terminalAmount){
		state.terminalAmount = terminalAmount;
	}
	
	public void setAddress(String address){
		state.setAddress(address);
	}
	public void setTerminalAddress(String terminalAddress){ //Must work, but if null ignore it.
		state.setTerminalAddress(terminalAddress);
	}
	
	public void setCourtesyOk(boolean value){
		courtesyOk = value;
	}
	
	public void setPIN(int pin){
		state.pinCode = pin;
		processStateChange();
	}
	
	//boolean debugBool = false;
	public void processStateChange() { //Either things move forward here OR on events from card wrapper.
		//Here we assume, no action in process and connected:
		try{
			if(state.commandInProgress == BitcoinCallbackMethods.None && state.isConnected){
				
				/*if(debugBool && state.timeLockSeconds == 0)
				{
					state.commandInProgress = BitcoinCallbackMethods.ResetPinCode;
					super.newTask(CardTaskUtil.getResetPinTask(12345, 1235));
				}*/
				
				if(state.txSource == null)
					state.txSource = new TXSourceId();
				

				if(connectDebug)
				{
					connectDebug = false;
					state.commandInProgress = BitcoinCallbackMethods.Debug;
					super.newTask(CardTaskUtil.getDebugTask());
					//super.newTask(CardTaskUtil.getReadEepromTask()); Fails as it should with "InvalidState"
				}else if(state.cardNetworkType != 0){
					state.commandInProgress = BitcoinCallbackMethods.Network;
					super.newTask(CardTaskUtil.getNetworkTask());
				}else if(state.cardProtocolType != 0){
					state.commandInProgress = BitcoinCallbackMethods.Protocol;
					super.newTask(CardTaskUtil.getProtocolTask());
				}else if(state.addresses.size() == 0){
					state.commandInProgress = BitcoinCallbackMethods.Addresses;
					super.newTask(CardTaskUtil.getAddressesTask());
				}else if(state.maxCardCharge == null){
					state.commandInProgress = BitcoinCallbackMethods.MaxAmount;
					super.newTask(CardTaskUtil.getMaxAmountTask());
				}else if(state.maxCardSources == 0){
					state.commandInProgress = BitcoinCallbackMethods.MaxSources;
					super.newTask(CardTaskUtil.getMaxSourcesTask());
				}else if(state.addresses.size() > 0 && !state.KnownSourcesSynched()){ //If we have addresses/source state change, get cards known sources:
					//This can be used for: Check successful TX, check new source loaded and at card boot up... confusing -> Will be automatically processed.
					//state.knownSources = new ArrayList<TXSourceId>(); Not clearing here should be ok, prevents clearing of unusable sources.
					state.commandInProgress = BitcoinCallbackMethods.Sources;
					super.newTask(CardTaskUtil.getSourcesTask(state.sourceIndex));
					
				//Find out if we already charged card:
				}else if(!state.waitingKnown){
					state.commandInProgress = BitcoinCallbackMethods.WaitingCharge;
					super.newTask(CardTaskUtil.getWaitingChargeTask());
					
				//Request payment card. Called as soon as possible, not by default after unlocking card as it may show card will accept amount instantly.
				}else if(state.getAddress() != null && state.amount != null && state.amount > 0.0 //We are ready to charge.
						&& !state.ChargeSynched() && !state.waitingIsResetRequest
						&& state.maxCardCharge >= state.totalChargeAsBitcoin()
						&& AppSettings.DUST_LIMIT <= state.amount){
					
					//PIN requirement undetermined or card unlocked.
					//If pin required we need to charge again after unlocking --> Automatic with above if clause.
					state.commandInProgress = BitcoinCallbackMethods.RequestPayment;
					cardMessage = state.updateCardMessageFromState();
					if(state.waitingChargeAmount() == 0
							|| state.totalChargeAsBitcoin() < state.waitingChargeAmount()){
						super.newTask(CardTaskUtil.getRequestPaymentTask(state.amount, state.terminalAmount, state.fee,
								state.getAddress(), state.getTerminalAddress()));
					}else{
						//Reset card if it is already occupied with a charge: (0 amount resets, card must be delay unlocked again however)
						super.newTask(CardTaskUtil.getRequestPaymentTask(0.0, 0.0, 0.0, "", ""));
					}
				}else if(((state.pinCode != -1)
						|| (state.requiresPin == -1 && !courtesyOk))
						&& state.timeLockSeconds == 0
					    && ((state.ChargeSynched() && state.totalKnownAmountAsBitcoin() >= state.totalWaitingAmount())
			    		|| (state.waitingIsResetRequest)) ){
					
					state.commandInProgress = BitcoinCallbackMethods.Pin;
					super.newTask(CardTaskUtil.getPinTask(state.pinCode));
				
				}else if (state.timeLockSeconds != 0 && state.totalKnownAmountAsBitcoin() >= state.totalWaitingAmount()
						&& !state.ChargeSynched()){
					state.commandInProgress = BitcoinCallbackMethods.TimeUnlock;
					super.newTask(CardTaskUtil.getTimeUnlockTask());
				
				//The placement of the else if clause and the condition added to sources response handling (< maxCardSources)
				//+ above extra timeunlock clause should mean that card loading will stop/be prioritized less after card is loaded enough.
				//If we have the data, give TX to card:
				}else if(state.getTXRaw() != null && state.blockHeader != null && state.merkleBranch != null
						&& state.KnownSourcesSynched() && !state.knownSource(state.txSource.TXHash)
						&& !state.txSource.Unusable
						&& state.getKnownSources().size() < state.maxCardSources){
					//Send tx.
					state.commandInProgress = BitcoinCallbackMethods.TX;
					state.packetNumber = 0;
					super.newTask(CardTaskUtil.getSendTXCommand(state.getTXRaw(), (byte) 0));
				
				//If source is added, but not verified yet, start verify hashes process:
				}else if(state.KnownSourcesSynched() && state.knownSource(state.txSource.TXHash)
						&& !state.isVerified(state.txSource.TXHash) && !state.txSource.Unusable){
					state.commandInProgress = BitcoinCallbackMethods.Headers;
					super.newTask(CardTaskUtil.getHeadersTask(state.blockHeader, state.txSource.TXHash));
					
				//Nothing useful left to do? Unlock card:
				}else if (state.timeLockSeconds != 0){
					state.commandInProgress = BitcoinCallbackMethods.TimeUnlock;
					super.newTask(CardTaskUtil.getTimeUnlockTask());
				}else{
					cardMessage = state.updateCardMessageFromState();
					callbackEvent.fire(fireKey, new Callback(-1));
				}
			}
		}catch(Exception ex){
			messageEvent.fire(fireKey, new Message("ProcessStateChange failed in BitcoinCard: " + (ex!=null?ex.toString():"Unexpected null exception."), MessageType.Error));
		}
	}
	
	private void disconnect() {
		state.connectClear(false);
		
		cardMessage = state.updateCardMessageFromState(); //was "No card."
		vignereCode = "";
		callbackEvent.fire(fireKey, new Callback(-1));
	}
	
	//Super class events:
	@Override
	protected void onConnectionEvent(boolean value){
		state.connectClear(value);
		
		cardMessage = state.updateCardMessageFromState(); //Was "No card."
		vignereCode = "";
		callbackEvent.fire(fireKey, new Callback(-1));
		
		//Call getNetwork:
		if(value){
			processStateChange();
		}
	}
	
	@Override
	protected void onExceptionEvent(Exception value){
		//Handle what can be handled here. Rest is logged. In the end nothing should be logged.
		if(value != null && value.toString() != null && (value.toString().equals("java.io.IOException: Transceive failed")
				|| value.toString().equals("android.nfc.TagLostException: Tag was lost.")))
		{
			messageEvent.fire(fireKey, new Message("Card communication most likely failed due to the card being moved. " + value.toString(), MessageType.Warning));
			state.commandInProgress = BitcoinCallbackMethods.None;
		}else if(!value.toString().contains("New task given although task is already in progress.")){
			messageEvent.fire(fireKey, new Message(value.toString(), MessageType.Error));
			state.commandInProgress = BitcoinCallbackMethods.Debug;
			super.newTask(CardTaskUtil.getDebugTask());
		}else{
			messageEvent.fire(fireKey, new Message(value.toString(), MessageType.Error));
		}
	}
	
	@Override
	protected void onResponseEvent(byte[] value){
		String byts = "";
		if(value != null){
			try{
				for(int i = 0; i < value.length; i++){
					byts = byts + " " + value[i];
				}
				switch(state.commandInProgress){
					case Network:
						state.cardNetworkType = CardTaskUtil.getNetworkResponse(value);
						state.commandInProgress = BitcoinCallbackMethods.None;
						if(state.cardNetworkType != 0){
							disconnect();
						}else{
							processStateChange();
						}
						break;
					case Protocol:
						state.cardProtocolType = CardTaskUtil.getProtocolResponse(value);
						state.commandInProgress = BitcoinCallbackMethods.None;
						if(state.cardProtocolType != 0){
							disconnect();
						}else{
							cardMessage = state.updateCardMessageFromState(); //Was "Card connected."
							callbackEvent.fire(fireKey, new Callback(-1));
							
							processStateChange();
						}
						break;
					case Addresses:
						ArrayList<String> addresses = CardTaskUtil.getAddressesResponse(value);
						state.addresses = addresses;
						
						callbackEvent.fire(fireKey, new Callback(-1)); //Let settings page know if its waiting for NFC card address.
						
						state.commandInProgress = BitcoinCallbackMethods.None;
						processStateChange();
						break;
					case MaxAmount:
						state.maxCardCharge = CardTaskUtil.getMaxAmountResponse(value);
						if(state.maxCardCharge == null || state.maxCardCharge <= 0)
							disconnect();
						
						state.commandInProgress = BitcoinCallbackMethods.None;
						processStateChange();
						break;
					case MaxSources:
						state.maxCardSources = CardTaskUtil.getMaxSourcesResponse(value);
						if(state.maxCardSources <= 0)
							disconnect();
						
						state.commandInProgress = BitcoinCallbackMethods.None;
						processStateChange();
						break;
					case Sources:
						SourceCardResponse resp = CardTaskUtil.getSourcesResponse(value);
						if(resp.Source != null)
							state.addKnownSource(resp.Source);
						
						if(resp.NextIndex == 0){
							//Done:
							
							String debugSrc = "";
							for(int i = 0; i < state.getKnownSources().size(); i++){
								debugSrc = debugSrc + "(" + state.getKnownSources().get(i).TXHash + "|" +
										state.getKnownSources().get(i).OutIndex + "|" + 
										state.getKnownSources().get(i).Satoshi + ")";
							}
							messageEvent.fire(fireKey, new Message("KnownSources:"+debugSrc, MessageType.Info));
							
							state.KnownSourcesSynched(true);
							if(state.getKnownSources().size() < state.maxCardSources){
								networkConnector.getUnspentTXs(state.addresses, state.getKnownSources()); //While unlocking, runs on its own thread.
								state.waitingForNetwork = true;
							}
							
							state.commandInProgress = BitcoinCallbackMethods.None;
							processStateChange();
						}else{
							//Next source:
							state.commandInProgress = BitcoinCallbackMethods.Sources;
							state.sourceIndex++;
							super.newTask(CardTaskUtil.getSourcesTask(state.sourceIndex));
						}
						break;
					case WaitingCharge:
						WaitingChargeCardResponse waitingResp = CardTaskUtil.getWaitingChargeResponse(value);
						
						state.waitingAmount = waitingResp.WaitingAmount;
						state.waitingTerminalAmount = waitingResp.WaitingTerminalAmount;
						state.waitingFee = waitingResp.WaitingFee;
						state.waitingAddress = waitingResp.WaitingAddress;
						state.waitingTerminalAddress = waitingResp.WaitingTerminalAddress;
						state.waitingIsResetRequest = waitingResp.WaitingIsResetRequest;
						state.waitingCardFee = waitingResp.WaitingCardFee;
						state.requiresPin = waitingResp.WaitingRequiresPin ? 1 : -1; //More complex than this:
						//Case 1: Request is processed and either requires pin or not -> above is ok.
						//Case 2: Request is NOT processed because another (smaller) request is waiting to be cleared -> Told Pin is required, will unlock now etc. = ok.
						//Case 3: Request is NOT processed because reset request is waiting. -> Handled separately = ok.
						//=> It was a mistake to make it dependant on ChargeSynched. Only vignereCode is dependant because it is displayed to the card holder - hence the old should not be shown.
						//Only reset vignere or relevant vignere should be shown.
						state.waitingKnown = true; //ChargeSynched() gives wrong result if this is not set right.
						
						if(state.ChargeSynched()
								|| (state.totalWaitingAmount() == 0 && state.waitingIsResetRequest)){
							vignereCode = waitingResp.WaitingVignereCode;
						}else{
							vignereCode = "";
						}
						
						callbackEvent.fire(fireKey, new Callback(-1)); //Update vignere and pin pad.
						
						state.commandInProgress = BitcoinCallbackMethods.None;
						processStateChange();
						break;
					case RequestPayment:
						ChargeRequestCardResponse requestResp = CardTaskUtil.getRequestPaymentResponse(value);
						vignereCode = requestResp.VignereCode;
						state.requiresPin = requestResp.RequiresPIN ? 1 : -1; //0 means undetermined.
						state.waitingKnown = false; //This will lead to wasted checks, however it is the simplest most robust way.
						callbackEvent.fire(fireKey, new Callback(-1)); //Update vignere shown + pin pad.
						
						state.commandInProgress = BitcoinCallbackMethods.None;
						processStateChange();
						break;
					case TimeUnlock:
						state.timeLockSeconds = CardTaskUtil.getTimeUnlockResponse(value);
						cardMessage = state.updateCardMessageFromState(); //Was full previous logic.
						callbackEvent.fire(fireKey, new Callback(-1));
						state.commandInProgress = BitcoinCallbackMethods.None;
						processStateChange();
						break;
					case TX:
						boolean packageAccepted = CardTaskUtil.getSendTXResponse(value);
						
						if(packageAccepted && state.getTXPackets() > state.packetNumber){
							state.packetNumber++;
							state.commandInProgress = BitcoinCallbackMethods.TX;
							super.newTask(CardTaskUtil.getSendTXCommand(state.getTXRaw(), state.packetNumber));
						}else if(packageAccepted){
							state.KnownSourcesSynched(false);
							state.commandInProgress = BitcoinCallbackMethods.None;
							processStateChange();
						}else{
							throw new Exception("Package was not accepted, but there is no error message.");
						}
						break;
					case Headers:
						boolean hAccept = CardTaskUtil.getHeadersResponse(value);
						if(hAccept){
							state.commandInProgress =  BitcoinCallbackMethods.Hashes;
							state.merkleIndex = 0;
							super.newTask(CardTaskUtil.getMerkleTask(state.merkleBranch[state.merkleIndex]));
						}else{
							if(state.txSource == null)
								state.txSource = new TXSourceId();
							state.txSource.Unusable = true;
							state.addKnownSource(state.txSource);
							state.finishedSendingSourceToCard_Clear();
							processStateChange();
						}
						break;
					case Hashes:
						boolean mAccept = CardTaskUtil.getMerkleResponse(value);
						if(mAccept || state.merkleIndex < state.merkleBranch.length-1){
							if(state.merkleIndex >= state.merkleBranch.length-1){
								state.commandInProgress = BitcoinCallbackMethods.None;
								state.merkleIndex = 0;
								state.KnownSourcesSynched(false); //Should cause a resync to check that verfified state is now true.
								processStateChange();
							}else{
								state.commandInProgress =  BitcoinCallbackMethods.Hashes;
								state.merkleIndex++;
								super.newTask(CardTaskUtil.getMerkleTask(state.merkleBranch[state.merkleIndex]));
							}
						}else{
							if(state.txSource == null)
								state.txSource = new TXSourceId();
							state.txSource.Unusable = true;
							state.addKnownSource(state.txSource);
							state.finishedSendingSourceToCard_Clear();
							state.commandInProgress = BitcoinCallbackMethods.None;
							processStateChange();
						}
						break;
					case Pin:
						PinCardResponse pinResp = CardTaskUtil.getPinResponse(value);
						state.timeLockSeconds = -1;
						if(pinResp.PinAccepted){
							if(!state.waitingIsResetRequest && state.paymentTxBytes.size() == 0)
							{
								state.paymentTxBytes = new LinkedList<Byte>();
								state.claimIsDEREncoded = true; //Always. Very simple so card must do it.
								if(!state.claimIsDEREncoded){
									cardMessage = "DER Err";
									throw new Exception("Terminal DER encoding not supported yet.");
								}
							}
							
							if((!state.waitingIsResetRequest && state.paymentTxBytes.size() == 0)
									|| state.paymentTxBytes.size() > 0){

								for(int i = 0; i < pinResp.TxDataPacket.length; i++){
									state.paymentTxBytes.add(pinResp.TxDataPacket[i]);
								}
							}
							
							if(pinResp.LastPacket || state.waitingIsResetRequest){

								/*String txRes = ByteConversionUtil.byteArrayToHexString(
										state.paymentTxBytes.toArray(new Byte[state.paymentTxBytes.size()]));
								txRes = txRes + "";*/
								
								/*
								state.commandInProgress = BitcoinCallbackMethods.Debug;
								super.newTask(CardTaskUtil.getDebugTask());
								break;*/
								
								String tmpRes = ByteConversionUtil.byteArrayToHexString(ByteConversionUtil.toBytes(state.paymentTxBytes));
								tmpRes = tmpRes + "";
								
								if(!state.waitingIsResetRequest && tmpRes != null && tmpRes.length() > 0){
									networkConnector.publishTX(state.paymentTxBytes);
								}
								
								//else if (state.waitingIsResetRequest){
								//	messageEvent.fire(fireKey, new Message("Attempted to publish empty TX.", MessageType.Error)); //TODO: Make sure this never happens.
								//}
								//TODO: Coomunicate sent TXes to History model/harddisk, for now just straight to the NetworkConnector.
								
								//state.paymentComplete = !state.waitingIsResetRequest;
								state.waitingIsResetRequest = false;
								state.pinCode = -1;
								cardMessage = state.updateCardMessageFromState();
								callbackEvent.fire(fireKey, new Callback(-1));
								
								state.commandInProgress = BitcoinCallbackMethods.Debug;
								super.newTask(CardTaskUtil.getDebugTask());
								
								//state.commandInProgress = BitcoinCallbackMethods.None;
								//processStateChange();
							}else{
								//Get more bytes, don't process state yet:
								state.commandInProgress = BitcoinCallbackMethods.Pin;
								super.newTask(CardTaskUtil.getPinTask(state.pinCode));
							}
						}else{
							state.pinCode = -1;
							cardMessage = "Wrong PIN.";
							callbackEvent.fire(fireKey, new Callback(-1));
							
							state.commandInProgress = BitcoinCallbackMethods.None;
							processStateChange();
						}
						break;
					case ResetPinCode:
						boolean resetSuccess = CardTaskUtil.getResetPinResponse(value);
						resetSuccess = resetSuccess && true;
						state.commandInProgress = BitcoinCallbackMethods.Debug;
						super.newTask(CardTaskUtil.getDebugTask());
						break;
					case DebugOnce:
						messageEvent.fire(fireKey, new Message("Debug message: " + CardTaskUtil.getDebugResponse(value), MessageType.Info));
						state.commandInProgress = BitcoinCallbackMethods.None;
						disconnect();
						break;
					case Debug:
						messageEvent.fire(fireKey, new Message("Debug message: " + CardTaskUtil.getDebugResponse(value), MessageType.Info));
						state.commandInProgress = BitcoinCallbackMethods.None;
						processStateChange();
						break;
					default:
						messageEvent.fire(fireKey, new Message("No response expected at the moment ("+byts+").", MessageType.Error));
						break;
				}
			}catch(Exception ex){
				if(ex != null && ex.getMessage() != null &&
						ex.getMessage().equals("BOBC Error: 11")){ //If source rejected (already spent), get new ones:
					TXSourceId tmp = state.txSource;
					tmp.Unusable = true;
					state.addKnownSource(tmp);
					state.sourceRejectedByCard_Clear();
					networkConnector.getUnspentTXs(state.addresses, state.getKnownSources());
					
					state.waitingForNetwork = true;
					state.commandInProgress = BitcoinCallbackMethods.None;
					processStateChange();
				}else{
					cardMessage = "Error.";
					callbackEvent.fire(fireKey, new Callback(-1));
					
					messageEvent.fire(fireKey, new Message("Unexpected byte response ("+byts+") to "+state.commandInProgress+" from BitcoinCard: " + ex.toString(), MessageType.Error));
					if(Tags.DEBUG)
						ex.printStackTrace(); //TODO: Make better unified logging system using printStackTrace nicely etc..
					state.commandInProgress = BitcoinCallbackMethods.DebugOnce;
					super.newTask(CardTaskUtil.getDebugTask());
				}
			}
		}else{
			messageEvent.fire(fireKey, new Message("Null response to "+state.commandInProgress+", should never happen.", MessageType.Error));
		}
	}
	
	private class NetworkListener extends EventListener<Callback>{
		@Override
		public void onEvent(Callback event) {
			try{
				if(event != null){
					//There can be network 3 cases:
					//1: Data is returned -> use it. DONE
					//2: There is connection, but no new sources -> Try to use offline sources or show "Not enough funds" message. DONE
					//3: No connection -> Try offline or show "No network" error. DONE
					
					//Other network stuff:
					//1: Process state change must be robust towards being offline. (ie new source loading being optional) DONE
					//2: TX publishing must happen asynchronously when network is established/support offline. TODO
					
					if(event.MethodId == NetworkCallbackMethods.TXData.Value()){
						if(event.Response != null){
							if(((byte[])event.Response[2]).length > 2000){
								TXSourceId val = new TXSourceId();
								val.TXHash = (String) event.Response[3];
								val.Unusable = true;
								state.addKnownSource(val);
								networkConnector.getUnspentTXs(state.addresses, state.getKnownSources()); //Runs on its own thread.
								state.waitingForNetwork = true;
							}else{
								state.blockHeader = (byte[]) event.Response[0];
								state.merkleBranch = (MerkleBranchElement[]) event.Response[1];
								state.setTXRaw((byte[]) event.Response[2]);
								state.txSource = new TXSourceId();
								state.txSource.TXHash = (String) event.Response[3];
								state.waitingForNetwork = false;
								cardMessage = state.updateCardMessageFromState(); //Was full previous logic.
								callbackEvent.fire(fireKey, new Callback(-1));
								processStateChange();
							}
						}else{
							state.waitingForNetwork = false;
							state.noMoreUnknownSources = true;
							state.blockHeader = null;
							state.merkleBranch = null;
							state.setTXRaw(null);
							state.txSource = new TXSourceId();
							cardMessage = state.updateCardMessageFromState(); //Was full previous logic.
							callbackEvent.fire(fireKey, new Callback(-1));
							processStateChange();
						}
					}else if(event.MethodId == NetworkCallbackMethods.NoConnection.Value()){
						state.hasNetwork = false;
						state.waitingForNetwork = false;
						cardMessage = "No network."; //Was "No network."
						callbackEvent.fire(fireKey, new Callback(-1));
						processStateChange(); //Does what can be done or exits process until new card connect.
					}else if(event.MethodId == NetworkCallbackMethods.TXSendResult.Value()){
						NetworkPublishResults.SendStatus status = SyntacticSugar.castAs(event.Response[0]); //TX accepted by Api.
						String message = SyntacticSugar.castAs(event.Response[1]); //Error in case of error.
						
						state.paymentComplete = status == SendStatus.Retry ? 0 
								: status == SendStatus.OK ? 1 : -1;
						state.updateCardMessageFromState();
						
						if(status == SendStatus.Invalid)
							messageEvent.fire(fireKey, new Message("Failed to send TX. Error: " + message, MessageType.Error));
						if(status == SendStatus.Retry)
							messageEvent.fire(fireKey, new Message("Failed to send TX. Warning: " + message, MessageType.Warning));
					}else{
						messageEvent.fire(fireKey, new Message("Unrecognized callback: " + event.MethodId, MessageType.Error));
					}
				}
			}catch(Exception ex){
				messageEvent.fire(fireKey, new Message(ex.toString(), MessageType.Error));
			}
		}
	}
}