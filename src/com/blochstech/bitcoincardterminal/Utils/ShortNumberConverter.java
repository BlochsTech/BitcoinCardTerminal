package com.blochstech.bitcoincardterminal.Utils;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

public class ShortNumberConverter {
	
	public static String ToShort(Double value){
		NumberFormat formatter = new DecimalFormat();

	    formatter = new DecimalFormat("0.000E00");
	    String temp = formatter.format(value);
	    
	    String temp2 = temp.substring(0, 5);
	    Double temp3 = Double.parseDouble(temp2);
	    formatter = new DecimalFormat("0.00");
	    formatter.setRoundingMode(RoundingMode.HALF_UP);
	    temp = formatter.format(temp3) + temp.substring(5);
	    
	    if(!temp.contains("E-")){
	    	temp = temp.substring(0, temp.indexOf("E")+1) + "0" + temp.substring(temp.indexOf("E")+1, temp.length());
	    }

	    return temp.toLowerCase(Locale.ENGLISH);
	}
	/*public static Double FromShort(String value){
		
	}*/
}
