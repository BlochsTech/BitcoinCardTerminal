package com.blochstech.bitcoincardterminal.Utils;

import java.util.LinkedList;

import android.util.Log;

public abstract class EventListener<T> implements IEventListener<T> {
	private LinkedList<Event<T>> list = new LinkedList<Event<T>>();

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
				Log.e(Tags.APP_TAG, "FATAL: EventListener was deadlocked for 20 seconds.");
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
		}else
		{
			Log.e(Tags.APP_TAG, "ERROR: Non lock holder tried to call Leave in Utils class Event.");
		}
	}

	void addEvent(Event<T> eventDispatcher) //Don't use other than in Event
	{
		if(eventDispatcher != null)
		{
			list.add(eventDispatcher);
		}
	}
	void removeEvent(Event<T> eventDispatcher) //Don't use other than in Event
	{
		if(eventDispatcher != null)
		{
			while(list.contains(eventDispatcher))
			{
				list.remove(eventDispatcher);
			}
		}
	}

	public void unregisterAll()  //Call this on your listeners and then null the listener; when you want to destroy your listener-using-object.
	{
		int i = 0;
		Event<T> eventDispatcher;
		Enter();
		while(i < list.size()) {
			eventDispatcher = list.get(i);
			eventDispatcher.Enter();
			if(eventDispatcher != null)
			{
				eventDispatcher.removeListener(this); //Don't use other than in EventListener
			}
			eventDispatcher.Leave();
			i++;
		}
		list.clear();
		Leave();
	}

	public abstract void onEvent(T event);
}
