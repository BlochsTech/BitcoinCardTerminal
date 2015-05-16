package com.blochstech.bitcoincardterminal;

import java.util.Calendar;

import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.NfcA;
import android.os.Bundle;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.Menu;
import android.view.MenuItem;

import com.blochstech.bitcoincardterminal.Model.Model;
import com.blochstech.bitcoincardterminal.Model.Communication.CurrencyApiConnector;
import com.blochstech.bitcoincardterminal.View.MainPage;
import com.blochstech.bitcoincardterminal.View.PageTags;
import com.blochstech.bitcoincardterminal.ViewModel.ViewStateManagers.MessageManager;
import com.blochstech.bitcoincardterminal.ViewModel.ViewStateManagers.NavigationManager;

//This class should get system intents and host the main view fragment.
//This class should let other classes handle intents and such (model only).
public class MainActivity extends FragmentActivity {
	
	private NfcAdapter mAdapter;
	private PendingIntent mPendingIntent;
	private String[][] techListsArray;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.fragment_placeholder);
        if (findViewById(R.id.reuseablePlaceholder) != null) {
			if (savedInstanceState == null) {
				MainPage mainPage = new MainPage();
				FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
				if(getSupportFragmentManager().findFragmentById(R.id.reuseablePlaceholder) != null)
				{
					transaction.replace(R.id.reuseablePlaceholder, mainPage);
				}
				else
				{
					transaction.add(R.id.reuseablePlaceholder, mainPage);
				}

				transaction.commit();
			}
		}
        
        ListenForNFCIntent();
        CurrencyApiConnector.SynchPrices("MainInit");
        
        MessageManager.Instance().AddMessage("Init " + Calendar.getInstance().getTime().toString(), false);
        
        Model.Instance(); //Init Model. (starts network task, among other things, that will upload pending TXs if there is network)
    }
    
    private void ListenForNFCIntent(){
    	mAdapter = NfcAdapter.getDefaultAdapter(this);
        mPendingIntent = PendingIntent.getActivity(
        		this,
        		0,
        		new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        		0);
        IntentFilter filter = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);
        filter.addDataScheme("vnd.android.nfc");
        techListsArray = new String[][] { new String[] { NfcA.class.getName() } };
        Intent launchIntent = getIntent();
        if(launchIntent != null && launchIntent.getAction().equals(NfcAdapter.ACTION_TECH_DISCOVERED))
        {
        	onNewIntent(launchIntent);
        }
    }
    
    @Override
    public void onResume(){
    	super.onResume();
    	if(mAdapter != null)
    		mAdapter.enableForegroundDispatch(this, mPendingIntent, null, techListsArray);
    	//Re-start listening when app returns.
    }
    
    @Override
    public void onPause(){
    	super.onPause();
    	Model.Instance().persistData();
    	if(mAdapter != null)
    		mAdapter.disableForegroundDispatch(this); //Stop listening when app not active.
    }
    
    @Override
    public void onNewIntent(Intent intent){
    	
    	if(intent != null && intent.getAction().equals(NfcAdapter.ACTION_TECH_DISCOVERED))
        {
	    	Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
	    	Model.Instance().setCard(tag);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }  
    
    @Override  
    public boolean onOptionsItemSelected(MenuItem item) {  
        switch (item.getItemId()) {  
            case R.id.action_settings:  
            	NavigationManager.Instance().setPage(PageTags.SETTINGS_PAGE);
            	return true;
              default:  
                return super.onOptionsItemSelected(item);  
        }  
    } 
}