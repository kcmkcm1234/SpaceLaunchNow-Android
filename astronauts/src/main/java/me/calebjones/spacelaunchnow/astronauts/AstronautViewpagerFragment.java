package me.calebjones.spacelaunchnow.astronauts;

import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.lifecycle.ViewModelProviders;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;
import me.calebjones.spacelaunchnow.astronauts.list.AstronautListFragment;
import me.calebjones.spacelaunchnow.common.base.BaseFragment;
import me.spacelaunchnow.astronauts.R;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.tabs.TabLayout;

public class AstronautViewpagerFragment extends BaseFragment {

    private AstronautListViewModel mViewModel;
    private ViewPager viewPager;
    private PagerAdapter pagerAdapter;
    private TabLayout tabLayout;
    private Context context;
    private AstronautListFragment activeFragment;
    private AstronautListFragment retiredFragment;
    private AstronautListFragment lostFragment;
    private AstronautListFragment inTrainingFragment;
    private AstronautListFragment otherFragment;

    public static AstronautViewpagerFragment newInstance() {
        return new AstronautViewpagerFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState)  {
        this.context = getActivity().getApplicationContext();

        View inflatedView = inflater.inflate(R.layout.astronaut_list_fragment, container, false);

        tabLayout = inflatedView.findViewById(R.id.tabLayout);
        tabLayout.addTab(tabLayout.newTab().setText("Active"));
        tabLayout.addTab(tabLayout.newTab().setText("In Training"));
        tabLayout.addTab(tabLayout.newTab().setText("Lost in Service"));
        tabLayout.addTab(tabLayout.newTab().setText("Retired"));
        tabLayout.addTab(tabLayout.newTab().setText("Other"));
        viewPager = inflatedView.findViewById(R.id.viewpager);

        pagerAdapter = new PagerAdapter
                (getChildFragmentManager(), tabLayout.getTabCount());
        viewPager.setOffscreenPageLimit(2);
        viewPager.setAdapter(pagerAdapter);
        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
        return inflatedView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = ViewModelProviders.of(this).get(AstronautListViewModel.class);
    }




    public class PagerAdapter extends FragmentPagerAdapter {
        int mNumOfTabs;

        public PagerAdapter(FragmentManager fm, int NumOfTabs) {
            super(fm);
            this.mNumOfTabs = NumOfTabs;
        }

        @Override
        public Fragment getItem(int position) {

            switch (position) {
                case 0:
                    if (activeFragment != null) {
                        return activeFragment;
                    } else {
                        return AstronautListFragment.newInstance();
                    }
                case 1:
                    if (inTrainingFragment != null) {
                        return inTrainingFragment;
                    } else {
                        return AstronautListFragment.newInstance();
                    }
                case 2:
                    if (lostFragment != null) {
                        return lostFragment;
                    } else {
                        return AstronautListFragment.newInstance();
                    }
                case 3:
                    if (retiredFragment != null) {
                        return retiredFragment;
                    } else {
                        return AstronautListFragment.newInstance();
                    }
                case 4:
                    if (otherFragment != null) {
                        return otherFragment;
                    } else {
                        return AstronautListFragment.newInstance();
                    }
                default:
                    return null;
            }
        }

        @NonNull
        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            Fragment createdFragment = (Fragment) super.instantiateItem(container, position);
            // save the appropriate reference depending on position
            switch (position) {
                case 0:
                    activeFragment = (AstronautListFragment) createdFragment;
                    break;
                case 1:
                    inTrainingFragment = (AstronautListFragment) createdFragment;
                    break;
                case 2:
                    lostFragment = (AstronautListFragment) createdFragment;
                    break;
                case 3:
                    retiredFragment = (AstronautListFragment) createdFragment;
                    break;
                case 4:
                    otherFragment = (AstronautListFragment) createdFragment;
                    break;
            }
            return createdFragment;
        }

        @Override
        public int getCount() {
            return mNumOfTabs;
        }

        @Override
        public int getItemPosition(Object object) {
            return POSITION_NONE;
        }
    }

}
