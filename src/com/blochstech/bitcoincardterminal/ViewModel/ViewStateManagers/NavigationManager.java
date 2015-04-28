package com.blochstech.bitcoincardterminal.ViewModel.ViewStateManagers;

import com.blochstech.bitcoincardterminal.Utils.Event;
import com.blochstech.bitcoincardterminal.View.PageTags;

//Updated directly by MainPage.
//Navstate changes set here and events sent to MainPage.
//The core idea is to avoid any one View having to know all the other Views in order to update say a global status icon,
	//message or page number..
public class NavigationManager {
		//Singleton pattern:
		private static NavigationManager instance = null;
		public static NavigationManager Instance()
		{
			if(instance == null)
			{
				instance = new NavigationManager();
			}
			return instance;
		}
		private NavigationManager()
		{
			UpdateEvent = new Event<Object>(this);
		}
		//Singleton pattern end.
		
		public Event<Object> UpdateEvent;
		
		private boolean allowRightSwipe = true, allowLeftSwipe = true;
		public boolean AllowRightSwipe(){
			return allowRightSwipe;
		}
		public boolean AllowLeftSwipe(){
			return allowLeftSwipe;
		}
		public void allowSwipes(boolean right, boolean left){
			allowRightSwipe = right;
			allowLeftSwipe = left;
		}
		
		private int currentPage = 0;
		public int CurrentPageNumber(){
			return currentPage;
		}
		private PageTags currentPageEnum = PageTags.CHARGE_PAGE;
		public PageTags CurrentPage(){
			return currentPageEnum;
		}
		private void setPage(int page){
			if(page < 4 && page >= 0){
				currentPage = page;
			}
		}
		
		public void setPage(PageTags page){
			setPage(page, false);
		}
		public void setPage(PageTags page, boolean quiet){
			if(page == PageTags.CHARGE_PAGE){
				setPage(0);
				allowSwipes(true, false);
			}
			if(page == PageTags.HISTORY_PAGE){
				setPage(1);
				allowSwipes(false, true);
			}
			if(page == PageTags.PIN_PAGE){
				setPage(2);
				allowSwipes(false, false);
			}
			if(page == PageTags.SETTINGS_PAGE){
				setPage(3);
				allowSwipes(false, false);
			}
			
			currentPageEnum = page;
			if(!quiet)
				UpdateEvent.fire(this, null);
		}
}
