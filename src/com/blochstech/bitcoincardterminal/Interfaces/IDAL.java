package com.blochstech.bitcoincardterminal.Interfaces;

import com.blochstech.bitcoincardterminal.Utils.Event;

public interface IDAL {
	//Events:
	public Event<Message> MessageEventReference();
	
	//Database mehtods:
	public void setReceiveAddress(String address);
	public String getReceiveAddress();
	
	public void setFee(String fee);
	public String getFee();
	
	public void setCourtesyOK(boolean courtesyOK);
	public boolean getCourtesyOK();
	
	public void setCurrency(int currency);
	public int getCurrency();
}
