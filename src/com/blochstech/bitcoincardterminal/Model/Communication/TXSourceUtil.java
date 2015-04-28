package com.blochstech.bitcoincardterminal.Model.Communication;

import java.util.ArrayList;
import java.util.Locale;

import android.util.Log;

import com.blochstech.bitcoincardterminal.Model.Communication.TXSourceId;
import com.blochstech.bitcoincardterminal.Utils.Tags;

public class TXSourceUtil {
	public static boolean containsSource(ArrayList<TXSourceId> list, String hash){
		try{
			if(hash == null || list == null)
				return false;
			
			for(int i = 0; i < list.size(); i++){
				if(list.get(i).TXHash.toLowerCase(Locale.ENGLISH).equals(hash.toLowerCase(Locale.ENGLISH))
						&& (list.get(i).Verified || list.get(i).Unusable))
					return true;
			}
		}catch(Exception e){
			Log.e(Tags.APP_TAG, "ContainsSource failed: " + e.toString());
		}
		return false;
	}
}
