package com.blochstech.bitcoincardterminal.ViewModel;

import com.blochstech.bitcoincardterminal.Interfaces.Currency;
import com.blochstech.bitcoincardterminal.Model.Model;
import com.blochstech.bitcoincardterminal.Utils.Event;
import com.blochstech.bitcoincardterminal.Utils.EventListener;
import com.blochstech.bitcoincardterminal.Utils.RegexUtil;
import com.blochstech.bitcoincardterminal.ViewModel.ViewStateManagers.MessageManager;

public class ChargePageVM {
	public Event<Object> UpdateEvent;
	private Object fireKey = new Object();
	private EventListener<Object> modelListener = new ModelListener();
	
	public ChargePageVM(){
		UpdateEvent = new Event<Object>(fireKey);
		Model.Instance().updateEvent.register(modelListener);
		
		stateUpdate();
	}
	
	public void stateUpdate()
	{
		currency = Model.Instance().getCurrency();
		priceDollarValue = Model.Instance().getPrice();
		price = priceDollarValue / Currency().Value();
	}
	
	private Double price = 0.0;
	private Double priceDollarValue = 0.0;
	public void Price(String value){
		if(RegexUtil.isMatch(value, RegexUtil.CommonPatterns.DECIMAL))
		{
			price = Math.min(Double.parseDouble(value), 10000.0);
			priceDollarValue = price * Model.Instance().getCurrency().Value();
		}
		UpdateEvent.fire(fireKey, null);
	}
	public Double Price(){
		return price;
	}
	public boolean PriceValid(){
		return true;
	}
	
	private Currency currency = Currency.Apples;
	public Currency Currency(){
		return currency;
	}
	
	public void CommitPrice(){
		Model.Instance().setPrice(priceDollarValue);
	}
	
	private class ModelListener extends EventListener<Object>{
		@Override
		public void onEvent(Object event) {
			try{
				
				if(event instanceof Currency){
					currency = Model.Instance().getCurrency();
					price = priceDollarValue / Currency().Value();
					UpdateEvent.fire(fireKey, null);
				}
			}catch (Exception ex){
	    		MessageManager.Instance().AddMessage(ex.toString(), true);
	    	}
		}
    }
}
