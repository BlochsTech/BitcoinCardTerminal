package com.blochstech.bitcoincardterminal.Model;

import android.nfc.Tag;

import com.blochstech.bitcoincardterminal.DataLayer.DAL;
import com.blochstech.bitcoincardterminal.Interfaces.Currency;
import com.blochstech.bitcoincardterminal.Interfaces.IModel;
import com.blochstech.bitcoincardterminal.Interfaces.Message;
import com.blochstech.bitcoincardterminal.Model.Communication.CurrencyApiConnector;
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
		updateEvent = new Event<Object>(fireKey, 3); //PinPageVM, ChargePageVM and SettingsPageVM
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
	public boolean setFee(Double feeDollarValue) {
		return paymentModel.setFee(feeDollarValue);
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

		Double feeDollarValue = paymentModel.getFee();
		Double btcVal = feeDollarValue / CurrencyApiConnector.DollarValue(Currency.Bitcoins);
		if(btcVal < AppSettings.MIN_FEE_BITCOINS)
		{
			feeDollarValue = AppSettings.MIN_FEE_BITCOINS * CurrencyApiConnector.DollarValue(Currency.Bitcoins);
			Model.Instance().setFee(feeDollarValue);
		}

		cardModel.setCharge(value / CurrencyApiConnector.DollarValue(Currency.Bitcoins), feeDollarValue / CurrencyApiConnector.DollarValue(Currency.Bitcoins), paymentModel.getAddress(), paymentModel.getCourtesyOK());
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
	public String getCardAddress() {
		return cardModel.getCardAddress();
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
