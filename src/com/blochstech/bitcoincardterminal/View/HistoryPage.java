package com.blochstech.bitcoincardterminal.View;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.blochstech.bitcoincardterminal.R;
import com.blochstech.bitcoincardterminal.Utils.EventListener;
import com.blochstech.bitcoincardterminal.Utils.SyntacticSugar;
import com.blochstech.bitcoincardterminal.ViewModel.HistoryPageVM;
import com.blochstech.bitcoincardterminal.ViewModel.ViewStateManagers.MessageManager;
import com.blochstech.bitcoincardterminal.ViewModel.ViewStateManagers.PageManager;

public class HistoryPage extends Fragment {
	private View myView;
	private HistoryPageVM myVM;
	private UpdateListener updateListener = new UpdateListener();
	
	private boolean initialized = false;
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
    	myView = inflater.inflate(R.layout.historypage, container, false);
    	
    	//Setup:
    	if(PageManager.Instance().isInitialized(PageManager.ViewModelTags.HISTORY_PAGE_VM)){
    		myVM = SyntacticSugar.<HistoryPageVM>castAs(PageManager.Instance().getVM(PageManager.ViewModelTags.HISTORY_PAGE_VM));
    	}
    	if(myVM == null){
    		myVM = new HistoryPageVM();
    		PageManager.Instance().setVM(PageManager.ViewModelTags.HISTORY_PAGE_VM, myVM);
    	}
    	
    	//Save views for faster access:
    	
    	//Begin:
        myVM.UpdateEvent.register(updateListener);
    	
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
    	update();
    }
	
	private void update(){
    	try{
    		if(initialized){
	    		//TODO:
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
}
