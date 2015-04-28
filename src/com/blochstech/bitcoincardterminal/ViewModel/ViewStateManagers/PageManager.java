package com.blochstech.bitcoincardterminal.ViewModel.ViewStateManagers;

import java.util.HashMap;

import android.text.TextUtils;

//Other classes should update this class.
//Other classes should subscribe to events from this class.
//The core idea is to avoid any one View having to know all the other Views in order to update say a global status icon,
	//message or page number..
public class PageManager {
	//Singleton pattern:
	private static PageManager instance = null;
	public static PageManager Instance()
	{
		if(instance == null)
		{
			instance = new PageManager();
		}
		return instance;
	}
	private PageManager()
	{
		//Hold page VMs so they don't need to reload every time Android kills a fragment.
		ViewModels = new HashMap<String, Object>();
	}
	//Singleton pattern end.
	
	//PROPERTIES:
	private HashMap<String, Object> ViewModels;
	public Object getVM(String key)
	{
		try{
			Object temp = null;
			if(!TextUtils.isEmpty(key) && ViewModels.containsKey(key)){
				temp = ViewModels.get(key);
			}
			return temp;
		}catch(Exception ex)
		{
			MessageManager.Instance().AddMessage("Error getting VM: " + ex.toString(), true);
			return null;
		}
	}
	public void setVM(String key, Object vm){
		try{
			if(vm != null && !TextUtils.isEmpty(key)){
				ViewModels.put(key, vm);
			}
		}catch(Exception ex){
			MessageManager.Instance().AddMessage("Error setting VM: " + ex.toString(), true);
		}
	}
	public boolean isInitialized(String key){
		try{
			if(!TextUtils.isEmpty(key) && ViewModels.containsKey(key)){
				return true;
			}else{
				return false;
			}
		}catch(Exception ex){
			MessageManager.Instance().AddMessage("Error checking intialization of VM: " + ex.toString(), true);
			return false;
		}
	}
	
	public class ViewModelTags{
		public static final String SETTINGS_PAGE_VM = "SettingsPageVM";
		public static final String CHARGE_PAGE_VM = "ChargePageVM";
		public static final String PIN_PAGE_VM = "PinPageVM";
		public static final String HISTORY_PAGE_VM = "HistoryPageVM";
	}
}
