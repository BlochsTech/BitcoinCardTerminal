package com.blochstech.bitcoincardterminal.Utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

import android.util.Log;

//Do not use on main thread, use networking worker thread.
public class TcpUtil {
	private static long lastCallMillis = 0;
	public static SimpleWebResponse SendReceiveTcpMessage(Socket socket, String message) {
		try{
			InputStream inStream = socket.getInputStream();
			PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())),
			true);
			
			out.println(message);
			
			BufferedReader input = new BufferedReader(new InputStreamReader(inStream));
			
			String tcpResult = "";

			/*while(inStream.available() <= 0){
				Thread.sleep(50);
				if(System.currentTimeMillis() - startTime > 500)
					return new SimpleWebResponse(null, false);
			}*/
			
			if(lastCallMillis >= System.currentTimeMillis() - 100)
				Thread.sleep(100);
			
			lastCallMillis = System.currentTimeMillis();
			
			//while ((line = input.readLine()) != null) {
			tcpResult = input.readLine();
			
			return new SimpleWebResponse(tcpResult, true);
		}catch(Exception ex){
			if(Tags.DEBUG){
				Log.e(Tags.APP_TAG, "Failed to send TCP message "+message+".");
				
				if(ex != null)
					ex.printStackTrace(); //TODO: Make helper function for getting stacktrace (print to stream, convert to string - done)
			}
		}
		
		return new SimpleWebResponse(null, false);
	}
}
