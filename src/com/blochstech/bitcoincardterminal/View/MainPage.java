package com.blochstech.bitcoincardterminal.View;

import com.blochstech.bitcoincardterminal.R;
import com.blochstech.bitcoincardterminal.Utils.EventListener;
import com.blochstech.bitcoincardterminal.ViewModel.ViewStateManagers.NavigationManager;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

//All view pages (so far) are actually singletons, this cannot be enforced on fragments
//which Android will destroy and recreate at whim.
//Hence all state in ViewModel.. as it should be.

//MainPage holds the Views in a ViewPager. MainPage supports changing page and may send page information to a navigationManager.
public class MainPage extends Fragment {
	//Adapter and pager:
	private MainFragmentAdapter mAdapter;
    private MainFragmentPager mPager;
    private OnPageChangeListener listener = new PageChangeListener();
	private UpdateListener updateListener = new UpdateListener();
    
    private int page = 0;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.mainpage, container, false);
        
        mAdapter = new MainFragmentAdapter(getFragmentManager());
        
        mPager = (MainFragmentPager) view.findViewById(R.id.pager);
        mPager.setAdapter(mAdapter);
        
        mPager.setOnPageChangeListener(listener);
        
        NavigationManager.Instance().UpdateEvent.register(updateListener);
        
        return view;
    }
    
    @Override
    public void onPause(){ //Fragment may be destroyed after this.
		NavigationManager.Instance().UpdateEvent.unregister(updateListener);
    	super.onPause();
    }
    
    @Override
    public void onResume(){
    	super.onResume();
		NavigationManager.Instance().UpdateEvent.register(updateListener);
    	update();
    }
    
    private void update(){
    	page = NavigationManager.Instance().CurrentPageNumber();
    	mPager.setCurrentItem(page);
    }
    
    private class PageChangeListener implements OnPageChangeListener
    {

		@Override
		public void onPageScrollStateChanged(int arg0) {
			//Nothing.
		}

		@Override
		public void onPageScrolled(int arg0, float arg1, int arg2) {
			//Nothing
		}

		@Override
		public void onPageSelected(int arg0) {
			//mAdapter.
			if(arg0 != page && mAdapter != null && mAdapter.getItem(arg0) != null){
				if(arg0 < page && !NavigationManager.Instance().AllowLeftSwipe()
					|| arg0 > page && !NavigationManager.Instance().AllowRightSwipe()){
					mPager.setCurrentItem(page);
				}else{
					mAdapter.getItem(arg0).onResume();
					page = arg0;
					NavigationManager.Instance().setPage(PageTags.Convert(page), true);
				}
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
