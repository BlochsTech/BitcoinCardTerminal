package com.blochstech.bitcoincardterminal.View;

import java.text.DecimalFormat;

import com.blochstech.bitcoincardterminal.R;
import com.blochstech.bitcoincardterminal.Interfaces.Currency;
import com.blochstech.bitcoincardterminal.Model.Communication.CurrencyApiConnector;
import com.blochstech.bitcoincardterminal.Utils.EventListener;
import com.blochstech.bitcoincardterminal.Utils.SyntacticSugar;
import com.blochstech.bitcoincardterminal.ViewModel.SettingsPageVM;
import com.blochstech.bitcoincardterminal.ViewModel.ViewStateManagers.MessageManager;
import com.blochstech.bitcoincardterminal.ViewModel.ViewStateManagers.NavigationManager;
import com.blochstech.bitcoincardterminal.ViewModel.ViewStateManagers.PageManager;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

//Everything in view classes will be destroyed at whim by Android. VMs and model/database should hold logic and information.
//Views defined in layout xml files.
public class SettingsPage extends Fragment {
	private View myView;
	private SettingsPageVM myVM;
	private UpdateListener updateListener = new UpdateListener();
	
	private EasyText addressBox;
	private EasyText feeBox;
	private CheckBox courtesyOK;
	private Button btcButton;
	private Button appleButton;
	private Button dollarButton;
	private Button yuanButton;
	private Button euroButton;
	private Button okButton;
	private TextView currencyText;
	private TextView feeCurrencyText;
	
	private OnClickListener btnListener = new ButtonListener();
	private TextListener textListener = new TextListener();
	
	private boolean initialized = false;
	
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
    	myView = inflater.inflate(R.layout.settingspage, container, false);
    	
    	//Setup:
    	if(PageManager.Instance().isInitialized(PageManager.ViewModelTags.SETTINGS_PAGE_VM)){
    		myVM = SyntacticSugar.<SettingsPageVM>castAs(PageManager.Instance().getVM(PageManager.ViewModelTags.SETTINGS_PAGE_VM));
    	}
    	if(myVM == null){
    		myVM = new SettingsPageVM();
    		PageManager.Instance().setVM(PageManager.ViewModelTags.SETTINGS_PAGE_VM, myVM);
    	}
    	
    	//Save views for faster access:
    	addressBox = new EasyText(SyntacticSugar.<EditText>castAs(myView.findViewById(R.id.addressText)));
    	feeBox = new EasyText(SyntacticSugar.<EditText>castAs(myView.findViewById(R.id.feeText)));
    	courtesyOK = SyntacticSugar.<CheckBox>castAs(myView.findViewById(R.id.courtesyCheckBox));
    	btcButton = SyntacticSugar.<Button>castAs(myView.findViewById(R.id.btcButton));
    	appleButton = SyntacticSugar.<Button>castAs(myView.findViewById(R.id.appleButton));
    	dollarButton = SyntacticSugar.<Button>castAs(myView.findViewById(R.id.dollarButton));
    	yuanButton = SyntacticSugar.<Button>castAs(myView.findViewById(R.id.yuanButton));
    	euroButton = SyntacticSugar.<Button>castAs(myView.findViewById(R.id.euroButton));
    	okButton = SyntacticSugar.<Button>castAs(myView.findViewById(R.id.okSettingsButton));
    	currencyText = SyntacticSugar.<TextView>castAs(myView.findViewById(R.id.currencyText));
    	feeCurrencyText = SyntacticSugar.<TextView>castAs(myView.findViewById(R.id.feeCurrency));
    	
    	//Begin:
        myVM.UpdateEvent.register(updateListener);

    	btcButton.setOnClickListener(btnListener);
    	appleButton.setOnClickListener(btnListener);
    	dollarButton.setOnClickListener(btnListener);
    	yuanButton.setOnClickListener(btnListener);
    	euroButton.setOnClickListener(btnListener);
    	okButton.setOnClickListener(btnListener);
    	courtesyOK.setOnClickListener(btnListener);
    	addressBox.updateEvent.register(textListener);
    	feeBox.updateEvent.register(textListener);
    	
    	initialized = true;
    	update();
        
        return myView;
    }
    
    @Override
    public void onPause(){ //Fragment may be destroyed after this.
    	if(myVM != null && myVM.UpdateEvent != null)
    		myVM.UpdateEvent.unregister(updateListener);
    	if(feeBox != null)
    		feeBox.ignoreTextChanges(true);
    	if(addressBox != null)
    		addressBox.ignoreTextChanges(true);
    	super.onPause();
    }
    
    @Override
    public void onResume(){
    	super.onResume();
    	if(myVM != null && myVM.UpdateEvent != null)
    		myVM.UpdateEvent.register(updateListener);
    	if(feeBox != null)
    		feeBox.ignoreTextChanges(false);
    	if(addressBox != null)
    		addressBox.ignoreTextChanges(false);
    	update();
    }
    
    private void update(){
    	try{
    		if(initialized){
	    		addressBox.setText(myVM.Address());
	        	addressBox.setValid(myVM.IsAddressValid());
	        	
	        	feeBox.setText(String.format("%.2f",myVM.Fee())); //Means precision = 7 and floating point to decimal conversion.
	        	
				selectButton(myVM.ChosenCurrency());
				
				DecimalFormat df = new DecimalFormat("0.#######");
				
				currencyText.setText("('"
						+myVM.ChosenCurrency().Description()
						+"' --> 1"
						+myVM.ChosenCurrency().Symbol()+" ~ "
						+df.format(CurrencyApiConnector.DollarValue(myVM.ChosenCurrency()))+"$)");
	        	feeCurrencyText.setText(myVM.ChosenCurrency().Symbol());
				
	        	courtesyOK.setChecked(myVM.CourtesyOK());
    		}
    	}catch (Exception ex){
    		MessageManager.Instance().AddMessage(ex.toString(), true);
    	}
    }
    
	private void selectButton(Currency currency){
    	btcButton.setEnabled(currency != Currency.MicroBitcoins);
    	appleButton.setEnabled(currency != Currency.Apples);
    	dollarButton.setEnabled(currency != Currency.Dollars);
    	yuanButton.setEnabled(currency != Currency.Yuans);
    	euroButton.setEnabled(currency != Currency.Euros);
    	
    	//euroButton.setPressed(pressed);
    }
    
    /*private class ButtonPresser implements Runnable{ //TODO: Internalize in "EasyButton". NOT WORKING
    	Button btn;
    	boolean pressed;
    	
    	public ButtonPresser(Button b, boolean press){
    		btn = b;
    		pressed = press;
    	}
    	public void run() {
        	btn.setPressed(pressed);
        }
    }*/
    
    private class ButtonListener implements OnClickListener {
		@Override
		public void onClick(View v) { //Select logic by btn id:
			try{
				switch(v.getId())
				{
					case R.id.courtesyCheckBox:
						myVM.CourtesyOK(courtesyOK.isChecked());
						break;
					case R.id.btcButton:
						myVM.ChosenCurrency(Currency.MicroBitcoins);
						break;
					case R.id.appleButton:
						myVM.ChosenCurrency(Currency.Apples);
						break;
					case R.id.dollarButton:
						myVM.ChosenCurrency(Currency.Dollars);
						break;
					case R.id.yuanButton:
						myVM.ChosenCurrency(Currency.Yuans);
						break;
					case R.id.euroButton:
						myVM.ChosenCurrency(Currency.Euros);
						break;
					case R.id.okSettingsButton:
						NavigationManager.Instance().setPage(PageTags.CHARGE_PAGE);
						break;
					default:
						MessageManager.Instance().AddMessage("Unknown button clicked: " + v.getId(), true);
						break;
				}
			}catch (Exception ex){
	    		MessageManager.Instance().AddMessage(ex.toString(), true);
	    	}
		}
    }
    
    private class TextListener extends EventListener<Integer>{
		@Override
		public void onEvent(Integer event) {
			try{
				switch(event){
					case R.id.addressText:
						myVM.Address(addressBox.getText().toString());
						break;
					case R.id.feeText:
						myVM.Fee(feeBox.getText().toString());
						break;
					default:
						MessageManager.Instance().AddMessage("Text change event Id not found.", true);
						break;
				}
			}catch (Exception ex){
	    		MessageManager.Instance().AddMessage(ex.toString(), true);
	    	}
		}
    }
    
    private class UpdateListener extends EventListener<Object> {
		@Override
		public void onEvent(Object event) {
			update();
		}
    }
}
