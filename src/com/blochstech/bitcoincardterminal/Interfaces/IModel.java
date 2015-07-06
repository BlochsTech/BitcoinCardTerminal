package com.blochstech.bitcoincardterminal.Interfaces;

import com.blochstech.bitcoincardterminal.Utils.Event;

import android.nfc.Tag;

public interface IModel {
	//Events:
	public Event<Message> MessageEventReference();
	
	public void persistData();
	
	//Temp data:
	public void setCard(Tag tag);
	
	public void setPrice(Double value);
	public Double getPrice();
	
	public void setPin(int value);
	
	public String getShortCharge();
	
	//Permanent data:
	public boolean setAddress(String address);
	public String getAddress();
	
	public boolean setFee(Double fee);
	public Double getFee();
	
	public void setCourtesyOK(boolean courtesyOK);
	public boolean getCourtesyOK();
	
	public void setCurrency(Currency currency);
	public Currency getCurrency();
	
	public String getCardMessage();
	
	public String getVignereCode();
	
	public boolean getPinRequired();

	public String getCardAddress();
}
