package com.blochstech.bitcoincardterminal;

import java.util.Calendar;

import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.NfcA;
import android.os.Bundle;
import android.app.PendingIntent;
import android.content.Context;
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
	
	public static MainActivity instance;
	
	public static Context GetMainContext(){
		Context res = instance != null ? instance.getApplicationContext() : null;
		return res;
	}
	
	
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        if(instance == null)
        	instance = this;
        
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
        
        //TEMP TEST
        /*try{
			TXObject parsedTX = TXUtil.ParseTXToObjectForm("0100000001C412E231F2941550BF4FB097FE231456072145BF9E8162670D3403E19E007D96020000008B483045022050CCF0CB8F8552BB13FE1338339572B578053428884D61DD5D7298249B906E9D02210091CDCC7DABB9E8D1BCAA47D6D022695EC49FB343CFA77385C4CAF90EB6BF990D014104E9C4C6AD2BE6D97BD4BBBA18FC51E62A0B4B393735FB8C0FB859253AAF4427789CE6D4D1B653C29D67B99E9DCCA1FBFAF1AB86AEBE50B5B277ECD8BA8DBCAC65FFFFFFFF025E150000000000001976A914B22A5A0F48C42A0219A4BFE146E2A2432D9F9E1388AC6D860000000000001976A9140044B6662B972525F7FB6C2B40D51AA4CB56BC5D88AC00000000");
			if(parsedTX != null)
				parsedTX = TXUtil.CorrectSignatureS(parsedTX);
			Log.i(Tags.APP_TAG, parsedTX.HexValue);
		}catch(Exception ex){
			Log.e(Tags.APP_TAG, "Failed to correct potential high S values in TX." + (ex!=null?ex.toString():""));
			if(Tags.DEBUG)
				ex.printStackTrace();
		}*/
        //TEMP TEST
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