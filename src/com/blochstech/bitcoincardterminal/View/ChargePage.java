package com.blochstech.bitcoincardterminal.View;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.blochstech.bitcoincardterminal.R;
import com.blochstech.bitcoincardterminal.Utils.EventListener;
import com.blochstech.bitcoincardterminal.Utils.SyntacticSugar;
import com.blochstech.bitcoincardterminal.ViewModel.ChargePageVM;
import com.blochstech.bitcoincardterminal.ViewModel.ViewStateManagers.MessageManager;
import com.blochstech.bitcoincardterminal.ViewModel.ViewStateManagers.NavigationManager;
import com.blochstech.bitcoincardterminal.ViewModel.ViewStateManagers.PageManager;

//Everything in view classes will be destroyed at whim by Android. VMs and model/database should hold logic and information.
//Views defined in layout xml files.
public class ChargePage extends Fragment {
	private View myView;
	private ChargePageVM myVM;
	private UpdateListener updateListener = new UpdateListener();
	
	private TextView currencyText;
	private EasyText priceBox;
	private Button okButton;
	
	private OnClickListener btnListener = new ButtonListener();
	private TextListener textListener = new TextListener();
	
	private boolean initialized = false;
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
    	myView = inflater.inflate(R.layout.chargepage, container, false);
    	
    	//Setup:
    	if(PageManager.Instance().isInitialized(PageManager.ViewModelTags.CHARGE_PAGE_VM)){
    		myVM = SyntacticSugar.<ChargePageVM>castAs(PageManager.Instance().getVM(PageManager.ViewModelTags.CHARGE_PAGE_VM));
    	}
    	if(myVM == null){
    		myVM = new ChargePageVM();
    		PageManager.Instance().setVM(PageManager.ViewModelTags.CHARGE_PAGE_VM, myVM);
    	}
    	
    	//Save views for faster access:
    	priceBox = new EasyText(SyntacticSugar.<EditText>castAs(myView.findViewById(R.id.priceBox)));
    	okButton = SyntacticSugar.<Button>castAs(myView.findViewById(R.id.okPriceButton));
    	currencyText = SyntacticSugar.<TextView>castAs(myView.findViewById(R.id.priceCurrency));
    	
    	//Begin:
        myVM.UpdateEvent.register(updateListener);
    	
    	okButton.setOnClickListener(btnListener);
    	priceBox.updateEvent.register(textListener);
    	
    	initialized = true;
    	update();
        
        return myView;
    }
    
    @Override
    public void onPause(){ //Fragment may be destroyed after this.
    	if(myVM != null && myVM.UpdateEvent != null)
    		myVM.UpdateEvent.unregister(updateListener);
    	if(priceBox != null)
    		priceBox.ignoreTextChanges(true);
    	super.onPause();
    }
    
    @Override
    public void onResume(){
    	super.onResume();
    	if(myVM != null && myVM.UpdateEvent != null)
    		myVM.UpdateEvent.register(updateListener);
    	if(priceBox != null)
    		priceBox.ignoreTextChanges(false);
    	if(myVM != null)
    		myVM.stateUpdate();
    	update();
    }
	
	private void update(){
    	try{
    		if(initialized){
	    		priceBox.setText(String.format("%.1f", myVM.Price())); //Means precision = 7 and floating point to decimal conversion.
	    		//priceBox.setValid(myVM.PriceValid());
	    		currencyText.setText(myVM.Currency().Symbol());
    		}
    	}catch (Exception ex){
    		MessageManager.Instance().AddMessage(ex.toString(), true);
    	}
    }
	
	private class UpdateListener extends EventListener<Object> {
		@Override
		public void onEvent(Object event) {
			update();
		}
    }
	
	private class ButtonListener implements OnClickListener {
		@Override
		public void onClick(View v) { //Select logic by btn id:
			try{
				switch(v.getId())
				{
					case R.id.okPriceButton:
						myVM.CommitPrice();
						NavigationManager.Instance().setPage(PageTags.PIN_PAGE);
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
					case R.id.priceBox:
						myVM.Price(priceBox.getText());
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
}
