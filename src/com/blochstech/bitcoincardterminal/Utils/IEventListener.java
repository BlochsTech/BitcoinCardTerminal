package com.blochstech.bitcoincardterminal.Utils;

public interface IEventListener<T> {
	public void onEvent(T event);
}
