package com.blochstech.bitcoincardterminal.Model.Communication;

public class MerkleBranchElement {
	public String hash;
	public boolean rightNode;
	public String otherHash;
	
	public MerkleBranchElement(String hash, boolean rightNode, String otherHash){
		this.hash = hash;
		this.rightNode = rightNode;
		this.otherHash = otherHash;
	}
}
