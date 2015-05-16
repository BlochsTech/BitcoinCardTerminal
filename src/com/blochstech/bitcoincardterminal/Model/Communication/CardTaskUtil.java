package com.blochstech.bitcoincardterminal.Model.Communication;

import java.util.ArrayList;

import com.blochstech.bitcoincardterminal.Utils.ByteConversionUtil;

//Static conversion help methods here between soft logic calls and hardcore byte arrays.
public class CardTaskUtil {
	//CLA INS P1 P2 Lc IDATA Le
	
	private final static int CLA = 128;
	
	private static void checkResponse(int[] response) throws Exception{
		if(response == null)
			throw new Exception("Null response.");
		if(response.length < 2)
			throw new Exception("Invalid response length, should always be 2 or more.");
		if(response[response.length-2] != 144 || response[response.length-1] != 0)
			throw new Exception("Card returned error codes (SW1SW2 = "
					+ response[response.length-2] + " " + response[response.length-1] +").");
	}
	private static void checkResponse(int[] response, int expectedLength) throws Exception{
		checkResponse(response);
		if(response.length != expectedLength)
			throw new Exception("Unexpected response length.");
	}
	
	//This command should really really fail. -> It DOES! Yay. (with invalid state error)
	public static byte[] getReadEepromTask(){
		int[] ubytes = new int[]{192, 8 , 0, 0, 255};
		
		return ByteConversionUtil.fromUnsigned(ubytes);
	}
	
	public static byte[] getNetworkTask(){
		int[] ubytes = new int[]{CLA, 0, 0, 0, 2, 0, 0, 2}; //Equals x80 x00 x00 x00 x02 x00 x00 x02
		
		return ByteConversionUtil.fromUnsigned(ubytes);
	}
	public static int getNetworkResponse(byte[] bytes) throws Exception{
		int[] response = ByteConversionUtil.toUnsigned(bytes);
		checkResponse(response, 4);
		
		int result = response[0] * 256 + response[1];
		
		return result;
	}
	
	public static byte[] getProtocolTask(){
		int[] ubytes = new int[]{CLA, 1, 0, 0, 2, 0, 0, 2};
		
		return ByteConversionUtil.fromUnsigned(ubytes);
	}
	public static int getProtocolResponse(byte[] bytes) throws Exception{
		int[] response = ByteConversionUtil.toUnsigned(bytes);
		checkResponse(response, 4);
		
		int result = response[0] * 256 + response[1];
		
		return result;
	}
	
	public static byte[] getAddressesTask() {
		int[] ubytes = new int[]{CLA, 2, 0, 0, 0};
		
		return ByteConversionUtil.fromUnsigned(ubytes);
	}
	public static ArrayList<String> getAddressesResponse(byte[] bytes) throws Exception
	{
		int[] response = ByteConversionUtil.toUnsigned(bytes);
		checkResponse(response);
		
		byte[] nBytes;
		nBytes = new byte[bytes.length-2];
		
		for(int i = 0; i < bytes.length-2; i++){
			nBytes[i] = bytes[i];
		}
		
		String addressString = new String(nBytes, "UTF-8");
		ArrayList<String> list = new ArrayList<String>();
		
		if(addressString != null){
			while(addressString.length() > 0){
				if(addressString.indexOf(":") != -1){
					list.add(addressString.substring(0, addressString.indexOf(":")));
					addressString = addressString.substring(addressString.indexOf(":")+1);
				}else{
					list.add(addressString);
					addressString = "";
				}
			}
		}
		
		return list;
	}
	
	public static byte[] getMaxAmountTask() {
		int[] ubytes = new int[9];
		
		for(int i = 0; i < 9; i++){
			if(i==0){
				ubytes[i] = CLA; //CLA
			}else if(i==1){
				ubytes[i] = 10; //INS
			}else if(i < 4){
				ubytes[i] = 0; //P1 and P2
			}else if (i == 4 || i == 8){
				ubytes[i] = 3; //Expected in- and out-data length.
			}else{
				ubytes[i] = 0;
			}
		};
		
		return ByteConversionUtil.fromUnsigned(ubytes);
	}
	public static Double getMaxAmountResponse(byte[] bytes) throws Exception {
		int[] response = ByteConversionUtil.toUnsigned(bytes);
		checkResponse(response);
			
		return (response[0]*256+response[1])*Math.pow(10, response[2])/100000000.0;
	}
	
	public static byte[] getSourcesTask(int sourceIndex) {
		int[] ubytes = new int[54];
		
		for(int i = 0; i < 54; i++){
			if(i==0){
				ubytes[i] = CLA; //CLA
			}else if(i==1){
				ubytes[i] = 5; //INS
			}else if(i < 4){
				ubytes[i] = 0; //P1 and P2
			}else if (i == 4 || i == 53){
				ubytes[i] = 48; //Expected in- and out-data length. (Er: 2 Next: 1 Out: 4 Hash: 32 Value: 8)
			}else if(i == 5 + 2){
				ubytes[i] = sourceIndex;
			}else{
				ubytes[i] = 0;
			}
		};
		
		return ByteConversionUtil.fromUnsigned(ubytes);
	}
	public static SourceCardResponse getSourcesResponse(byte[] bytes) throws Exception {
		int[] response = ByteConversionUtil.toUnsigned(bytes);
		checkResponse(response);
		if(response[0] != 0 || response[1] != 0)
			throw new Exception("BOBC Error: " + (response[0]*256+response[1]));
		
		SourceCardResponse res = new SourceCardResponse();
		
		int[] nUBytes = ByteConversionUtil.subBytes(response, 2, 1);
		if(nUBytes != null)
			res.NextIndex = nUBytes[0];
		
		byte[] nBytes = ByteConversionUtil.subBytes(bytes, 3, 4);
		if(nBytes != null)
			res.Source.OutIndex = ByteConversionUtil.byteArrayToHexString(nBytes);
		
		nBytes = ByteConversionUtil.subBytes(bytes, 7, 32);
		if(nBytes != null)
			res.Source.TXHash = ByteConversionUtil.byteArrayToHexString(nBytes);
		
		nUBytes = ByteConversionUtil.subBytes(response, 39, 8);
		if(nBytes != null)
			res.Source.Satoshi = ByteConversionUtil.UByteArrayToBENumber(nUBytes);
		
		res.Source.Verified = response[47] == 1;
		
		if(res.Source.TXHash.equals("0000000000000000000000000000000000000000000000000000000000000000")
			&& res.Source.OutIndex.equals("00000000"))
			res.Source = null;
		
		return res;
	}
	
	//Command &H80 &H03 RequestPayment(ErrorCode%, RequiresPIN@, AmountMantissa%, AmountExp@, FeeMantissa%, FeeExp@,
	//TerminalAmountMantissa%, TerminalAmountExp@, Decimals@, ReceiverAddress as String*20, ReceiverAddressType@,
	//TerminalReceiverAddress as String*20, TerminalReceiverAddressType@, returnvalue as String*8)
	public static byte[] getRequestPaymentTask(Double amount, Double terminalAmount, Double fee, String address, String terminalAddress) throws Exception {
		int[] ubytes = new int[69]; //63+6
		
		int[] byteAmount = TypeConverter.toCardFloatType(amount);
		int[] byteTerminalAmount = TypeConverter.toCardFloatType(terminalAmount);
		int[] byteFeeAmount = TypeConverter.toCardFloatType(fee);
		int[] byteAddress = TypeConverter.fromBase58CheckToAddressBytes(address); //Length 21 because of type byte.
		int[] byteTerminalAddress = TypeConverter.fromBase58CheckToAddressBytes(terminalAddress); //Length 21 because of type byte.
		
		for(int i = 0; i < 69; i++){
			if(i==0){
				ubytes[i] = CLA; //CLA
			}else if(i==1){
				ubytes[i] = 3; //INS
			}else if(i < 4){
				ubytes[i] = 0; //P1 and P2
			}else if (i == 4 || i == 68){
				ubytes[i] = 63; //Expected in- and out-data length.
			}else if (i >= 8 && i <= 10){
				ubytes[i] = byteAmount[i-8];
			}else if (i >= 14 && i <= 16){
				ubytes[i] = byteTerminalAmount[i-14];
			}else if (i >= 11 && i <= 13){
				ubytes[i] = byteFeeAmount[i-11];
			}else if(i == 17){
				ubytes[i] = 8;
			}else if (i >= 18 && i <= 38){
				ubytes[i] = byteAddress[i-18];
			}else if (i >= 39 && i <= 59){
				ubytes[i] = byteTerminalAddress[i-39];
			}
			//8 bytes return value left as 0's (60-67). 68 set to length.
		};
		
		return ByteConversionUtil.fromUnsigned(ubytes);
	}
	public static ChargeRequestCardResponse getRequestPaymentResponse(byte[] bytes) throws Exception {
		int[] response = ByteConversionUtil.toUnsigned(bytes);
		checkResponse(response);
		if(response[0] != 0 || response[1] != 0)
			throw new Exception("BOBC Error: " + (response[0]*256+response[1]));
		
		byte[] nBytes;
		nBytes = new byte[8];
		
		int j = 0;
		for(int i = bytes.length-10; i < bytes.length-2; i++){
			nBytes[j] = bytes[i];
			j++;
		}
		
		ChargeRequestCardResponse resp = new ChargeRequestCardResponse();
		resp.VignereCode = new String(nBytes, "UTF-8");
		resp.RequiresPIN = response[2] == 1;
		
		return resp;
	}

	public static byte[] getDebugTask() {
		int[] ubytes = new int[]{CLA, 255, 0, 0, 0};
		
		return ByteConversionUtil.fromUnsigned(ubytes);
	}
	public static String getDebugResponse(byte[] bytes) throws Exception{
		int[] response = ByteConversionUtil.toUnsigned(bytes);
		checkResponse(response);
		
		return ByteConversionUtil.bytesToString(bytes, 0, bytes.length-2);
	}
	
	public static byte[] getTimeUnlockTask() {
		int[] ubytes = new int[]{CLA, 9, 0, 0, 2, 0, 0, 2};
		
		return ByteConversionUtil.fromUnsigned(ubytes);
	}
	public static int getTimeUnlockResponse(byte[] bytes) throws Exception{
		int[] response = ByteConversionUtil.toUnsigned(bytes);
		checkResponse(response, 4);
		
		return response[0]*256 + response[1];
	}
	
	public static byte[] getWaitingChargeTask() {
		int[] ubytes = new int[70];
		
		for(int i = 0; i < 70; i++){
			if(i==0){
				ubytes[i] = CLA; //CLA
			}else if(i==1){
				ubytes[i] = 11; //INS
			}else if(i < 4){
				ubytes[i] = 0; //P1 and P2
			}else if (i == 4 || i == 69){
				ubytes[i] = 64; //Expected in- and out-data length.
			}else{
				ubytes[i] = 0;
			}
		};
		
		return ByteConversionUtil.fromUnsigned(ubytes);
	}
	public static WaitingChargeCardResponse getWaitingChargeResponse(byte[] bytes) throws Exception{
		int[] response = ByteConversionUtil.toUnsigned(bytes);
		checkResponse(response);

		WaitingChargeCardResponse resp = new WaitingChargeCardResponse();
		resp.WaitingAmount = ((response[0] * 256 + response[1]) * Math.pow(10, response[2])) / 100000000;
		resp.WaitingFee = ((response[3] * 256 + response[4]) * Math.pow(10, response[5])) / 100000000;
		resp.WaitingTerminalAmount = ((response[6] * 256 + response[7]) * Math.pow(10, response[8])) / 100000000;
		resp.WaitingCardFee = ((response[51] * 256 + response[52]) * Math.pow(10, response[53])) / 100000000;
		resp.WaitingRequiresPin = response[54] == 1;
		resp.WaitingVignereCode = ByteConversionUtil.bytesToString(bytes, 55, 8);
		resp.WaitingIsResetRequest = response[63] == 1;
				
		resp.WaitingAddress = new int[21];
		resp.WaitingTerminalAddress = new int[21];
		for(int i = 0; i < 21; i++)
		{
			resp.WaitingAddress[i] = response[9+i];
			resp.WaitingTerminalAddress[i] = response[30+i];
		}
		return resp;
	}
	
	public static byte[] getMaxSourcesTask() {
		int[] ubytes = new int[8];
		
		for(int i = 0; i < 8; i++){
			if(i==0){
				ubytes[i] = CLA; //CLA
			}else if(i==1){
				ubytes[i] = 15; //INS
			}else if(i < 4){
				ubytes[i] = 0; //P1 and P2
			}else if (i == 4 || i == 7){
				ubytes[i] = 2; //Expected in- and out-data length.
			}else{
				ubytes[i] = 0;
			}
		};
		
		return ByteConversionUtil.fromUnsigned(ubytes);
	}
	public static int getMaxSourcesResponse(byte[] bytes) throws Exception {
		int[] response = ByteConversionUtil.toUnsigned(bytes);
		checkResponse(response);
			
		return response[0]*256+response[1];
	}
	
	public static byte[] getSendTXCommand(byte[] txRaw, byte packetIndex) {
		int[] ubytes = new int[256];
		
		for(int i = 0; i < 256; i++){
			if(i==0){
				ubytes[i] = CLA; //CLA
			}else if(i==1){
				ubytes[i] = 6; //INS
			}else if(i < 4){
				ubytes[i] = 0; //P1 and P2
			}else if (i == 4 || i == 255){
				ubytes[i] = 250; //Expected in- and out-data length. (6 goes to command bytes)
			}else if(i == 7){ //Accepted byte:
				ubytes[i] = packetIndex > 0 ? 1 : 0; //Clear card TX stream if first packet ie. "don't 'accept' existing in card".
			}else if(i == 8){
				if(txRaw.length <= packetIndex*246+246){
					ubytes[8] = 246-(packetIndex*246+246-txRaw.length); //EndsTXStream.
				}
			}else if(i > 8 && i < 255){
				//(6 goes to command bytes and 4 to other parameters)
				if(txRaw.length > i-9+packetIndex*246 && -1 < i-9+packetIndex*246){
					ubytes[i] = ByteConversionUtil.toUnsigned(txRaw[i-9+packetIndex*246]);
				}else{
					ubytes[i] = 0;
				}
			}else{ //ErrorCodes
				ubytes[i] = 0;
			}
		};
		
		return ByteConversionUtil.fromUnsigned(ubytes);
	}
	public static boolean getSendTXResponse(byte[] bytes) throws Exception{ //Run at each response to check errorcode.
		int[] response = ByteConversionUtil.toUnsigned(bytes);
		checkResponse(response);
		
		if(response[0] != 0 || response[1] != 0){
			throw new Exception("BOBC Error: " + (response[0]*256+response[1]));
		}else{
			return response[2] == 1;
		}
	}
	
	public static byte[] getHeadersTask(byte[] blockHeader, String txHash) {
		int[] ubytes = new int[89+32];
		int[] blockBytes = ByteConversionUtil.toUnsigned(blockHeader);
		
		int[] hashBytes = ByteConversionUtil.toUnsigned(ByteConversionUtil.hexStringToByteArray(txHash));
		
		for(int i = 0; i < 89+32; i++){
			if(i==0){
				ubytes[i] = CLA; //CLA
			}else if(i==1){
				ubytes[i] = 7; //INS
			}else if(i < 4){
				ubytes[i] = 0; //P1 and P2
			}else if (i == 4 || i == 88+32){
				ubytes[i] = 83+32; //Expected in- and out-data length.
			}else if(i >= 5 + 3 && i < 40){
				ubytes[i] = hashBytes[i-8];
			}else if(i >= 5 + 3 + 32 && i < 88+32){
				ubytes[i] = blockBytes[i-5-3-32];
			}else{
				ubytes[i] = 0;
			}
		};
		
		return ByteConversionUtil.fromUnsigned(ubytes);
	}
	public static boolean getHeadersResponse(byte[] bytes) throws Exception {
		int[] response = ByteConversionUtil.toUnsigned(bytes);
		checkResponse(response);
		if(response[0] != 0 || response[1] != 0)
			throw new Exception("BOBC Error: " + (response[0]*256+response[1]));
		
		return response[2] == 1;
	}
	
	public static byte[] getMerkleTask(MerkleBranchElement element){
		int[] ubytes = new int[40];
		int[] hashBytes = ByteConversionUtil.toUnsigned(ByteConversionUtil.hexStringToByteArray(element.hash));
		
		for(int i = 0; i < 40; i++){
			if(i==0){
				ubytes[i] = CLA; //CLA
			}else if(i==1){
				ubytes[i] = 8; //INS
			}else if(i < 4){
				ubytes[i] = 0; //P1 and P2
			}else if (i == 4 || i == 39){
				ubytes[i] = 34; //Expected in- and out-data length.
			}else if(i == 5+1){
				ubytes[i] = element.rightNode ? 1 : 0;
			}else if(i >= 5 + 2 && i < 39){
				ubytes[i] = hashBytes[i-5-2];
			}else{
				ubytes[i] = 0;
			}
		};
		
		return ByteConversionUtil.fromUnsigned(ubytes);
	}
	public static boolean getMerkleResponse(byte[] bytes) throws Exception{
		int[] response = ByteConversionUtil.toUnsigned(bytes);
		checkResponse(response);
		
		return response[0] == 1;
	}

	public static byte[] getPinTask(int pin){ //Len vars 250 : (ErrorCode%, Pin%, EndOfTXStream@, TXBytes as String*245)
		int[] ubytes = new int[256];
		
		for(int i = 0; i < 256; i++){
			if(i==0){
				ubytes[i] = CLA; //CLA
			}else if(i==1){
				ubytes[i] = 4; //INS
			}else if(i < 4){
				ubytes[i] = 0; //P1 and P2
			}else if (i == 4 || i == 255){
				ubytes[i] = 250; //Expected in- and out-data length.
			}else if(i == 5+2){
				ubytes[i] = pin / 256;
			}else if(i == 5+3){
				ubytes[i] = pin % 256;
			}else{
				ubytes[i] = 0;
			}
		};
		
		return ByteConversionUtil.fromUnsigned(ubytes);
	}
	public static PinCardResponse getPinResponse(byte[] bytes) throws Exception{
		int[] response = ByteConversionUtil.toUnsigned(bytes);
		checkResponse(response);
		if(response[0] != 0 || response[1] != 0)
			throw new Exception("BOBC Error: " + (response[0]*256+response[1]));
		
		PinCardResponse resp = new PinCardResponse();
		
		//resp.ClaimIsDEREncoded = response[4] == 1; -> No longer used/in method.
		int returnedPinValue = (int) (response[2] >= 128 ? -Math.pow(2, 15) : 0) + (response[2] % 128)*256 + response[3]; //Highest bit is negative. highest bit is 2^15.
		resp.PinAccepted = returnedPinValue != -1;
		int packetLen = response[4] == 0 ? 245 : response[4];
		resp.LastPacket = response[4] != 0;
		resp.TxDataPacket = new byte[packetLen];
		for(int i = 5; i < packetLen + 5; i++){
			resp.TxDataPacket[i-5] = bytes[i];
		}
		
		return resp;
	}
	
	public static byte[] getResetPinTask(int PUK, int newPIN){
		int[] ubytes = new int[14];
		
		int[] pin = ByteConversionUtil.toUnsigned(ByteConversionUtil.leIntToBytes(newPIN));
		int[] puk = ByteConversionUtil.toUnsigned(ByteConversionUtil.leIntToBytes(PUK));
		
		for(int i = 0; i < 14; i++){
			if(i==0){
				ubytes[i] = CLA; //CLA
			}else if(i==1){
				ubytes[i] = 16; //INS
			}else if(i < 4){
				ubytes[i] = 0; //P1 and P2
			}else if (i == 4 || i == 13){
				ubytes[i] = 8; //Expected in- and out-data length.
			}else if (i == 5 || i == 6){
				ubytes[i] = 0; //ErrorCode.
			}else if(i >= 7 && i <= 10){
				ubytes[i] = puk[3+7-i]; //Run backwards to get the right order.
			}else if(i >= 11 && i <= 12){
				ubytes[i] = pin[1+11-i]; //2 bytes only, run backwards for right order.
			}else{
				ubytes[i] = 0;
			}
		};
		
		return ByteConversionUtil.fromUnsigned(ubytes);
	}
	public static boolean getResetPinResponse(byte[] bytes) throws Exception{
		int[] response = ByteConversionUtil.toUnsigned(bytes);
		checkResponse(response, 10);
		if(response[0] != 0 || response[1] != 0)
			throw new Exception("BOBC Error: " + (response[0]*256+response[1]));
		
		int result = (int) ByteConversionUtil.UByteArrayToLENumber(new int[]{response[2],response[3],response[4],response[5]});
		
		return result != -1;
	}
}