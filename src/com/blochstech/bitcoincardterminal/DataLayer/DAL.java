package com.blochstech.bitcoincardterminal.DataLayer;

import android.util.Log;

import com.blochstech.bitcoincardterminal.Interfaces.IDAL;
import com.blochstech.bitcoincardterminal.Interfaces.Message;
import com.blochstech.bitcoincardterminal.Interfaces.Message.MessageType;
import com.blochstech.bitcoincardterminal.Utils.*;

public class DAL implements IDAL {
	//Singleton pattern:
	private static IDAL instance = null;
	public static IDAL Instance()
	{
		if(instance == null)
		{
			instance = new DAL();
		}
		return instance;
	}
	private DAL()
	{
		try {
			messageEvent = new Event<Message>(this);
			addressCache = new GenericFileCache<String>(addressCachePath, "");
			feeCache = new GenericFileCache<String>(feeCachePath, "100");
			courtesyOKCache = new GenericFileCache<Boolean>(courtesyOKCachePath, false);
			currencyCache = new GenericFileCache<Integer>(currencyCachePath, 0);
		} catch (Exception e) {
			Log.e(Tags.APP_TAG, "DAL failed to initialize. Error: " + e.toString());
		}
	}
	//Singleton pattern end.
		
	private GenericFileCache<String> addressCache;
	private GenericFileCache<String> feeCache;
	private GenericFileCache<Boolean> courtesyOKCache;
	private GenericFileCache<Integer> currencyCache;
	private final String addressCachePath = "/BitcoinTerminal/Data/receiverAddress.bin";
	private final String feeCachePath = "/BitcoinTerminal/Data/fee.bin";
	private final String courtesyOKCachePath = "/BitcoinTerminal/Data/courtesyOK.bin";
	private final String currencyCachePath = "/BitcoinTerminal/Data/currency.bin";
	//TODO: Permission.
	
	public Event<Message> messageEvent;
	
	@Override
	public void setReceiveAddress(String address) {
		try {
			addressCache.Open(this);
			addressCache.set(this, address);
			addressCache.Close(this);
		} catch (Exception e) {
			messageEvent.fire(this, new Message("Error: " + e.getMessage() + e.toString(), MessageType.Error));
		}
	}
	@Override
	public String getReceiveAddress() {
		try {
			addressCache.Open(this);
			String tmp = addressCache.get(this);
			addressCache.Close(this);
			return tmp;
		} catch (Exception e) {
			messageEvent.fire(this, new Message("Error: " + e.getMessage() + e.toString(), MessageType.Error));
		}
		return null;
	}
	
	@Override
	public void setFee(String fee) {
		try {
			feeCache.Open(this);
			feeCache.set(this, fee);
			feeCache.Close(this);
		} catch (Exception e) {
			messageEvent.fire(this, new Message("Error: " + e.getMessage() + e.toString(), MessageType.Error));
		}
	}
	@Override
	public String getFee() {
		try {
			feeCache.Open(this);
			String tmp = feeCache.get(this);
			feeCache.Close(this);
			return tmp;
		} catch (Exception e) {
			messageEvent.fire(this, new Message("Error: " + e.getMessage() + e.toString(), MessageType.Error));
		}
		return "0.0";
	}
	
	@Override
	public void setCourtesyOK(boolean courtesyOK) {
		try {
			courtesyOKCache.Open(this);
			courtesyOKCache.set(this, courtesyOK);
			courtesyOKCache.Close(this);
		} catch (Exception e) {
			messageEvent.fire(this, new Message("Error: " + e.getMessage() + e.toString(), MessageType.Error));
		}
	}
	@Override
	public boolean getCourtesyOK() {
		try {
			courtesyOKCache.Open(this);
			Boolean tmp = courtesyOKCache.get(this);
			courtesyOKCache.Close(this);
			return tmp;
		} catch (Exception e) {
			messageEvent.fire(this, new Message("Error: " + e.getMessage() + e.toString(), MessageType.Error));
		}
		return false;
	}
	
	@Override
	public void setCurrency(int currency) {
		try {
			currencyCache.Open(this);
			currencyCache.set(this, currency);
			currencyCache.Close(this);
		} catch (Exception e) {
			messageEvent.fire(this, new Message("Error: " + e.getMessage() + e.toString(), MessageType.Error));
		}
	}
	@Override
	public int getCurrency() {
		try {
			currencyCache.Open(this);
			int tmp = currencyCache.get(this);
			currencyCache.Close(this);
			return tmp;
		} catch (Exception e) {
			messageEvent.fire(this, new Message("Error: " + e.getMessage() + e.toString(), MessageType.Error));
		}
		return 0;
	}
	
	@Override
	public Event<Message> MessageEventReference() {
		return messageEvent;
	}
}
