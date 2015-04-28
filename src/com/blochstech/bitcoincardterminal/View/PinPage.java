package com.blochstech.bitcoincardterminal.View;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.blochstech.bitcoincardterminal.R;
import com.blochstech.bitcoincardterminal.Interfaces.Currency;
import com.blochstech.bitcoincardterminal.Utils.EventListener;
import com.blochstech.bitcoincardterminal.Utils.ShortNumberConverter;
import com.blochstech.bitcoincardterminal.Utils.SyntacticSugar;
import com.blochstech.bitcoincardterminal.ViewModel.PinPageVM;
import com.blochstech.bitcoincardterminal.ViewModel.ViewStateManagers.MessageManager;
import com.blochstech.bitcoincardterminal.ViewModel.ViewStateManagers.NavigationManager;
import com.blochstech.bitcoincardterminal.ViewModel.ViewStateManagers.PageManager;

//Everything in view classes will be destroyed at whim by Android. VMs and model/database should hold logic and information.
//Views defined in layout xml files.
public class PinPage extends Fragment {
/*http://stackoverflow.com/questions/2394935/can-i-underline-text-in-an-android-layout
 * TextView textView = (TextView) view.findViewById(R.id.textview);
SpannableString content = new SpannableString("Content");
content.setSpan(new UnderlineSpan(), 0, content.length(), 0);
textView.setText(content);*/
	private View myView;
	private PinPageVM myVM;
	private UpdateListener updateListener = new UpdateListener();
	
	private Button deleteButton;
	private Button cancelButton;
	private Button okButton;
	
	private Button zeroButton;
	
	private Button oneButton;
	private Button twoButton;
	private Button threeButton;
	
	private Button fourButton;
	private Button fiveButton;
	private Button sixButton;
	
	private Button sevenButton;
	private Button eightButton;
	private Button nineButton;
	
	private TextView claimedShortCharge;
	private TextView cardShortCharge;
	
	private TextView claimedCharge;
	private TextView claimedChargeBTC;
	
	private TextView pinCode;
	private TextView cardReadyText;
	
	private TextView noPINText;
	
	private OnClickListener btnListener = new ButtonListener();
	
	private boolean initialized = false;
	
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
    	myView = inflater.inflate(R.layout.pinpage, container, false);
    	
    	//Setup:
    	if(PageManager.Instance().isInitialized(PageManager.ViewModelTags.PIN_PAGE_VM)){
    		myVM = SyntacticSugar.<PinPageVM>castAs(PageManager.Instance().getVM(PageManager.ViewModelTags.PIN_PAGE_VM));
    	}
    	if(myVM == null){
    		myVM = new PinPageVM();
    		PageManager.Instance().setVM(PageManager.ViewModelTags.PIN_PAGE_VM, myVM);
    	}
    	
    	//Save views for faster access:
    	claimedShortCharge = SyntacticSugar.<TextView>castAs(myView.findViewById(R.id.claimedShortCharge));
    	cardShortCharge = SyntacticSugar.<TextView>castAs(myView.findViewById(R.id.cardShortCharge));
    	
    	claimedCharge = SyntacticSugar.<TextView>castAs(myView.findViewById(R.id.claimedCharge));
    	claimedChargeBTC = SyntacticSugar.<TextView>castAs(myView.findViewById(R.id.claimedChargeBTC));
    	
    	pinCode = SyntacticSugar.<TextView>castAs(myView.findViewById(R.id.pinCode));
    	cardReadyText = SyntacticSugar.<TextView>castAs(myView.findViewById(R.id.cardReadyText));
    	
    	noPINText = SyntacticSugar.<TextView>castAs(myView.findViewById(R.id.noPINText));
    	
    	deleteButton = SyntacticSugar.<Button>castAs(myView.findViewById(R.id.deletePinButton));
    	cancelButton = SyntacticSugar.<Button>castAs(myView.findViewById(R.id.cancelPinButton));
    	okButton = SyntacticSugar.<Button>castAs(myView.findViewById(R.id.okPinButton));
    	
    	zeroButton = SyntacticSugar.<Button>castAs(myView.findViewById(R.id.zeroPinButton));
    	
    	oneButton = SyntacticSugar.<Button>castAs(myView.findViewById(R.id.onePinButton));
    	twoButton = SyntacticSugar.<Button>castAs(myView.findViewById(R.id.twoPinButton));
    	threeButton = SyntacticSugar.<Button>castAs(myView.findViewById(R.id.threePinButton));
    	
    	fourButton = SyntacticSugar.<Button>castAs(myView.findViewById(R.id.fourPinButton));
    	fiveButton = SyntacticSugar.<Button>castAs(myView.findViewById(R.id.fivePinButton));
    	sixButton = SyntacticSugar.<Button>castAs(myView.findViewById(R.id.sixPinButton));
    	
    	sevenButton = SyntacticSugar.<Button>castAs(myView.findViewById(R.id.sevenPinButton));
    	eightButton = SyntacticSugar.<Button>castAs(myView.findViewById(R.id.eightPinButton));
    	nineButton = SyntacticSugar.<Button>castAs(myView.findViewById(R.id.ninePinButton));

    	//Begin:
        myVM.UpdateEvent.register(updateListener);

        deleteButton.setOnClickListener(btnListener);
        cancelButton.setOnClickListener(btnListener);
        okButton.setOnClickListener(btnListener);
        
        zeroButton.setOnClickListener(btnListener);
        
        oneButton.setOnClickListener(btnListener);
        twoButton.setOnClickListener(btnListener);
        threeButton.setOnClickListener(btnListener);
        
        fourButton.setOnClickListener(btnListener);
        fiveButton.setOnClickListener(btnListener);
        sixButton.setOnClickListener(btnListener);
        
        sevenButton.setOnClickListener(btnListener);
        eightButton.setOnClickListener(btnListener);
        nineButton.setOnClickListener(btnListener);
    	
    	initialized = true;
    	update();
        
        return myView;
    }
    
    @Override
    public void onPause(){ //Fragment may be destroyed after this.
    	if(myVM != null && myVM.UpdateEvent != null)
    		myVM.UpdateEvent.unregister(updateListener);
    	super.onPause();
    }
    
    @Override
    public void onResume(){
    	super.onResume();
    	if(myVM != null && myVM.UpdateEvent != null)
    		myVM.UpdateEvent.register(updateListener);
    	if(myVM != null)
    		myVM.stateUpdate();
    	update(); //This is not always necessary, if Android has the fragment loaded while an event happens. This is not always the case.
    }
    
    private void update(){
    	try{
    		if(initialized){
    			//Normal updates:
    			if(myVM.CardsShortCharge().isEmpty()){
    				claimedShortCharge.setText(ShortNumberConverter.ToShort(myVM.BitcoinPrice()));
    			}else{
    				claimedShortCharge.setText(myVM.CardsShortCharge());
    			}
    			cardShortCharge.setText(myVM.CardPrice());
    			
    			claimedCharge.setText(String.format("%.2f", myVM.Price()) + " " + myVM.Currency().Symbol());
				claimedChargeBTC.setText(String.format("%.8f", myVM.BitcoinPrice()) + " " + Currency.Bitcoins.Symbol());
				
				cardReadyText.setText(myVM.CardMessage());
	    		pinCode.setText("PIN: " + myVM.PinCode());
	    		
	    		//Visibility:
	    		int visibility = myVM.PINRequired() ? View.VISIBLE : View.GONE;
	    		pinCode.setVisibility(visibility);
	    		deleteButton.setVisibility(visibility);
	    		zeroButton.setVisibility(visibility);
	    		oneButton.setVisibility(visibility);
	    		twoButton.setVisibility(visibility);
	    		threeButton.setVisibility(visibility);
	    		fourButton.setVisibility(visibility);
	    		fiveButton.setVisibility(visibility);
	    		sixButton.setVisibility(visibility);
	    		sevenButton.setVisibility(visibility);
	    		eightButton.setVisibility(visibility);
	    		nineButton.setVisibility(visibility);
	    		
	    		noPINText.setVisibility(!myVM.PINRequired() ? View.VISIBLE : View.GONE);
	    		
	    		visibility = myVM.CourtesyOk() || myVM.PINRequired() ? View.VISIBLE : View.GONE;
	    		okButton.setVisibility(visibility);
    		}
    	}catch (Exception ex){
    		MessageManager.Instance().AddMessage(ex.toString(), true);
    	}
    }
    
    private class ButtonListener implements OnClickListener {
		@Override
		public void onClick(View v) { //Select logic by btn id:
			try{
				switch(v.getId())
				{
					case R.id.deletePinButton:
						myVM.pressPinButton(10);
						break;
					case R.id.cancelPinButton:
						NavigationManager.Instance().setPage(PageTags.CHARGE_PAGE);
						break;
					case R.id.okPinButton:
						myVM.okPressed();
						break;
					case R.id.zeroPinButton:
						myVM.pressPinButton(0);
						break;
					case R.id.onePinButton:
						myVM.pressPinButton(1);
						break;
					case R.id.twoPinButton:
						myVM.pressPinButton(2);
						break;
					case R.id.threePinButton:
						myVM.pressPinButton(3);
						break;
					case R.id.fourPinButton:
						myVM.pressPinButton(4);
						break;
					case R.id.fivePinButton:
						myVM.pressPinButton(5);
						break;
					case R.id.sixPinButton:
						myVM.pressPinButton(6);
						break;
					case R.id.sevenPinButton:
						myVM.pressPinButton(7);
						break;
					case R.id.eightPinButton:
						myVM.pressPinButton(8);
						break;
					case R.id.ninePinButton:
						myVM.pressPinButton(9);
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
    
    private class UpdateListener extends EventListener<Object> {
		@Override
		public void onEvent(Object event) {
			update();
		}
    }
}
