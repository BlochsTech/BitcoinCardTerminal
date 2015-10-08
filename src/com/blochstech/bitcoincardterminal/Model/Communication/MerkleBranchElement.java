package com.blochstech.bitcoincardterminal.Model.Communication;

public class MerkleBranchElement {
	public String hash; //This is the one sent to the card.
	public boolean rightNode; //True if property hash is on the right side of the pair. 
	public String otherHash; //In the first round this is the txHash.
	
	public MerkleBranchElement(String hash, boolean rightNode, String otherHash){
		this.hash = hash;
		this.rightNode = rightNode;
		this.otherHash = otherHash;
	}
}
