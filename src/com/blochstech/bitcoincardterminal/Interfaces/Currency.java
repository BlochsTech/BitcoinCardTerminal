package com.blochstech.bitcoincardterminal.Interfaces;

import android.util.Log;
import android.widget.Toast;

import com.blochstech.bitcoincardterminal.MainActivity;
import com.blochstech.bitcoincardterminal.Utils.Tags;

public enum Currency {
	MicroBitcoins(0),
	Apples(1),
	Yuans(2),
	Dollars(3),
	Euros(4),
	Bitcoins(5);
	
	private int value;
	private Currency(int value) {
	  this.value = value;
	}
	public int getValue() {
      return value;
	}
	public static Currency Convert(int value){
		if(0 <= value && value < Currency.values().length)
			return Currency.values()[value];
		else{
			//TODO: Better unified logging util.
			if(Tags.DEBUG)
				Log.e(Tags.APP_TAG, "Attempted to convert invalid integer " + value + " to currency.");
			Toast.makeText(MainActivity.GetMainContext(), "ERR: Attempted to convert invalid integer " + value + " to currency.", Toast.LENGTH_LONG).show();
			return Currency.MicroBitcoins;
		}
	}
	public String Description(){
		switch(value){
			case 0:
				return "microbitcoins";
			case 1:
				return "apples";
			case 2:
				return "yuans";
			case 3:
				return "dollars";
			case 4:
				return "euros";
			case 5:
				return "bitcoins";
			default:
				return "apples";
		}
	}
	public String Symbol(){
		switch(value){
			case 0:
				return "µ฿";
			case 1:
				return "Æ";
			case 2:
				return "¥";
			case 3:
				return "$";
			case 4:
				return "€";
			case 5:
				return "฿";
			default:
				return "Æ";
		}
	}
	
	/*public Double Value(){ //TODO: Move to model as an interface object cannot access the internet...
		//Currency value in apples they can buy with apples at 0.36 2014 dollars.
		//Fiat assumed to decrease 90% in  20 years and bitcoin to reach M2 - 21 million 2014 dollars.
		//Even without crypto currency disturbing things, fiat has declined 99% the past 100 years.
		//Apples assumed to be stable for the same time frame.
		
		long millis = System.currentTimeMillis();
		Double years = (Double) ((millis)/(365.25*24.0*60.0*60.0*1000) - 44);
		years = Math.min(20, years);
		//Double e = 2.718282;
		//Double fiatDegrowth = -0.115; //Goes to 0.1 in 20 years. TODO:  Replace with sigmoid functions...
		//Double btcGrowth = 0.4976; //Goes to 21000 in 20 years.
		//Double btcValue = 0.001902, dollarValue = 2.78 , yuanValue = 0.46, euroValue = 3.81; //Show values in dollar?
		Double microbtcValue = 0.000235, dollarValue = 1.0 , yuanValue = 0.161, euroValue = 1.08, appleValue = 0.36, btcValue = 235.0;
		Double val;
		
		switch(value){
			case 0:
				val = microbtcValue * 1;//Math.pow(e, btcGrowth*years); While  such functions may be good long term predictions, short term its useless.
				return val;
			case 1:
				return appleValue;
			case 2:
				val = yuanValue * 1;//Math.pow(e, fiatDegrowth*years);
				return val;
			case 3:
				val = dollarValue * 1;//Math.pow(e, fiatDegrowth*years);
				return val;
			case 4:
				val = euroValue * 1;//Math.pow(e, fiatDegrowth*years);
				return val;
			case 5:
				val = btcValue * 1;
				return val;
			default:
				return appleValue;
		}
	}*/
}
