package com.blochstech.bitcoincardterminal.Utils;

import java.util.LinkedList;

import android.util.Log;

public class Event<T> {
	private LinkedList<EventListener<T>> list = new LinkedList<EventListener<T>>();
	private Object key;
	private int maxSubscribers = 1; //<= 0 -> unlimited.

	//CONCURRENCY/MONITOR PATTERN:
	private Thread holder = null;
	synchronized void Enter() //Allow same thread re-entry.
	{
		long startTime = System.currentTimeMillis();
		
		while(holder != null)
		{
			try {
				this.wait();
			} catch (InterruptedException e) {}
			if(System.currentTimeMillis() - startTime > 20000)
			{
				Log.e(Tags.APP_TAG, "FATAL: Event was deadlocked for 20 seconds.");
			}
		}
		holder = Thread.currentThread();
	}
	synchronized void Leave() 
	{
		if(holder == Thread.currentThread())
		{
			holder = null;
			notifyAll();
		}
		else
		{
			Log.e(Tags.APP_TAG, "ERROR: Non lock holder tried to call Leave in Utils class Event.");
		}
	}
	/*private int Semaphore = 0;
	synchronized void Enter() throws Exception //TODO: Allow same thread re-entry.
	{
		while(Semaphore > 0)
		{
			try {
				this.wait();
			} catch (InterruptedException e) {}
		}
		Semaphore++;
		if(Semaphore > 1 || Semaphore < 0){
			throw new Exception("Synchronization error in Event.");
		}
	}
	synchronized void Leave() throws Exception
	{
		Semaphore--;
		if(Semaphore > 1 || Semaphore < 0){
			throw new Exception("Synchronization error in Event.");
		}
		notifyAll();
	}*/

	public Event(Object fireKey)
	{
		key = fireKey;
		maxSubscribers = 1;
	}
	public Event(Object fireKey, int maxSubscribers)
	{
		key = fireKey;
		this.maxSubscribers = maxSubscribers;
	}

	public void register(EventListener<T> listener) 
	{
		if(listener != null)
		{
			listener.Enter(); //Always same order listener->eventDispatcher to avoid deadlocks.
			Enter();
			
			if(!list.contains(listener)){
				list.add(listener);
				listener.addEvent(this);
			}
			if(maxSubscribers > 0 && list.size() > maxSubscribers){
				Log.e(Tags.APP_TAG, "Unexpected number of subscribers.");
			}
			
			Leave();
			listener.Leave();
		}
	}

	public void unregister(EventListener<T> listener)
	{
		if(listener != null)
		{
			listener.Enter();
			Enter();
			
			while(list.contains(listener)){
				list.remove(listener);
				listener.removeEvent(this);
			}
			
			Leave();
			listener.Leave();
		}
	}
	void removeListener(EventListener<T> listener) //Don't use other than in EventListener
	{
		if(listener != null)
		{
			while(list.contains(listener)){
				list.remove(listener);
			}
		}
	}

	public void fire(Object fireKey, T event) 
	{
		if(fireKey == key)
		{
			int i = 0;
			EventListener<T> listener;
			Enter();
			while(i < list.size()) {
				listener = list.get(i);
				Leave();
				listener.Enter();
				Enter();
				
				try{
					listener.onEvent(event);
				}catch (Exception e){
					Log.e(Tags.APP_TAG, "Failed to execute eventListener's onEvent method: " + e.toString());
				}
				
				Leave();
				listener.Leave();
				Enter();
				i++;
			}
			Leave();
		}else{
			if(fireKey != null && key != null)
				Log.e(Tags.APP_TAG, "Fire method called with wrong key. Correct key: " + key.toString() + ". Key used: " + fireKey.toString());
		}
	}

	public void unloadEvent(Object fireKey)  //Call this when you want to destroy your object owning the event. DON'T null the event though.
	{
		if(fireKey == key)
		{
			Enter();
			boolean repeat = list.size() > 0;
			
			while(repeat)//Listener then event! (Prevents deadlock)
			{
				
				if(list.get(0) != null){
					EventListener<T> listener = list.get(0);
					Leave(); //Prevents deadlock
					listener.Enter();
					Enter();
					list.get(0).removeEvent(this);
					list.remove(0);
					repeat = list.size() > 0;
					Leave();
					listener.Leave();
					Enter(); //Has to match the ending Leave().
				}else
				{
					list.remove(0);
					repeat = list.size() > 0;
				}
			}
			Leave();
		}
	}
}
