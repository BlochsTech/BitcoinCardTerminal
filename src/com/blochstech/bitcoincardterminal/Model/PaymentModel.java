package com.blochstech.bitcoincardterminal.Model;

import com.blochstech.bitcoincardterminal.DataLayer.DAL;
import com.blochstech.bitcoincardterminal.Interfaces.Currency;

//State of address, fee, price and currency - loaded at startup.
//Set price, convert, PIN, finish. 
class PaymentModel {
	PaymentModel() //CONSTRUCT/SAVE IN MAIN MODEL. MAIN MODEL CALLS METHODS ON SUBMODEL.
	{
		//Disk access only at pause and startup.
		receivingAddress = DAL.Instance().getReceiveAddress();
		feeDollarValue = Double.parseDouble(DAL.Instance().getFee());
		courtesyOK = DAL.Instance().getCourtesyOK();
		currency = Currency.Convert(DAL.Instance().getCurrency());
		if(receivingAddress == null || receivingAddress.equals(""))
			receivingAddress = "1AaJJCgqaebkQ7u7NahqG5dE65GPa8YTRB"; //Donation address of the Danish Bitcoin Foundation
	}
	
	void persistData(){ //Call onPause via MainAcitvity and Model.
		//Disk access only at pause and startup.
		DAL.Instance().setReceiveAddress(receivingAddress);
		DAL.Instance().setFee(feeDollarValue.toString());
		DAL.Instance().setCourtesyOK(courtesyOK);
		DAL.Instance().setCurrency(currency.getValue());
	}
	
	private String receivingAddress = "";
	private Double feeDollarValue = 0.0;
	private boolean courtesyOK = false;
	private Currency currency = Currency.Apples;
	
	private Double priceDollarValue = 0.0;
	
	void setPrice(Double value){
		priceDollarValue = value;
		Model.Instance().fireUpdate(priceDollarValue);
	}
	Double getPrice(){
		return priceDollarValue;
	}
	
	boolean setAddress(String address) {
		//TODO: Check checksum!
		receivingAddress = address;
		return true;
	}
	String getAddress() {
		return receivingAddress;
	}
	
	boolean setFee(Double fee) {  //We expect fee as dollar value now. Above 0.40$ is not allowed.
		if(fee <= 0.40){
			feeDollarValue = fee;
			Model.Instance().fireUpdate(feeDollarValue);
			return true;
		}else{
			return false;
		}
	}
	Double getFee() {
		return feeDollarValue;
	}
	
	void setCourtesyOK(boolean courtesyOK) {
		this.courtesyOK = courtesyOK;
		Model.Instance().fireUpdate(courtesyOK);
	}
	boolean getCourtesyOK() {
		return courtesyOK;
	}
	
	void setCurrency(Currency currency) {
		this.currency = currency;
		Model.Instance().fireUpdate(currency);
	}
	Currency getCurrency() {
		return currency;
	}
}
