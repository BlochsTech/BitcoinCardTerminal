package com.blochstech.bitcoincardterminal.Model.Communication;

public class BlockResponse {
	//Block header:
	public String Version;
	public String PreviousBlock;
	public String MerkleRoot;
	public String Time;
	public String Bits;
	public String Nonce;
	
	//Merkle branch:
	public MerkleBranchElement[] MerkleBranch;
	
	//Connection:
	public boolean HasConnection;
	
	//Confirmed:
	public boolean TXIsInABlock;
	
	public BlockResponse(boolean HasConnection, boolean TXIsInABlock){
		this.HasConnection = HasConnection;
		this.TXIsInABlock = TXIsInABlock;
	}
	
	public BlockResponse(){
		HasConnection = true;
		TXIsInABlock = true;
	}
}
