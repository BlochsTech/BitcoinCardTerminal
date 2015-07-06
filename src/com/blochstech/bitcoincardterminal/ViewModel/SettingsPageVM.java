package com.blochstech.bitcoincardterminal.ViewModel;

import com.blochstech.bitcoincardterminal.Model.Model;
import com.blochstech.bitcoincardterminal.Model.Communication.CurrencyApiConnector;
import com.blochstech.bitcoincardterminal.Model.Communication.TypeConverter;
import com.blochstech.bitcoincardterminal.Utils.Event;
import com.blochstech.bitcoincardterminal.Utils.EventListener;
import com.blochstech.bitcoincardterminal.Utils.RegexUtil;
import com.blochstech.bitcoincardterminal.ViewModel.ViewStateManagers.MessageManager;
import com.blochstech.bitcoincardterminal.Interfaces.Currency;

//VM class, holds state of a 1-1 corresponding view class.
//Is stored in PageManager.
//VMs should handle input and expose update events to the View. VMs can also call the model layer or utils.
public class SettingsPageVM {

	private EventListener<Object> modelListener = new ModelListener();

	public Event<Object> UpdateEvent;
	private Object fireKey = new Object();
	
	public SettingsPageVM(){
		UpdateEvent = new Event<Object>(fireKey);
		Model.Instance().updateEvent.register(modelListener);
		
		//Init:
		address = Model.Instance().getAddress();
		addressValid = TypeConverter.verifyBase58CheckSum(address);
		//OLD CODE: RegexUtil.isMatch(address, RegexUtil.CommonPatterns.BITCOIN_ADDRESS);
		
		feeDollarValue = Model.Instance().getFee();
		
		currency = Model.Instance().getCurrency();
		fee = feeDollarValue / CurrencyApiConnector.DollarValue(ChosenCurrency());
		
		courtesyOK = Model.Instance().getCourtesyOK();
	}
	
	private String address = "";
	public String Address(){
		return address;
	}
	public void Address(String value){
		if(RegexUtil.isMatch(value, RegexUtil.CommonPatterns.BASE58_CHARS))
		{
			address = value;
		}
		addressValid = TypeConverter.verifyBase58CheckSum(address);
		if(addressValid){
			addressValid = Model.Instance().setAddress(address);
		}
		UpdateEvent.fire(fireKey, null);
	}
	
	private boolean useNfc = false;
	public void UseNFC(boolean value){
		useNfc = value;
		String cardAddress = Model.Instance().getCardAddress();
		if(useNfc && cardAddress != null && cardAddress.length() > 0){
			Address(cardAddress);
			useNfc = false;
			UpdateEvent.fire(fireKey, null);
		}
	}
	
	private Double fee = 0.0;
	private Double feeDollarValue = 0.0;
	public Double Fee(){
		return fee;
	}
	public void Fee(String value){
		if(RegexUtil.isMatch(value, RegexUtil.CommonPatterns.DECIMAL))
		{
			String cleanValue = value.replace(",", ".");
			fee = Math.min(Double.parseDouble(cleanValue), 10000.0);
			feeDollarValue = fee * CurrencyApiConnector.DollarValue(ChosenCurrency());
			Model.Instance().setFee(feeDollarValue);
		}
		UpdateEvent.fire(fireKey, null);
	}
	
	private Currency currency = Currency.Apples;
	public Currency ChosenCurrency(){
		return currency;
	}
	public void ChosenCurrency(Currency value){
		currency = value;
		Model.Instance().setCurrency(currency);
		
		
		fee = feeDollarValue / CurrencyApiConnector.DollarValue(value);
		
		UpdateEvent.fire(fireKey, null);
	}
	
	private boolean courtesyOK = false;
	public boolean CourtesyOK(){
		return courtesyOK;
	}
	public void CourtesyOK(boolean value){
		courtesyOK = value;
		Model.Instance().setCourtesyOK(courtesyOK);
		UpdateEvent.fire(fireKey, null);
	}
	
	private boolean addressValid = false;
	private boolean feeValid = true; //Amount too high.
	public boolean IsValid(){
		return addressValid && feeValid;
	}
	public boolean IsAddressValid(){
		return addressValid;
	}
	
	private class ModelListener extends EventListener<Object>{
		@Override
		public void onEvent(Object event) {
			try{
				String cardAddress = Model.Instance().getCardAddress();
				if(useNfc && cardAddress != null && cardAddress.length() > 0){
					useNfc = false;
					Address(cardAddress);
					UpdateEvent.fire(fireKey, null);
				}
			}catch (Exception ex){
	    		MessageManager.Instance().AddMessage(ex.toString(), true);
	    	}
		}
    }
}