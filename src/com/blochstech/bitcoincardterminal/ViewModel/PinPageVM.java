package com.blochstech.bitcoincardterminal.ViewModel;

import com.blochstech.bitcoincardterminal.Interfaces.Currency;
import com.blochstech.bitcoincardterminal.Model.Model;
import com.blochstech.bitcoincardterminal.Utils.Event;
import com.blochstech.bitcoincardterminal.Utils.EventListener;
import com.blochstech.bitcoincardterminal.ViewModel.ViewStateManagers.MessageManager;

public class PinPageVM {
	public Event<Object> UpdateEvent;
	private Object fireKey = new Object();
	private EventListener<Object> modelListener = new ModelListener();
	
	public PinPageVM(){
		UpdateEvent = new Event<Object>(fireKey);
		Model.Instance().updateEvent.register(modelListener);
		
		stateUpdate();
	}
	
	public void stateUpdate(){
		currency = Model.Instance().getCurrency();
		fullPriceDollarValue = Model.Instance().getPrice() + Model.Instance().getFee();
		fullPrice = fullPriceDollarValue / currency.Value();
		courtesyOk = Model.Instance().getCourtesyOK();
		cardMessage = Model.Instance().getCardMessage();
		cardPrice = Model.Instance().getVignereCode();
		pinRequired = Model.Instance().getPinRequired();
		cardsShortCharge = Model.Instance().getShortCharge();
	}
	
	private String cardsShortCharge = "";
	public String CardsShortCharge(){
		return cardsShortCharge;
	}
	
	private String pinCode = "";
	public void pressPinButton(int buttonId){ //0 = 0, 1 = 1,..., delete = 10
		if(buttonId < 10 && buttonId >= 0 && pinCode.length() < 4){
			pinCode = pinCode + buttonId;
			UpdateEvent.fire(fireKey, null);
		}else if(buttonId == 10){
			pinCode = pinCode.substring(0, Math.max(0,pinCode.length()-1));
			UpdateEvent.fire(fireKey, null);
		}
	}
	public String PinCode(){
		String res = "";
		for(int i = 0; i < pinCode.length(); i++)
		{
			res = res + "*";
		}
		return res;
	}
	
	private Double fullPrice = 0.0;
	private Double fullPriceDollarValue = 0.0;
	public Double Price(){
		return fullPrice;
	}
	public Double BitcoinPrice(){
		return fullPriceDollarValue / Currency.Bitcoins.Value();
	}
	
	private String cardPrice = "";
	public String CardPrice(){
		return cardPrice;
	}
	
	private Currency currency = Currency.Apples;
	public Currency Currency(){
		return currency;
	}
	
	private String cardMessage = ""; 
	public String CardMessage(){
		return cardMessage;
	}
	
	private boolean pinRequired = true; //Model events?
	public boolean PINRequired(){
		return pinRequired;
	}
	
	private boolean courtesyOk = false; //Model events?
	public boolean CourtesyOk(){
		return courtesyOk;
	}
	
	public void okPressed(){
		int pinIntValue = 0;
		try{
			if(pinCode != null && pinCode.length() > 0)
				pinIntValue = Integer.parseInt(pinCode);
		}catch(Exception ex){
			MessageManager.Instance().AddMessage(ex != null ? ex.toString() : "Null error.", true);
		}
		if(pinIntValue > 9999){
			pinIntValue = pinIntValue % 10000;
		}
		
		Model.Instance().setPin(pinIntValue);
		pinCode = "";
		UpdateEvent.fire(fireKey,  null);
	}
	
	private class ModelListener extends EventListener<Object>{
		@Override
		public void onEvent(Object event) {
			try{
				
				if(event instanceof Currency || event instanceof Double || event instanceof Boolean || event instanceof String){
					stateUpdate();
					UpdateEvent.fire(fireKey, null);
				}
				
			}catch (Exception ex){
				MessageManager.Instance().AddMessage(ex != null ? ex.toString() : "Null error.", true);
	    	}
		}
    }
}
