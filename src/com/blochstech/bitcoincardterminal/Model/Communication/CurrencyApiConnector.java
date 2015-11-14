package com.blochstech.bitcoincardterminal.Model.Communication;

import java.util.LinkedList;

import org.json.JSONObject;

import com.blochstech.bitcoincardterminal.DataLayer.GenericFileCache;
import com.blochstech.bitcoincardterminal.Interfaces.Currency;
import com.blochstech.bitcoincardterminal.Utils.ByteConversionUtil;
import com.blochstech.bitcoincardterminal.Utils.RegexUtil;
import com.blochstech.bitcoincardterminal.Utils.SimpleWebResponse;
import com.blochstech.bitcoincardterminal.Utils.Tags;
import com.blochstech.bitcoincardterminal.Utils.WebUtil;

import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.util.Log;

public class CurrencyApiConnector {
	//Client/worker:
	private final static String userAgent = "BOBC-0 Terminal/0.0/Android";
	private final static String contentType = "application/json";
	private static AndroidHttpClient client;
	
	private static long lastSynched = 0;
	//(In dollars)
	private static double microbtcValue = 0.00024;
	private static double appleValue = 0.40;
	private static double yuanValue = 0.16;
	private static double euroValue = 1.12;
	private static double btcValue = 240;
	
	private static CurrencyApiConnector instance = null;
	private static boolean isSynching = false;

	private static GenericFileCache<LinkedList<Double>> ratesCache;
	private final static String ratesCachePath = "/BitcoinTerminal/Currencies/ratesCache.bin";
	
	private CurrencyApiTask getWorker(){
		return new CurrencyApiTask();
	}
	
	//START WORKER - <StartParamsType, ProgressResultType, PostExecuteType> (doInBackground returns to onPostExecute)
	private class CurrencyApiTask extends AsyncTask<Void, Double, Void> { //TODO: Init synch on app start-up.
		
		/*private String WebCall(String url) throws IOException{
			String netResult = "", line;
			
			HttpGet getRequest = new HttpGet(url);
			getRequest.addHeader("accept", "application/json");
			
			HttpResponse response = client.execute(getRequest);
			
			 
			if (response.getStatusLine().getStatusCode() != 200) {
				if(Tags.DEBUG)
					Log.e(Tags.APP_TAG, "Failed : HTTP error code : "
				   + response.getStatusLine().getStatusCode());
				throw new IOException("Web call failed. HTTP status code not 200.");
			}else{
				BufferedReader br = new BufferedReader(
		                         new InputStreamReader((response.getEntity().getContent())));
		 
				while ((line = br.readLine()) != null) {
					netResult = netResult + line;
				}
			}
			
			return netResult;
		}*/
		
		protected Void doInBackground(Void... startParams) {
		    //Do network calls here:
			
			Double[] result = new Double[6];
			
			if(client == null){
				client = AndroidHttpClient.newInstance(userAgent);
			}
			
			SimpleWebResponse resp;
			
			//BTC rate:
			boolean useBackup = false;
			try{
				//https://www.bitstamp.net/api/ticker_hour/
				resp = WebUtil.SimpleHttpGet(client, "http://api.coindesk.com/v1/bpi/currentprice.json", contentType, "CurrencyApiGet_BTC_Rate");
				if(resp.IsConnected && resp.Response != null){
					JSONObject json = new JSONObject(resp.Response);
					JSONObject usdObject = json.getJSONObject("bpi").getJSONObject("USD");
					result[5] = usdObject.getDouble("rate");
				}
			}catch(Exception ex){
				useBackup = true;
				if(Tags.DEBUG)
					Log.e(Tags.APP_TAG, "Failed to get BTC/USD rate. Ex: " + ex.toString());
			}
			
			if(useBackup){
				try{
					resp = WebUtil.SimpleHttpGet(client, "https://www.bitstamp.net/api/ticker_hour/", contentType, "CurrencyApiGet_BTC_Rate");
					if(resp.IsConnected && resp.Response != null){
						JSONObject json = new JSONObject(resp.Response);
						result[5] = json.getDouble("last");
					}
				}catch(Exception ex){
					if(Tags.DEBUG)
						Log.e(Tags.APP_TAG, "Failed to get backup BTC/USD rate. Ex: " + ex.toString());
				}
			}

			//MicroBtc rate:
			if(result[5] != null)
				result[0] = result[5]/1000000;
			
			//Yuan+Euro rate:
			try{
				resp = WebUtil.SimpleHttpGet(client, "http://api.fixer.io/latest", contentType, "CurrencyApiGet_RMB_EUR_Rate");
				if(resp.IsConnected && resp.Response != null){
					JSONObject json = new JSONObject(resp.Response);
					JSONObject ratesObject = json.getJSONObject("rates");
					
					result[4] = ratesObject.getDouble("USD");
					
					result[2] = result[4] / ratesObject.getDouble("CNY");
				}
			}catch(Exception ex){
				if(Tags.DEBUG)
					Log.e(Tags.APP_TAG, "Failed to get YUAN and EUR/USD rate. Ex: " + ex.toString());
			}
			
			//Apples rate:
			try{
				//1 kg unorganic apples:
				Double[] rawAppleRates = new Double[6];
				String[] urls = new String[]{"http://www.numbeo.com/food-prices/city_result.jsp?country=Denmark&city=Copenhagen&displayCurrency=USD",
						"http://www.numbeo.com/food-prices/city_result.jsp?country=United+States&city=New+York%2C+NY",
						"http://www.numbeo.com/food-prices/city_result.jsp?country=China&city=Shanghai&displayCurrency=USD",
						"http://www.numbeo.com/food-prices/city_result.jsp?country=Nigeria&city=Lagos&displayCurrency=USD",
						"http://www.numbeo.com/food-prices/city_result.jsp?country=Brazil&city=Sao+Paulo&displayCurrency=USD",
						"http://www.numbeo.com/food-prices/city_result.jsp?country=Australia&city=Sydney&displayCurrency=USD"};
				
				String webResult, match;
				
				for(int j = 0 ; j < urls.length; j++){
					webResult = "";
					resp = WebUtil.SimpleHttpGet(client, urls[j], contentType, "CurrencyApiGet_Apple_Rate");
					if(resp.IsConnected && resp.Response != null)
						webResult = resp.Response;
			
					match = RegexUtil.getMatch(webResult, "Apples \\(1kg\\)[^0-9]*[0-9\\.]+");
					if(match != null && match.lastIndexOf(" ") != -1){
						rawAppleRates[j] = Double.parseDouble(match.substring(match.lastIndexOf(" "), match.length()));
					}
				}
				
				Double total = 0.0;
				int divisor = 0;
				for(int i = 0; i < rawAppleRates.length; i++){
					if(rawAppleRates[i] != null){
						total += rawAppleRates[i];
						divisor++;
					}
				}
				
				result[1] = total/divisor * 0.140 * 1.0; //1 kg to 140g/1 apple. The organic prices were the same as numbeo in my area, to be updated...
			}catch(Exception ex){
				if(Tags.DEBUG)
					Log.e(Tags.APP_TAG, "Failed to get YUAN and EUR/USD rate. Ex: " + ex.toString());
			}
			
			
			publishProgress(result);
			
		    return null;
		}
		
		protected void onProgressUpdate(Double... endResult) { //("..." means array) Code runs on main thread:
			//Persist:
			Object holder = new Object();
			try{
				if(endResult.length > 5){
					ratesCache.Open(holder);
					LinkedList<Double> newCache = ratesCache.get(holder);
					for(int i = 0; i < newCache.size(); i++){
						newCache.set(i, ByteConversionUtil.safeAssign(endResult[i], newCache.get(i), 0));
					}
					ratesCache.set(holder, newCache); //Only update queue, with head item now removed, if tx sent.
					ratesCache.Close(holder);
				}
			}catch(Exception ex){
				ratesCache.Close(holder);
				if(Tags.DEBUG)
					Log.e(Tags.APP_TAG, "Could not persist currency rates. Exception: " + ex.toString());
			}
			
			//Set currency values:
			if(endResult[0] != null && endResult[0] != 0)
				microbtcValue = endResult[0];

			if(endResult[1] != null && endResult[1] != 0)
				appleValue = endResult[1];

			if(endResult[2] != null && endResult[2] != 0)
				yuanValue = endResult[2];

			if(endResult[4] != null && endResult[4] != 0)
				euroValue = endResult[4];
			
			if(endResult[5] != null && endResult[5] != 0)
				btcValue = endResult[5];
		}
	
		protected void onPostExecute(Void end) { //Code runs on main thread:
			isSynching = false;
		}
	}
	//END WORKER
	
	public synchronized static void SynchPrices(String caller){
		
		if(instance == null){
			instance = new CurrencyApiConnector();
			
			try {
				if(ratesCache == null)
					ratesCache = new GenericFileCache<LinkedList<Double>>(ratesCachePath, new LinkedList<Double>());	
			} catch (Exception e) {
				ratesCache = null;
				if(Tags.DEBUG)
					Log.e(Tags.APP_TAG, "CurrencyApiConnector could not instantiate cache, transactions will "
						+ "be lost if they cannot immediately be relayed. " + e.getMessage());
			}
		}
		
		if(!isSynching)
		{
			Object holder = new Object();
			try{
				ratesCache.Open(holder);
				LinkedList<Double> ratesList = ratesCache.get(holder);
	
				if(ratesList.size() > 5){
					microbtcValue = ByteConversionUtil.safeAssign(ratesList.get(0), microbtcValue, 0);
					appleValue = ByteConversionUtil.safeAssign(ratesList.get(1), appleValue, 0);
					yuanValue = ByteConversionUtil.safeAssign(ratesList.get(2), yuanValue, 0);
					euroValue = ByteConversionUtil.safeAssign(ratesList.get(4), euroValue, 0);
					btcValue = ByteConversionUtil.safeAssign(ratesList.get(5), btcValue, 0);
				}
				ratesCache.Close(holder);
			}catch (Exception ex){
				ratesCache.Close(holder);
				if(Tags.DEBUG)
					Log.e(Tags.APP_TAG, "Failed to get currency rates from disk. Hardcoded or Api values will be used. Exception: " + ex.toString());
			}
			
			isSynching = true;
			instance.getWorker().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (new Void[0]));
		}
	}
	
	public static Double DollarValue(Currency currency) {
		if(lastSynched < System.currentTimeMillis() - 1000*60*60){
			lastSynched = System.currentTimeMillis();
			SynchPrices("GetValueMethod");
		}
		
		switch(currency){
			case MicroBitcoins:
				return microbtcValue;
			case Apples:
				return appleValue;
			case Yuans:
				return yuanValue;
			case Dollars:
				return 1.0;
			case Euros:
				return euroValue;
			case Bitcoins:
				return btcValue;
			default:
				return appleValue;
		}
	}
}
