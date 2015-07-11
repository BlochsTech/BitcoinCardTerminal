package com.blochstech.bitcoincardterminal.Model.Communication;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import android.nfc.Tag;
import android.nfc.tech.IsoDep; //Must use IsoDep or Android will only enable 14443 protocol and not ISO 7816 (APDU commands).
import android.os.AsyncTask;
import android.util.Log;

import com.blochstech.bitcoincardterminal.Utils.ByteConversionUtil;
import com.blochstech.bitcoincardterminal.Utils.Tags;

//Living object pattern(My name: Object has own thread and takes care of itself. Takes update info calls and 
//omits events on the main thread, should be threadsafe during updates from the outside.)
//Async task: http://developer.android.com/reference/android/os/AsyncTask.html
abstract class NFCWrapper {
	private Timer heartBeatCheck;
	private TimerTask heartBeatCheckTask;
	private long lastHeartBeat = 0;
	
	private IsoDep cardApi = null;
	private IsoDep newCard = null;
	private byte[] task = null;
	private Thread workerThreadReference = null;
	
	protected abstract void onConnectionEvent(boolean value);
	protected abstract void onExceptionEvent(Exception value);
	protected abstract void onResponseEvent(byte[] value);
	
	protected NFCWrapper(){
		new CardHandlingTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Object[])null);
		//new CardHandlingTask().execute((Object[])null);
		
		heartBeatCheckTask = new TimerTask() {
		  @Override
		  public void run() {
		    if(lastHeartBeat < System.currentTimeMillis() - 10000){
		    	//We cannot enter leave because if this happens the workerthread is likely holding NfcA and is blocked.
		    	if(cardApi != null && cardApi.isConnected()){
		    		try {
						cardApi.close(); //Causes IOExceptions on the blocked using threads.
					} catch (Exception e) { //Catch exception as null pointers can happen here. (worker thread can set api to null also)
						cardApi = null;
						onConnectionEvent(false);
					}
		    	}else{
		    		onExceptionEvent(new Exception("NfcAWrapper workerthread is deadlocked (it seems), but was not using card."));
		    	}
		    }
		  }
		};
		heartBeatCheck = new Timer();
		heartBeatCheck.scheduleAtFixedRate(heartBeatCheckTask, 5000, 5000);
	}
	
	private String outsideThread = "outSideThread";
	protected void newCard(Tag card){ //Should cause card disconnect and replacing. Current task cancelled.
		Enter(outsideThread);
			newCard = IsoDep.get(card);
			if(workerThreadReference != null)
				workerThreadReference.interrupt();
		Leave(outsideThread);
	}
	
	protected void newTask(byte[] newTaskBytes){
		Enter(outsideThread);
		if(task == null){
			task = newTaskBytes;
			if(workerThreadReference != null)
				workerThreadReference.interrupt();
			Leave(outsideThread);
		}else{
			Leave(outsideThread);
			onExceptionEvent(new Exception("New task given although task is already in progress. Current task: " + ByteConversionUtil.byteArrayToHexString(task)
					+ "new task: " + ByteConversionUtil.byteArrayToHexString(newTaskBytes)));
		}
		
	}
	
	private String holder = null;
	private synchronized void Enter(String holdObj) //Do not allow same thread re-entry - Android re-uses threads!
	{
		long startTime = System.currentTimeMillis();
		
		while(holder != null && holdObj != holder)
		{
			try {
				this.wait(20000);
			} catch (InterruptedException e) {}
			if(System.currentTimeMillis() - startTime > 20000)
			{
				if(Tags.DEBUG)
					Log.e(Tags.APP_TAG, "FATAL: NfcAWrapper was deadlocked for 20 seconds. Attempting:" + holdObj + " Holding:" + holder);
			}
		}
		holder = holdObj; //Thread.currentThread();
	}
	private synchronized void Leave(String holdObj) 
	{
		if(holder == holdObj || holder == null)
		{
			holder = null;
			notifyAll();
		}
		else
		{
			if(Tags.DEBUG)
				Log.e(Tags.APP_TAG, "ERROR: Non lock holder tried to call Leave in NfcAWrapper. Attempting:" + holdObj + " Holding:" + holder);
		}
	}
	
	//Worker thread code:
	private class EventUpdate{
		EventUpdate(Exception exception){
			this.exception = exception;
		}
		EventUpdate(Boolean connected){
			this.connected = connected;
		}
		EventUpdate(byte[] response){
			this.response = response;
		}
		Exception exception;
		byte[] response;
		Boolean connected;
	}
	
	private String insideThread = "insideThread";
	private class CardHandlingTask extends AsyncTask<Object, EventUpdate, Object> { //Start in CardModel constructor.
		protected Object doInBackground(Object... startParams) {
		    boolean loop = true;
		    int connectFails = 0;
		    boolean actionTaken = false, failed = false;
		    byte[] response;
		    while(loop){
		    	Enter(insideThread);
		    		workerThreadReference = Thread.currentThread();
		    		//Let main thread know we are working:
					lastHeartBeat = System.currentTimeMillis();
			    	//If new card, disconnect:
			    	if(newCard != null){
			    		actionTaken = true;
			    		if(cardApi != null){
			    			try {
								cardApi.close();
							} catch (IOException e) {}
			    		}
			    		cardApi = newCard;
			    		newCard = null;
			    		task = null;
			    	}
			    	
			    	//If possible and not done, connect:
			    	if(cardApi != null && !cardApi.isConnected()){
			    		actionTaken = true;
			    		try {
			    			Leave(insideThread);
			    			failed = true;
							cardApi.connect(); //Never blocks thanks to workerthread/heartbeat design.
							failed = false;
							Enter(insideThread);
							cardApi.setTimeout(10000); //We will aim to make the unlock operation last 1 second.
							publishProgress(new EventUpdate(true));
						} catch (IOException e) {
							if(failed)
								Enter(insideThread);
							if(connectFails >= 3)
							{
								cardApi = null;
								connectFails = 0;
								publishProgress(new EventUpdate(false));
							}
							connectFails++;
						}
			    	}
			    	
			    	//If has task and connected, send task:
			    	String savedTask = "";
			    	if(task != null && cardApi != null && cardApi.isConnected()){
			    		actionTaken = true;
			    		try {
			    			Leave(insideThread);
			    			failed = true;
			    			savedTask = "Task1:" + ByteConversionUtil.byteArrayToHexString(task);
							response = cardApi.transceive(task);
							savedTask = savedTask + " Task2:" + ByteConversionUtil.byteArrayToHexString(task);
							failed = false;
							Enter(insideThread);
							task = null;
							publishProgress(new EventUpdate(response));
						} catch (Exception te) {
							if(failed)
								Enter(insideThread);
							Exception nfcEx = new Exception(te.toString() + " Task:"+ ByteConversionUtil.byteArrayToHexString(task));
							task = null;
							publishProgress(new EventUpdate(nfcEx));
							try {
								cardApi.close(); //If the transceive error was connection based we dont want to trust the present connection.
							} catch (IOException e) {
							}
							cardApi = null; //TODO: this was moved out of catch above for error reduction, check if ok. Seems like.
							publishProgress(new EventUpdate(false));
						}
			    	}
	    		Leave(insideThread);
		    		
		    	//If none of the above, leave and sleep.
	    		try {
	    			if(!actionTaken){
    					Thread.sleep(1000); //Interrupts will wake this up.
	    			}
				} catch (InterruptedException e) { //InterruptedException
				}
	    		actionTaken = false;
		    }
		    return null;
		}
		
		protected void onProgressUpdate(EventUpdate... eventArray) { //("..." means array) Code runs on main thread:
			//Reponse/error/connection:
			if(eventArray != null && eventArray.length > 0){
				EventUpdate event = eventArray[0];
				if(event.exception != null)
					onExceptionEvent(event.exception);
				if(event.connected != null)
					onConnectionEvent(event.connected);
				if(event.response != null)
					onResponseEvent(event.response);
			}
		}
	
		protected void onPostExecute(Object endResult) { //Code runs on main thread:
			//Do nothing, for now.
		}
	}
	//End Worker thread code.
}
