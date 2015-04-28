package com.blochstech.bitcoincardterminal.Utils;

import android.util.Log;

public class ParseHelper {
	
	public static String RegexSubstring(String input, String start, boolean startAfterStart){
		return RegexSubstring(input, start, null, startAfterStart);
	}
	public static String RegexSubstring(String input, String start, String end){
		return RegexSubstring(input, start, end, true);
	}
	public static String RegexSubstring(String input, String start, String end, boolean startAfterStart){
		return RegexSubstring(input, start, end, startAfterStart, 0);
	}
	public static String RegexSubstring(String input, String start, String end, boolean startAfterStart, int matchNumber){
		try{
			if(input == null || start == null || start.isEmpty())
				return null;
			
			int startPos = RegexUtil.getIndex(input, start, startAfterStart, 0, matchNumber);
			if(startPos == -1)
				return null;
			
			int endPos = end == null || end.isEmpty() 
						? input.length() 
						: RegexUtil.getIndex(input, end, startPos);
			if(endPos == -1)
				endPos = input.length();
			
			return input.substring(startPos, endPos);
		}catch(Exception ex){
			Log.e(Tags.APP_TAG, "Error in ParseHelper RegexSubstring: " + ex.toString());
			return null;
		}
	}
}
