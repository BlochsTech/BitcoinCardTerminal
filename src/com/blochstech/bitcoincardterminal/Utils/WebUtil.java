package com.blochstech.bitcoincardterminal.Utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;

import android.net.http.AndroidHttpClient;
import android.util.Log;

//DO NOT USE OUTSIDE WORKER THREAD.
public class WebUtil {
	
	//DO NOT USE OUTSIDE WORKER THREAD.
	public static SimpleWebResponse SimpleHttpGet(AndroidHttpClient client, String url, String contentType, String callerDebugName) throws IOException{
		String netResult = "", line;
		
		HttpGet getRequest = new HttpGet(url);
		getRequest.addHeader("accept", contentType);
		
		HttpResponse response = client.execute(getRequest);
		
		 
		if (response.getStatusLine().getStatusCode() != 200) {
			HttpResponseLog(response, callerDebugName);
			return new SimpleWebResponse(null, false);
		}else{
			BufferedReader br = new BufferedReader(
	                         new InputStreamReader((response.getEntity().getContent())));
	 
			while ((line = br.readLine()) != null) {
				netResult = netResult + line;
			}
		}
		
		return new SimpleWebResponse(netResult, true);
	}
	
	//DO NOT USE OUTSIDE WORKER THREAD.
	public static SimpleWebResponse SimpleHttpPost(AndroidHttpClient client, String url, String body, String contentType, String callerDebugName) throws IOException {
		String netResult = "", line;
		
		HttpPost postRequest = new HttpPost(url);
		postRequest.addHeader("accept", contentType);
		postRequest.setEntity(new StringEntity(body, "UTF8"));
		
		HttpResponse response = client.execute(postRequest);
		 
		if (response.getStatusLine().getStatusCode() != 200) {
			HttpResponseLog(response, callerDebugName);
			return new SimpleWebResponse(null, false);
		}else{
			BufferedReader br = new BufferedReader(
	                         new InputStreamReader((response.getEntity().getContent())));
	 
			while ((line = br.readLine()) != null) {
				netResult = netResult + line;
			}
		}
		
		return new SimpleWebResponse(netResult, true);
	}
	
	//This method consumes response content, do not try to read it twice.
	public static String HttpResponseLog(HttpResponse response, String debugCallName){
		String netResult="", line;
		try {
			BufferedReader br;
			
			br = new BufferedReader(
			        new InputStreamReader((response.getEntity().getContent())));
			
			while ((line = br.readLine()) != null) {
				netResult = netResult + line;
			}
			
			Header[] headers = response.getAllHeaders();
			String headerVals = "";
			
			for(int i = 0; i < headers.length; i++){
				if(i > 0)
					headerVals += " ";
				headerVals += "Header["+i+"]: " + headers[i].getValue();
			}
			
			if(Tags.DEBUG)
				Log.e(Tags.APP_TAG, "HTTP call by " +debugCallName+ " failed. Response log - HTTP code: " + response.getStatusLine().getStatusCode() 
						+ ".\nContent: " + netResult + "."
						+ ".\nHeaders: " + headerVals + ".");
		} catch (IOException e) {
			if(Tags.DEBUG){
				Log.e(Tags.APP_TAG, "Failed to log HTTP response for "+debugCallName+".");
				
				if( e != null)
					e.printStackTrace();
			}
		}
		return netResult;
	}
}
