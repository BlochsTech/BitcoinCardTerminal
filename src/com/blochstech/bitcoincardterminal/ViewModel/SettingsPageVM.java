package com.blochstech.bitcoincardterminal.ViewModel;

import com.blochstech.bitcoincardterminal.Model.Model;
import com.blochstech.bitcoincardterminal.Model.Communication.CurrencyApiConnector;
import com.blochstech.bitcoincardterminal.Model.Communication.TypeConverter;
import com.blochstech.bitcoincardterminal.Utils.Event;
import com.blochstech.bitcoincardterminal.Utils.RegexUtil;
import com.blochstech.bitcoincardterminal.Interfaces.Currency;

//VM class, holds state of a 1-1 corresponding view class.
//Is stored in PageManager.
//VMs should handle input and expose update events to the View. VMs can also call the model layer or utils.
public class SettingsPageVM {
	public SettingsPageVM(){
		UpdateEvent = new Event<Object>(this);
		
		//Init:
		address = Model.Instance().getAddress();
		addressValid = TypeConverter.verifyBase58CheckSum(address);
		//OLD CODE: RegexUtil.isMatch(address, RegexUtil.CommonPatterns.BITCOIN_ADDRESS);
		
		feeDollarValue = Model.Instance().getFee();
		
		currency = Model.Instance().getCurrency();
		fee = feeDollarValue / CurrencyApiConnector.DollarValue(ChosenCurrency());
		
		courtesyOK = Model.Instance().getCourtesyOK();
	}
	
	public Event<Object> UpdateEvent;
	
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
		UpdateEvent.fire(this, null);
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
		UpdateEvent.fire(this, null);
	}
	
	private Currency currency = Currency.Apples;
	public Currency ChosenCurrency(){
		return currency;
	}
	public void ChosenCurrency(Currency value){
		currency = value;
		Model.Instance().setCurrency(currency);
		
		
		fee = feeDollarValue / CurrencyApiConnector.DollarValue(value);
		
		UpdateEvent.fire(this, null);
	}
	
	private boolean courtesyOK = false;
	public boolean CourtesyOK(){
		return courtesyOK;
	}
	public void CourtesyOK(boolean value){
		courtesyOK = value;
		Model.Instance().setCourtesyOK(courtesyOK);
		UpdateEvent.fire(this, null);
	}
	
	private boolean addressValid = false;
	private boolean checksumValid = true;
	private boolean feeValid = true; //Amount too high.
	public boolean IsValid(){
		return addressValid && feeValid;
	}
	public boolean IsAddressValid(){
		return addressValid && checksumValid;
	}
	
	/*public void Submit(){ //TODO: Save simply at valid data and remove OK btn etc..
		boolean success = true;
		String error = null;
		if(IsValid())
		{
			success = success && Model.Instance().setAddress(address);
			checksumValid = success;
			if(!success){
				 error = "Address checksum mismatch, no settings saved.";
			}
		}
		
		if(!IsValid() || !success){
			if(error == null)
				error = !addressValid
					? "Address invalid, no settings saved."
					: "Unknown error saving settings.";
			
			MessageManager.Instance().AddMessage(error, true);
		}
		UpdateEvent.fire(this, null);
	}*/
	
}