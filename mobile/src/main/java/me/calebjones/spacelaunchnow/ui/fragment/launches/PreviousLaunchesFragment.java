package me.calebjones.spacelaunchnow.ui.fragment.launches;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

import com.afollestad.materialdialogs.MaterialDialog;
import com.borax12.materialdaterangepicker.date.DatePickerDialog;
import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ContentViewEvent;
import com.crashlytics.android.answers.SearchEvent;
import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
import com.github.rahatarmanahmed.cpv.CircularProgressView;

import java.lang.reflect.Field;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import butterknife.ButterKnife;
import me.calebjones.spacelaunchnow.BuildConfig;
import me.calebjones.spacelaunchnow.content.adapter.LaunchAdapter;
import me.calebjones.spacelaunchnow.content.database.ListPreferences;
import me.calebjones.spacelaunchnow.content.database.SwitchPreferences;
import me.calebjones.spacelaunchnow.content.models.Strings;
import me.calebjones.spacelaunchnow.content.models.Launch;
import me.calebjones.spacelaunchnow.MainActivity;
import me.calebjones.spacelaunchnow.R;
import me.calebjones.spacelaunchnow.content.services.LaunchDataService;
import me.calebjones.spacelaunchnow.utils.Utils;
import timber.log.Timber;


public class PreviousLaunchesFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener, SearchView.OnQueryTextListener, DatePickerDialog.OnDateSetListener {

    private View view, empty;
    private RecyclerView mRecyclerView;
    private LaunchAdapter adapter;
    private String newURL;
    private FloatingActionMenu menu;
    private String start_date, end_date;
    private List<Launch> rocketLaunches;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private ListPreferences listPreferences;
    private SwitchPreferences switchPreferences;
    private SharedPreferences sharedPrefs;
    private int mScrollPosition;
    private FloatingActionButton agency, vehicle, country, location, reset;
    private int mScrollOffset = 4;
    private static final Field sChildFragmentManagerField;
    private LinearLayoutManager layoutManager;

    public PreviousLaunchesFragment() {
        // Required empty public constructor
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.listPreferences = ListPreferences.getInstance(getContext());
        this.switchPreferences = SwitchPreferences.getInstance(getContext());
        this.sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        this.rocketLaunches = new ArrayList();
        adapter = new LaunchAdapter(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        setHasOptionsMenu(true);

        if (!BuildConfig.DEBUG){
            Answers.getInstance().logContentView(new ContentViewEvent()
                    .putContentName("PreviousLaunchesFragment")
                    .putContentType("Fragment"));
        }

        LayoutInflater lf = getActivity().getLayoutInflater();

        view = lf.inflate(R.layout.fragment_previous_launches, container, false);
        ButterKnife.bind(getActivity());

        agency = (FloatingActionButton) view.findViewById(R.id.agency);
        vehicle = (FloatingActionButton) view.findViewById(R.id.vehicle);
        country = (FloatingActionButton) view.findViewById(R.id.country);
        location = (FloatingActionButton) view.findViewById(R.id.launch_location);
        reset = (FloatingActionButton) view.findViewById(R.id.reset);
        menu = (FloatingActionMenu) view.findViewById(R.id.menu);
        empty = view.findViewById(R.id.empty_launch_root);


        /*Set up Pull to refresh*/
        mSwipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.previous_swipe_refresh_layout);
        mSwipeRefreshLayout.setOnRefreshListener(this);

        mRecyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
        RecyclerView hideView = (RecyclerView) view.findViewById(R.id.recycler_view_staggered);
        if (mRecyclerView.getVisibility() != View.VISIBLE) {
            hideView.setVisibility(View.GONE);
            mRecyclerView.setVisibility(View.VISIBLE);
        }
        layoutManager = new LinearLayoutManager(getContext());
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setAdapter(adapter);
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener(){
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (Math.abs(dy) > mScrollOffset) {
                    if (dy > 0) {
                        menu.hideMenu(true);
                    } else {
                        menu.showMenu(true);
                    }
                }
                int topRowVerticalPosition =
                        (recyclerView == null || recyclerView.getChildCount() == 0) ? 0 : recyclerView.getChildAt(0).getTop();
                mSwipeRefreshLayout.setEnabled(topRowVerticalPosition >= 0);

            }
        });

        if (this.listPreferences.getPreviousFirstBoot()) {
            this.listPreferences.setPreviousFirstBoot(false);
            Timber.d("Previous Launch Fragment: First Boot.");
            getDefaultDateRange();
            if(this.listPreferences.getLaunchesPrevious() == null || this.listPreferences.getLaunchesPrevious().size() == 0){
                fetchData();
            } else {
                this.rocketLaunches.clear();
                displayLaunches();
            }
        } else {
            Timber.d("Previous Launch Fragment: Not First Boot.");
            this.rocketLaunches.clear();
            getDateRange();
            displayLaunches();
        }
        setUpFab();
        return view;
    }

    private void setUpFab() {
        menu.setClosedOnTouchOutside(true);

        createCustomAnimation();

        reset.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                switchPreferences.resetAllPrevFilters();
                if (switchPreferences.getPrevFiltered()){
                    switchPreferences.setPrevFiltered(false);
                    listPreferences.removeFilteredList();
                    getDefaultDateRange();
                    displayLaunches();
                }
                menu.close(true);
            }
        });

        agency.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAgencyDialog();
            }
        });
        vehicle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showVehicleDialog();
            }
        });
        country.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showCountryDialog();
            }
        });
        location.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLocationDialog();
            }
        });

    }

    private void showCountryDialog() {
        new MaterialDialog.Builder(getContext())
                .title("Select a Country")
                .content("Check an country below, to remove all filters use reset icon in the toolbar.")
                .items(R.array.country)
                .buttonRippleColorRes(R.color.colorAccentLight)
                .itemsCallbackMultiChoice(switchPreferences.getPrevCountryFiltered(), new MaterialDialog.ListCallbackMultiChoice() {
                    @Override
                    public boolean onSelection(MaterialDialog dialog, Integer[] which, CharSequence[] text) {
                        switchPreferences.setPrevCountryFiltered(which);
                        ArrayList<String> keyArray = new ArrayList<>();
                        for (int i = 0; i < which.length;i ++){
                            keyArray.add(text[i].toString());
                        }
                        if (keyArray.size() > 0) {
                            adapter.clear();
                            fetchDataFiltered(2, keyArray);
                        }
                        menu.toggle(false);
                        return true;
                    }
                })
                .positiveText("Filter")
                .negativeText("Close")
                .icon(ContextCompat.getDrawable(getContext(), R.mipmap.ic_launcher))
                .show();
    }

    private void showLocationDialog() {
        new MaterialDialog.Builder(getContext())
                .title("Select a Location")
                .content("Check an location below, to remove all filters use reset icon in the toolbar.")
                .items(R.array.location)
                .buttonRippleColorRes(R.color.colorAccentLight)
                .itemsCallbackMultiChoice(switchPreferences.getPrevLocationFiltered(), new MaterialDialog.ListCallbackMultiChoice() {
                    @Override
                    public boolean onSelection(MaterialDialog dialog, Integer[] which, CharSequence[] text) {
                        switchPreferences.setPrevLocationFiltered(which);
                        ArrayList<String> keyArray = new ArrayList<>();
                        for (int i = 0; i < which.length;i ++){
                            keyArray.add(text[i].toString());
                        }
                        if (keyArray.size() > 0) {
                            adapter.clear();
                            fetchDataFiltered(3, keyArray);
                        }
                        menu.toggle(false);
                        return true;
                    }
                })
                .positiveText("Filter")
                .negativeText("Close")
                .icon(ContextCompat.getDrawable(getContext(), R.mipmap.ic_launcher))
                .show();
    }

    private void showAgencyDialog() {
        new MaterialDialog.Builder(getContext())
                .title("Select an Agency")
                .content("Check an agency below, to remove all filters use reset icon in the toolbar.")
                .items(R.array.agencies)
                .buttonRippleColorRes(R.color.colorAccentLight)
                .itemsCallbackMultiChoice(switchPreferences.getPrevAgencyFiltered(), new MaterialDialog.ListCallbackMultiChoice() {
                    @Override
                    public boolean onSelection(MaterialDialog dialog, Integer[] which, CharSequence[] text) {
                        switchPreferences.setPrevAgencyFiltered(which);
                        ArrayList<String> keyArray = new ArrayList<>();
                        for (int i = 0; i < which.length;i ++){
                            keyArray.add(text[i].toString());
                        }
                        if (keyArray.size() > 0) {
                            adapter.clear();
                            fetchDataFiltered(0, keyArray);
                        }
                        menu.toggle(false);
                        return true;
                    }
                })
                .positiveText("Filter")
                .negativeText("Close")
                .icon(ContextCompat.getDrawable(getContext(), R.mipmap.ic_launcher))
                .show();
    }

    private void showVehicleDialog() {
        new MaterialDialog.Builder(getContext())
                .title("Select a Launch Vehicle")
                .content("Check a vehicle below, to remove all filters use reset icon in the toolbar.")
                .items(R.array.vehicles)
                .buttonRippleColorRes(R.color.colorAccentLight)
                .itemsCallbackMultiChoice(switchPreferences.getPrevVehicleFiltered(), new MaterialDialog.ListCallbackMultiChoice() {
                    @Override
                    public boolean onSelection(MaterialDialog dialog, Integer[] which, CharSequence[] text) {
                        switchPreferences.setPrevVehicleFiltered(which);
                        ArrayList<String> keyArray = new ArrayList<>();
                        for (int i = 0; i < which.length;i ++){
                            keyArray.add(text[i].toString());
                        }
                        if (keyArray.size() > 0) {
                            adapter.clear();
                            fetchDataFiltered(1, keyArray);
                        }
                        menu.toggle(false);
                        return true;
                    }
                })
                .positiveText("Filter")
                .negativeText("Close")
                .icon(ContextCompat.getDrawable(getContext(), R.mipmap.ic_launcher))
                .show();
    }

    private void setTitle() {
        ((MainActivity) getActivity()).setActionBarTitle(this.listPreferences.getPreviousTitle());
    }

    public void recreate(){
        recreate();
    }

    private String formatDatesForTitle(String start_date) {
        SimpleDateFormat in = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat out = new SimpleDateFormat("LLL yyyy");

        Date sDate;

        try {
            sDate = in.parse(start_date);
            return out.format(sDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    //TODO Test empty
    public void displayLaunches() {
        Timber.v("DisplayLaunches - Filtered - %s", switchPreferences.getPrevFiltered());
        if (!switchPreferences.getPrevFiltered()){
            rocketLaunches = listPreferences.getLaunchesPrevious();
        } else {
            rocketLaunches = listPreferences.getLaunchesPreviousFiltered();
        }

        if (rocketLaunches != null) {
            Timber.v("DisplayLaunches - List size: %s", rocketLaunches.size());

            adapter.clear();
            if (rocketLaunches.size() > 0) {
                List<Launch> launches = new ArrayList<>();
                Calendar rightNow = Calendar.getInstance();
                for (int i = 0; i < rocketLaunches.size(); i++){
                    if (rocketLaunches.get(i) != null && rocketLaunches.get(i).getNetstamp() > 0) {
                        final Date date = new Date((rocketLaunches.get(i).getNetstamp()) * 1000);

                        Calendar future = Utils.DateToCalendar(date);
                        rightNow.setTimeInMillis(System.currentTimeMillis());
                        long timeToFinish = future.getTimeInMillis() - rightNow.getTimeInMillis();
                        if (timeToFinish < 0) {
                            launches.add(rocketLaunches.get(i));
                        }
                    }
                }
                if (launches.size() > 0) {
                    empty.setVisibility(View.GONE);
                    adapter.addItems(launches);
                } else {
                    empty.setVisibility(View.VISIBLE);
                }
            } else {
                empty.setVisibility(View.VISIBLE);
            }
            //Animate the FAB's loading
            view.postDelayed(new Runnable() {
                @Override
                public void run() {
                    fabSlideIn();
                }
            }, 750);
        } else {
            empty.setVisibility(View.VISIBLE);
        }
    }

    public void fetchData() {
        showLoading();
        String url;
        if (listPreferences.isDebugEnabled()) {
            url = "https://launchlibrary.net/dev/launch/" + this.start_date + "/" + this.end_date + "?sort=desc&limit=1000";
        } else {
            url = "https://launchlibrary.net/1.2/launch/" + this.start_date + "/" + this.end_date + "?sort=desc&limit=1000";
        }
        Timber.d("Sending Intent URL: %s", url);
        Intent intent = new Intent(getContext(), LaunchDataService.class);
        intent.putExtra("URL", url);
        intent.setAction(Strings.ACTION_GET_PREV_LAUNCHES);
        getContext().startService(intent);
    }

    private void showLoading() {
        CircularProgressView progressView = (CircularProgressView)
                view.findViewById(R.id.progress_View);
        progressView.setVisibility(View.VISIBLE);
        progressView.startAnimation();
    }

    // Three types: 0 - Agency 1 - Vehicle 2 - Country
    public void fetchDataFiltered(int type, ArrayList<String> key) {
        Timber.d("Filtering by: %s", key.toString());

        Answers.getInstance().logSearch(new SearchEvent()
                    .putQuery(key.toString()));


        String title = key.toString().replaceAll("\\[", "").replaceAll("\\]","");
        if (switchPreferences.getPrevFiltered()){
            listPreferences.setPreviousTitle(listPreferences.getPreviousTitle() + " | " + title);
        } else {
            listPreferences.setPreviousTitle(title);
        }
        listPreferences.setPrevFilter(type, key);
        displayLaunches();
        setTitle();
    }

    private void createCustomAnimation() {

        AnimatorSet set = new AnimatorSet();

        ObjectAnimator scaleOutX = ObjectAnimator.ofFloat(menu.getMenuIconView(), "scaleX", 1.0f, 0.2f);
        ObjectAnimator scaleOutY = ObjectAnimator.ofFloat(menu.getMenuIconView(), "scaleY", 1.0f, 0.2f);

        ObjectAnimator scaleInX = ObjectAnimator.ofFloat(menu.getMenuIconView(), "scaleX", 0.2f, 1.0f);
        ObjectAnimator scaleInY = ObjectAnimator.ofFloat(menu.getMenuIconView(), "scaleY", 0.2f, 1.0f);

        scaleOutX.setDuration(50);
        scaleOutY.setDuration(50);

        scaleInX.setDuration(150);
        scaleInY.setDuration(150);

        scaleInX.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                menu.getMenuIconView().setImageResource(menu.isOpened()
                        ? R.drawable.ic_sort : R.drawable.ic_close);
            }
        });

        set.play(scaleOutX).with(scaleOutY);
        set.play(scaleInX).with(scaleInY).after(scaleOutX);
        set.setInterpolator(new OvershootInterpolator(2));

        menu.setIconToggleAnimatorSet(set);
    }

    //Required for DateRange Dialogue that returns data from the dialogue.
    @Override
    public void onDateSet(DatePickerDialog view, int year, int monthOfYear, int dayOfMonth,int yearEnd, int monthOfYearEnd, int dayOfMonthEnd) {
        monthOfYear = monthOfYear + 1;
        monthOfYearEnd = monthOfYearEnd + 1;

        if (monthOfYear == 0){
            monthOfYear = 1;
        }
        if (monthOfYearEnd == 0){
            monthOfYearEnd = 1;
        }
        String dayDateStart = dayOfMonth < 10 ? "0" + dayOfMonth : "" + dayOfMonth;
        String monthDateStart = monthOfYear < 10 ? "0" + monthOfYear : "" + monthOfYear;
        String dayDateEnd = dayOfMonthEnd < 10 ? "0" + dayOfMonthEnd : "" + dayOfMonthEnd;
        String monthDayEnd = monthOfYearEnd < 10 ? "0" + monthOfYearEnd : "" + monthOfYearEnd;

        start_date = year + "-" + monthDateStart + "-" + dayDateStart;
        end_date = yearEnd + "-" + monthDayEnd + "-" + dayDateEnd;

        if (switchPreferences.getPrevFiltered()){
            listPreferences.setPreviousTitle(listPreferences.getPreviousTitle()
                    + " | " + formatDatesForTitle(start_date)
                    + " - " + formatDatesForTitle(end_date));
        } else {
            listPreferences.setPreviousTitle(formatDatesForTitle(start_date)
                    + " - " + formatDatesForTitle(end_date));
            switchPreferences.setPrevFiltered(true);
        }

        setTitle();
        adapter.clear();
        fetchData();
    }
    public void getDateRange() {
        start_date = this.listPreferences.getStartDate();
        end_date = this.listPreferences.getEndDate();
    }

    public void getDefaultDateRange() {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DATE, 1);

        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        String formattedDate = df.format(c.getTime());

        this.start_date = this.listPreferences.getStartDate();
        this.end_date = String.valueOf(formattedDate);
        this.listPreferences.resetPreviousTitle();
        setTitle();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.previous_menu, menu);

        final MenuItem item = menu.findItem(R.id.action_search);
        final SearchView searchView = (SearchView) MenuItemCompat.getActionView(item);
        searchView.setOnQueryTextListener(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_date) {
            Calendar now = Calendar.getInstance();
            DatePickerDialog dpd = com.borax12.materialdaterangepicker.date.DatePickerDialog.newInstance(
                    PreviousLaunchesFragment.this,
                    now.get(Calendar.YEAR),
                    now.get(Calendar.MONTH),
                    now.get(Calendar.DAY_OF_MONTH)
            );
            dpd.show(getActivity().getFragmentManager(), "DatePicker");
            return true;
        }
        if (id == R.id.action_refresh) {
            adapter.clear();
            this.switchPreferences.setPrevFiltered(false);
            getDefaultDateRange();
            fetchData();
            return true;
        }

        if (id == R.id.return_home){
            mRecyclerView.scrollToPosition(0);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onQueryTextChange(String query) {
        // Here is where we are going to implement our filter logic
        final List<Launch> filteredModelList = filter(rocketLaunches, query);
        if (query.length() > 3){
            if (!BuildConfig.DEBUG){
                Answers.getInstance().logSearch(new SearchEvent()
                        .putQuery(query));
            }
        }
        adapter.animateTo(filteredModelList);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mRecyclerView.scrollToPosition(0);
            }
        }, 500);
//        mRecyclerView.scrollToPosition(0);
        return false;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    private List<Launch> filter(List<Launch> models, String query) {
        query = query.toLowerCase();

        final List<Launch> filteredModelList = new ArrayList<>();
        for (Launch model : models) {
            final String name = model.getName().toLowerCase();
            final String rocketName = model.getRocket().getName().toLowerCase();
            final String locationName = model.getLocation().getName().toLowerCase();
            String missionName;

            //If pad and agency exist add it to location, otherwise get whats always available
            if (model.getLocation().getPads().size() > 0 && model.getLocation().getPads().
                    get(0).getAgencies().size() > 0){
                missionName = model.getLocation().getPads().get(0).getAgencies().get(0).getName() + " " + (model.getRocket().getName());
            } else {
                missionName = model.getRocket().getName();
            }
            missionName = missionName.toLowerCase();

            if (rocketName.contains(query) || locationName.contains(query) || missionName.contains(query) || name.contains(query)) {
                filteredModelList.add(model);
            }
        }
        return filteredModelList;
    }

    @Override
    public void onResume() {
        Timber.d("OnResume!");
        super.onResume();
    }

    @Override
    public void onRefresh() {
        adapter.clear();
        this.switchPreferences.setPrevFiltered(false);
        this.switchPreferences.resetAllPrevFilters();
        getDefaultDateRange();
        fetchData();
    }

    private void fabSlideOut() {
        menu.animate().translationX(menu.getWidth() + 250).setInterpolator(new AccelerateInterpolator(1)).start();
    }

    private void fabSlideIn() {
        menu.animate().translationX(0).setInterpolator(new DecelerateInterpolator(4)).start();
    }

    static {
        Field f = null;
        try {
            f = Fragment.class.getDeclaredField("mChildFragmentManager");
            f.setAccessible(true);
        } catch (NoSuchFieldException e) {
            e.getLocalizedMessage();
            Timber.e("Error getting mChildFragmentManager field %s", e.getLocalizedMessage());
        }
        sChildFragmentManagerField = f;
    }

    @Override
    public void onDetach() {
        super.onDetach();

        if (sChildFragmentManagerField != null) {
            try {
                sChildFragmentManagerField.set(this, null);
            } catch (Exception e) {
                e.getLocalizedMessage();
                Crashlytics.logException(e);
                Timber.e("Error setting mChildFragmentManager field %s", e.getLocalizedMessage());
            }
        }
    }
}
