package com.blochstech.bitcoincardterminal.View;

import com.blochstech.bitcoincardterminal.ViewModel.ViewStateManagers.MessageManager;
import com.blochstech.bitcoincardterminal.ViewModel.ViewStateManagers.NavigationManager;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;

public class MainFragmentPager extends ViewPager {
	
	public MainFragmentPager(Context context) {
		super(context);
	}
	
	public MainFragmentPager(Context context, AttributeSet attrs) { 
		super(context, attrs);
	}
	
	
	@Override 
	public boolean onTouchEvent(android.view.MotionEvent event) {
		
		if(event.getActionMasked() == 2 && !NavigationManager.Instance().AllowLeftSwipe() && !NavigationManager.Instance().AllowRightSwipe())
		{
			return false;
		}
		
		return super.onTouchEvent(event);
	} 
	
	@Override
	public void setCurrentItem(int i)
	{
		try{
			super.setCurrentItem(i);
		}catch(Exception ex){
			MessageManager.Instance().AddMessage("Failed to set page to page #" + i + ". Likely the page's startup code or XML layout file is invalid."
					+ " Error: " + (ex!=null?ex.getMessage():"NoErrorMessage"), true);
		}
	}
}
