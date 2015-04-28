package com.blochstech.bitcoincardterminal.View;

public enum PageTags{
	CHARGE_PAGE(0),
	HISTORY_PAGE(1),
	PIN_PAGE(2),
	SETTINGS_PAGE(3);
	
	private int value;
	private PageTags(int value) {
	  this.value = value;
	}
	public int getValue() {
      return value;
	}
	public static PageTags Convert(int value){
		switch(value){
			case 0:
				return CHARGE_PAGE;
			case 1:
				return HISTORY_PAGE;
			case 2:
				return PIN_PAGE;
			case 3:
				return SETTINGS_PAGE;
			default:
				return CHARGE_PAGE;
		}
	}
}
