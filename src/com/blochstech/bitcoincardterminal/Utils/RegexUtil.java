package com.blochstech.bitcoincardterminal.Utils;

import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.util.Log;

public class RegexUtil {
	public class CommonPatterns{
		//33 long, no 0, i, o or L:
		//public static final String BITCOIN_ADDRESS = "(\\A|^)[13]{1}[1-9A-HJ-NP-Za-km-z]{33}(\\Z|$)"; Not technically correct, use validator function.
		//public static final String BITCOIN_ADDRESS_CHARS = "(\\A|^)[1-9A-HJ-NP-Za-km-z]{0,34}(\\Z|$)";
		public static final String DECIMAL = "(\\A|^)[0-9]+(\\.{1}[0-9]*){0,1}(\\Z|$)";
		public static final String BYTEHEX = "(\\A|^)([0-9a-fA-F]{2})*(\\Z|$)";
		public static final String BASE58_CHARS = "(\\A|^)[1-9A-HJ-NP-Za-km-z]*(\\Z|$)";
	}
	
	private static Matcher getMatcher(String input, String pattern){
		try{
			if(input == null || pattern == null)
				return null;
    	
	    	Pattern r = Pattern.compile(pattern);
	        Matcher m = r.matcher(input);
	        return m;
		}catch(Exception ex){
			Log.e(Tags.APP_TAG, "Regex getMatcher error: " + ex.toString());
			return null;
		}
	}
	
    public static boolean isMatch(String input, String pattern){
        Matcher m = getMatcher(input, pattern);
        if (m == null)
        	return false;
        
        try{
        	return m.find();
        }catch(Exception ex){
			Log.e(Tags.APP_TAG, "Regex isMatch error: " + ex.toString());
			return false;
		}
    }
    
    public static int countMatches(String input, String pattern){
    	try{
    		Matcher m = getMatcher(input, pattern);
	    	if (m == null)
	        	return 0;
	    	int count = 0;
	    	while(m.find()){
	    		count++;
	    	}
	    	return count;
    	}catch(Exception ex){
			Log.e(Tags.APP_TAG, "Regex countMatches error: " + ex.toString());
			return 0;
		}
    }
    
    public static LinkedList<Match> getMatches(String input, String pattern){
    	try{
    		LinkedList<Match> result = new LinkedList<Match>();
    		Matcher m = getMatcher(input, pattern);
	    	if (m == null)
	        	return result;

	    	while(m.find()){
	    		result.add(new Match(m.start(), m.end(), m.group()));
	    	}
	    	return result;
    	}catch(Exception ex){
			Log.e(Tags.APP_TAG, "Regex getMatches error: " + ex.toString());
			return new LinkedList<Match>();
		}
    }
    
    public static int getIndex(String input, String pattern){
    	return getIndex(input, pattern, false, 0, 0);
    }
    
    public static int getIndex(String input, String pattern, int startIndex){
    	return getIndex(input, pattern, false, startIndex, 0);
    }
    
    public static int getIndex(String input, String pattern, boolean after, int startIndex, int skips){
    	String tmpString = input.substring(startIndex);
    	Matcher m = getMatcher(tmpString, pattern);
        if (m == null)
        	return -1;
        
        try{
        	int skipped = 0;
        	while(skipped < skips && m.find()){
        		skipped++;
        	}
        	
        	if(m.find()){
        		if(after){
        			return startIndex+m.end();
        		}else{
        			return startIndex+m.start();
        		}
        	}else{
        		return -1;
        	}
        }catch(Exception ex){
			Log.e(Tags.APP_TAG, "Regex getIndex error: " + ex.toString());
			return -1;
		}
    }
}