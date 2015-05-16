package com.blochstech.bitcoincardterminal.DataLayer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;

import android.os.Environment;
import android.util.Log;

import com.blochstech.bitcoincardterminal.Utils.ResourceHolder;
import com.blochstech.bitcoincardterminal.Utils.SyntacticSugar;
import com.blochstech.bitcoincardterminal.Utils.Tags;

public class GenericFileCache<T>{
	private static HashMap<String, ResourceHolder> pathsInUse = new HashMap<String, ResourceHolder>();
	private String FileCachePath;
	
	//PUBLIC USE METHODS:
	public GenericFileCache (String fileCachePath, T initialValue) throws Exception{
		FileCachePath = fileCachePath != null ? Environment.getExternalStorageDirectory() + fileCachePath : "";
		initCache(initialValue);
		if(!(initialValue instanceof Serializable))
		{
			throw new Exception("The value attempted used with the cache is not serializable. Path: " + FileCachePath);
		}
	}
	
	public synchronized void Open(Object holder) throws Exception{
		ResourceHolder holderObj;
		long startTime = System.currentTimeMillis();
		boolean wait = true;
		while(wait){
			holderObj = pathsInUse.get(FileCachePath);
			if(holderObj == null){
				pathsInUse.put(FileCachePath, new ResourceHolder(holder));
				wait = false;
			} else if (holderObj.Holder() == holder && holderObj.HoldingThread() == Thread.currentThread()){
				wait = false;
			} else {
				if(startTime + 20000 < System.currentTimeMillis())
					throw new Exception("Deadlock for 20 seconds while opening generic cache. Path: " + FileCachePath);
				try {
					this.wait();
				} catch (InterruptedException e) {}
			}
		}
		//Permit re-entry:
		return;
	}
	
	public synchronized void Close(Object holder){ //TODO: Find all usages and make sure they are called even if exception happens.
		if(isHolder(holder)){
			pathsInUse.remove(FileCachePath);
			this.notifyAll();
		}else{
			if(Tags.DEBUG)
				Log.e(Tags.APP_TAG, "A non-lock holder tried to close the generic cache. Path: " + FileCachePath);
		}
	}
	
	public T get(Object holder) throws Exception
    {
		if(isHolder(holder)){
			return GetFileValue();
		}else{
			throw new Exception("A non-lock holder tried to read the generic cache. Path: " + FileCachePath);
		}
    }
	
	public void set(Object holder, T value) throws Exception
    {
    	if(isHolder(holder)){
			SaveFileValue(value);
		}else{
			throw new Exception("A non-lock holder tried to save the generic cache. Path: " + FileCachePath);
		}
    }
	
	//PRIVATE METHODS:
    private boolean isHolder(Object holder){
    	ResourceHolder holderObj = pathsInUse.get(FileCachePath);
		if(holderObj.Holder() == holder && holderObj.HoldingThread() == Thread.currentThread()){
			return true;
		}else{
			return false;
		}
    }
    
	private synchronized void initCache(T initialValue) throws Exception
    {
		try{
	        File dir = new File(FileCachePath.substring(0, FileCachePath.lastIndexOf('/')));
	        if (dir != null && !dir.exists())
	        {
	        	dir.mkdirs();
	        }
	        if(!dir.exists())
	        	throw new Exception("Getting directory failed. Path: " + FileCachePath);
	        
	        File file = new File(FileCachePath);
	        if(file == null || !file.exists())
		        SaveFileValue(initialValue);
		}catch(Exception ex)
		{
			throw ex;
		}
    }
	
	private T GetFileValue() throws Exception //Use generics and Xml serilization in the future save any value
    {
        boolean fail = true;
        long startTry = System.currentTimeMillis();
        while (fail) {
            fail = false;
            try
            {
            	File file = new File(FileCachePath);
            	FileInputStream fis = new FileInputStream(file);
				ObjectInputStream ois = new ObjectInputStream(fis);
            	Object readObj = ois.readObject();
            	ois.close();
            	fis.close();
            	
            	return SyntacticSugar.<T>castAs(readObj);
            }
            catch (Exception ex)
            {
            	if (startTry + 10000 < System.currentTimeMillis())
                {
                    throw new Exception("GenericFileCache failed to get file cache value for 10 seconds. Path: " + FileCachePath, ex);
                }
                fail = true;
                Thread.yield();
            }
        }
        return null;
    }
	private void SaveFileValue(T value) throws Exception //Use generics and Xml serilization in the future save any value
    {
        boolean fail = true;
        long startTry = System.currentTimeMillis();
        
        while (fail){
        	fail = false;
        	try{
    	        File file = new File(FileCachePath);
    	        FileOutputStream fos = new FileOutputStream(file);
    	        ObjectOutputStream objectOutputStream = new ObjectOutputStream(fos);
    	        objectOutputStream.writeObject(value);
    	        objectOutputStream.close();
    	        fos.close();
        	}catch(Exception ex){
        		if (startTry + 10000 < System.currentTimeMillis())
                {
                    throw new Exception("GenericFileCache failed to save file cache value for 10 seconds. Path: " + FileCachePath, ex);
                }
                fail = true;
                Thread.yield();
        	}
        }
    }
}