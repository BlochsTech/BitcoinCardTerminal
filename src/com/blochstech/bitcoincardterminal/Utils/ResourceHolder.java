package com.blochstech.bitcoincardterminal.Utils;

public class ResourceHolder {
	//private Object lockKey; Usually as key in hashmap or unnecessary.
	private Object holder;
	private Thread holdingThread;
	
	//public Object LockKey(){ return lockKey; } //The value or resource being locked upon.
	public Object Holder(){ return holder; } //The object with access (only for extra restrictive cases).
	public Thread HoldingThread(){ return holdingThread; } //The thread that has the lock, re-entry should generally be allowed.
	
	public ResourceHolder(Object holder, Thread holdingThread){
		//this.lockKey = lockKey;
		this.holder = holder;
		this.holdingThread = holdingThread;
	}
	public ResourceHolder(Object holder){
		//this.lockKey = lockKey;
		this.holder = holder;
		this.holdingThread = Thread.currentThread();
	}
}
