package com.blochstech.bitcoincardterminal.View;

import java.util.ArrayList;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

//This class should never be used directly, go via MainPage or its ViewModel.
//Thankfully, since it is not static this shouldbe automatically enforced.
class MainFragmentAdapter extends FragmentStatePagerAdapter {
	private final ArrayList<Fragment> pages;
	//private int currentPage = 0;
	
	MainFragmentAdapter(FragmentManager fm){
		super(fm);
		pages = new ArrayList<Fragment>();
		pages.add(new ChargePage());
		pages.add(new HistoryPage());
		pages.add(new PinPage());
		pages.add(new SettingsPage());
	}
	
	@Override
    public int getCount() {
        return pages.size();
    }

    @Override
    public Fragment getItem(int position) {
    	if(pages.size() > position && position >= 0){
    		return pages.get(position);
    	}else{
    		return null;
    	}
    }
}
