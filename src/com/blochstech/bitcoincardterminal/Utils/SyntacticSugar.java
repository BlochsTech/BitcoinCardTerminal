package com.blochstech.bitcoincardterminal.Utils;

public class SyntacticSugar {
	@SuppressWarnings("unchecked")
	public static <T> T castAs(Object castee){
		try{
			return (T) castee;
		}catch(Exception ex){
			return null;
		}
	}
	
	public static boolean EqualWithinMargin(Double value1, Double value2, Double margin){
		return value1 * (1+margin) >= value2 && value2 * (1+margin) >= value1;
	}
}
