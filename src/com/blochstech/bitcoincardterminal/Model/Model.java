package com.blochstech.bitcoincardterminal.Model;

import android.nfc.Tag;

import com.blochstech.bitcoincardterminal.DataLayer.DAL;
import com.blochstech.bitcoincardterminal.Interfaces.Currency;
import com.blochstech.bitcoincardterminal.Interfaces.IModel;
import com.blochstech.bitcoincardterminal.Interfaces.Message;
import com.blochstech.bitcoincardterminal.Utils.Event;
import com.blochstech.bitcoincardterminal.Utils.EventListener;

//If issues appear with concurrency, simply make this Model class thread safe (see events in util), it is the only entry to the model package.
public class Model implements IModel{
	//Singleton pattern:
	private static Model instance = null;
	public static Model Instance()
	{
		if(instance == null)
		{
			instance = new Model();
		}
		return instance;
	}
	private Model()
	{
		messageEvent = new Event<Message>(fireKey);
		updateEvent = new Event<Object>(fireKey, 2);
		DAL.Instance().MessageEventReference().register(messageListener);
		paymentModel = new PaymentModel();
		cardModel = new CardModel();
		cardModel.MessageEventReference().register(messageListener);
	}
	//Singleton pattern end.
	
	private PaymentModel paymentModel;
	private CardModel cardModel;
	
	private EventListener<Message> messageListener = new MessageListener();
	
	public Event<Message> messageEvent;
	public Event<Object> updateEvent; //Whatever object was updated or an enum if needed.
	
	private Object fireKey = new Object();
	
	void fireUpdate(Object event){ //Accesible to model only.
		updateEvent.fire(fireKey, event);
	}
	
	@Override
	public void persistData(){
		paymentModel.persistData();
		cardModel.persistData();
	}
	
	@Override
	public void setCard(Tag tag) {
		cardModel.setCard(tag);
	}
	
	@Override
	public void setPin(int value){
		cardModel.setPin(value);
	}
	
	@Override
	public boolean setAddress(String address) {
		return paymentModel.setAddress(address);
	}
	@Override
	public String getAddress() {
		return paymentModel.getAddress();
	}
	
	@Override
	public boolean setFee(Double fee) {
		return paymentModel.setFee(fee);
	}
	@Override
	public Double getFee() {
		return paymentModel.getFee();
	}
	
	@Override
	public void setCourtesyOK(boolean courtesyOK) {
		paymentModel.setCourtesyOK(courtesyOK);
	}
	@Override
	public boolean getCourtesyOK() {
		return paymentModel.getCourtesyOK();
	}
	
	@Override
	public void setCurrency(Currency currency) {
		paymentModel.setCurrency(currency);
	}
	@Override
	public Currency getCurrency() {
		return paymentModel.getCurrency();
	}
	
	@Override
	public void setPrice(Double value) {
		paymentModel.setPrice(value);
		cardModel.setCharge(value / Currency.Bitcoins.Value(), paymentModel.getFee() / Currency.Bitcoins.Value(), paymentModel.getAddress(), paymentModel.getCourtesyOK());
	}
	@Override
	public Double getPrice() {
		return paymentModel.getPrice();
	}
	
	@Override
	public String getCardMessage() {
		return cardModel.getCardMessage();
	}
	
	@Override
	public String getShortCharge() {
		return cardModel.getShortCharge();
	}
	
	@Override
	public String getVignereCode() {
		return cardModel.getVignereCode();
	}
	
	@Override
	public boolean getPinRequired() {
		return cardModel.getPinRequired();
	}
	
	@Override
	public Event<Message> MessageEventReference() {
		return messageEvent;
	}

	private class MessageListener extends EventListener<Message>{
		@Override
		public void onEvent(Message event) {
			messageEvent.fire(fireKey, event);
		}
	}
}
