package com.blochstech.bitcoincardterminal.Model.Communication;

public enum NetworkCallbackMethods {
		TXData(0), //TXData ready.
		NoConnection(1), //HTML error.
		TXSendResult(2);
		
		private int value;
		public int Value(){
			return value;
		}
		private NetworkCallbackMethods(int value) {
		  this.value = value;
		}
		public static NetworkCallbackMethods Convert(int value){
			return NetworkCallbackMethods.values()[value];
		}	
}