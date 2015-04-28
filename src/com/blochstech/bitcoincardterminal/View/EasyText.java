package com.blochstech.bitcoincardterminal.View;

import java.util.HashMap;

import com.blochstech.bitcoincardterminal.R;
import com.blochstech.bitcoincardterminal.Utils.Event;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

//Internal, view only.
class EasyText {
	private TextWatcher textListener = new TextListener();
	//private boolean watcherSet = false;
	
	private EditText myView;
	
	private boolean ignoreChangeEvent = false;
	private static HashMap<String, Boolean> ignoreFlags = new HashMap<String, Boolean>();
	
	Event<Integer> updateEvent = new Event<Integer>(this);
	
	EasyText(EditText v){
		myView = v;
		myView.addTextChangedListener(textListener);
    	setValid(true);
	}
	
	void ignoreTextChanges(boolean value){
		ignoreFlags.put("" + myView.getId(), value);
	}
	
	void setText(String value){
		ignoreChangeEvent = true;
		/*if(watcherSet){
			myView.removeTextChangedListener(textListener);
			watcherSet = false;
		}*/
		int cursor = Math.max(0, Math.min(value.length(), myView.getSelectionStart()));
		myView.setText(value);
		myView.setSelection(cursor);
		/*myView.addTextChangedListener(textListener);
    	watcherSet = true;*/
	}
	
	String getText(){
		return myView.getText().toString();
	}
	
	int getId(){
		return myView.getId();
	}
	
	void setValid(boolean valid){
		if(valid){
			myView.setBackgroundColor(myView.getResources().getColor(R.color.ThinBlue));
		}else{
			myView.setBackgroundColor(myView.getResources().getColor(R.color.ThinRed));	
		}
	}
	
	private void fire(){
		updateEvent.fire(this, getId());
	}
    private class TextListener implements TextWatcher { //Switch on new text

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count){}

		@Override
		public void afterTextChanged(Editable s) {
			boolean ignoreAll = ignoreFlags.containsKey(""+myView.getId()) && ignoreFlags.get("" + myView.getId());
			if(!ignoreChangeEvent && !ignoreAll){
				fire();
			}else{
				ignoreChangeEvent = false;
			}
		}
    }
}
