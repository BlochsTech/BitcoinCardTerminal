package com.blochstech.bitcoincardterminal.Model;

import com.blochstech.bitcoincardterminal.Interfaces.Message;
import com.blochstech.bitcoincardterminal.Model.Communication.BitcoinCard;
import com.blochstech.bitcoincardterminal.Model.Communication.Callback;
import com.blochstech.bitcoincardterminal.Utils.Event;
import com.blochstech.bitcoincardterminal.Utils.EventListener;

import android.nfc.Tag;

class CardModel {
	private Event<Message> messageEvent;
	private Object fireKey = new Object();
	
	private EventListener<Message> messageListener = new MessageListener();
	private EventListener<Callback> callbackListener = new CallbackListener();
	
	private BitcoinCard card;
	
	CardModel(){
		card = new BitcoinCard();
		messageEvent = new Event<Message>(fireKey);
		
		card.CallbackEventReference().register(callbackListener);
		card.MessageEventReference().register(messageListener);
	}

	public void persistData() {
		//Use as needed.
	}
	
	private String cardMessage = "No card.";
	String getCardMessage(){
		return cardMessage;
	}
	
	private String shortCharge = "";
	String getShortCharge(){
		return shortCharge;
	}
	
	private String vignereCode = "";
	String getVignereCode(){
		if(vignereCode == null)
			return "";
		if(vignereCode.length() > 8){
			return vignereCode.substring(0, 8);
		}else{
			return vignereCode;
		}
	}
	
	private boolean pinRequired = true;
	boolean getPinRequired(){
		return pinRequired;
	}

	void setCard(Tag tag) {
		card.setTag(tag);
	}
	
	void setCharge(Double btcAmount, Double btcFee, String address, boolean courtesyOk) {
		card.setFee(btcFee); //This first and enforced via one charge method as 0 fee is also valid and could be charged by accident.
		card.setAmount(btcAmount);
		card.setAddress(address);
		card.setCourtesyOk(courtesyOk);
		
		card.processStateChange();
	}
	
	void setPin(int value){
		card.setPIN(value);
	}
	
	private class CallbackListener extends EventListener<Callback> {
		@Override
		public void onEvent(Callback event) {
			cardMessage = card.CardMessage();
			vignereCode = card.VignereCode();
			pinRequired = card.PinRequired();
			shortCharge = card.ShortCharge();
			Model.Instance().fireUpdate(cardMessage);
		}
    }
	
	private class MessageListener extends EventListener<Message> {
		@Override
		public void onEvent(Message event) {
			messageEvent.fire(fireKey, event);
		}
    }
	
	Event<Message> MessageEventReference() {
		return messageEvent;
	}
}