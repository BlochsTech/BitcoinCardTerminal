package com.blochstech.bitcoincardterminal.ViewModel.ViewStateManagers;

import java.util.LinkedList;
import java.util.Queue;

import com.blochstech.bitcoincardterminal.Interfaces.Message;
import com.blochstech.bitcoincardterminal.Interfaces.Message.MessageType;
import com.blochstech.bitcoincardterminal.Model.Model;
import com.blochstech.bitcoincardterminal.Utils.EventListener;
import com.blochstech.bitcoincardterminal.Utils.Tags;

import android.util.Log;

//The core idea is to avoid any one View having to know all the other Views in order to update say a global status icon,
	//message or page number..
public class MessageManager {
	//Singleton pattern:
	private static MessageManager instance = null;
	public static MessageManager Instance()
	{
		if(instance == null)
		{
			instance = new MessageManager();
		}
		return instance;
	}
	private MessageManager()
	{
		Model.Instance().MessageEventReference().register(modelMessageListener);
	}
	//Singleton pattern end.
	
	private Queue<String> messages = new LinkedList<String>();
	private final int maxMessages = 500;
	private EventListener<Message> modelMessageListener = new ModelMessageListener();
	
	public void AddMessage(String msg, boolean isError){
		AddMessage(msg, isError, false);
	}
	
	@SuppressWarnings("unused")
	public void AddMessage(String msg, boolean isError, boolean isWarning){
		//TODO: Add to list + android log... rest can wait... post log guide in this class.
		if(msg != null && !msg.isEmpty() && Tags.DEBUG){
			messages.add(msg);
			if(isError){
				Log.e(Tags.APP_TAG, msg);
			}else if(isWarning){
				Log.w(Tags.APP_TAG, msg);
			}else{
				Log.i(Tags.APP_TAG, msg);
			}
			if(messages.size() > maxMessages)
				messages.poll();
		}
	}
	
	public void AddMessage(String  msg){
		AddMessage(msg, false);
	}
	
	private class ModelMessageListener extends EventListener<Message>{
		@Override
		public void onEvent(Message event) {
			AddMessage(event.Message, event.Type == MessageType.Error, event.Type == MessageType.Warning);
		}
	}
}
