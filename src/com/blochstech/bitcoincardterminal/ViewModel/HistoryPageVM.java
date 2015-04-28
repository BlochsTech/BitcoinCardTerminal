package com.blochstech.bitcoincardterminal.ViewModel;

import com.blochstech.bitcoincardterminal.Utils.Event;

public class HistoryPageVM {
	public Event<Object> UpdateEvent;
	
	public HistoryPageVM(){
		UpdateEvent = new Event<Object>(this);
		
		//TODO: Load from model.
		//Keep PIN saved in VM only until used.
	}
}
