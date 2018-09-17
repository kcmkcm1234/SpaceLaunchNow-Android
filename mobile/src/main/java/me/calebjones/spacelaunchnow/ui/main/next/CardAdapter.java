package me.calebjones.spacelaunchnow.ui.main.next;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SectionIndexer;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.simplelist.MaterialSimpleListItem;
import com.bumptech.glide.load.MultiTransformation;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.crashlytics.android.Crashlytics;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.realm.RealmList;
import jp.wasabeef.glide.transformations.BlurTransformation;
import jp.wasabeef.glide.transformations.ColorFilterTransformation;
import jp.wasabeef.glide.transformations.gpu.BrightnessFilterTransformation;
import me.calebjones.spacelaunchnow.R;
import me.calebjones.spacelaunchnow.content.data.LaunchStatus;
import me.calebjones.spacelaunchnow.content.database.ListPreferences;
import me.calebjones.spacelaunchnow.content.util.DialogAdapter;
import me.calebjones.spacelaunchnow.data.models.main.Landing;
import me.calebjones.spacelaunchnow.data.models.main.Launch;
import me.calebjones.spacelaunchnow.data.models.main.Stage;
import me.calebjones.spacelaunchnow.data.models.realm.RealmStr;
import me.calebjones.spacelaunchnow.ui.launchdetail.activity.LaunchDetailActivity;
import me.calebjones.spacelaunchnow.utils.GlideApp;
import me.calebjones.spacelaunchnow.utils.Utils;
import me.calebjones.spacelaunchnow.utils.analytics.Analytics;
import me.calebjones.spacelaunchnow.utils.transformations.SaturationTransformation;
import me.calebjones.spacelaunchnow.utils.views.CountDownTimer;
import timber.log.Timber;

import static me.calebjones.spacelaunchnow.utils.GlideOptions.bitmapTransform;

/**
 * Adapts UpcomingLaunch data to the LaunchFragment
 */
public class CardAdapter extends RecyclerView.Adapter<CardAdapter.ViewHolder> implements SectionIndexer {

    private static ListPreferences sharedPreference;
    public int position;
    private RealmList<Launch> launchList;
    private Context context;
    private Calendar rightNow;
    private SharedPreferences sharedPref;
    private Boolean night;
    private Boolean play = false;
    private int nightColor;
    private int color;
    private int accentColor;

    public CardAdapter(Context context) {
        rightNow = Calendar.getInstance();
        launchList = new RealmList<>();
        this.context = context;
        if (Utils.checkPlayServices(this.context)) {
            play = true;
        }
        nightColor = ContextCompat.getColor(context, R.color.dark_theme_secondary_text_color);
        color = ContextCompat.getColor(context, R.color.colorTextSecondary);
        accentColor = ContextCompat.getColor(context, R.color.colorAccent);
        sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        sharedPreference = ListPreferences.getInstance(context);
        night = sharedPreference.isNightModeActive(context);
    }

    public static Calendar DateToCalendar(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return cal;
    }

    public void addItems(List<Launch> launchList) {
        if (this.launchList != null) {
            this.launchList.addAll(launchList);
        } else {
            this.launchList = new RealmList<>();
            this.launchList.addAll(launchList);
        }
        this.notifyDataSetChanged();
    }

    public void clear() {
        if (launchList != null) {
            launchList.clear();
            this.notifyDataSetChanged();
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.content_card, viewGroup, false);
        return new ViewHolder(v);
    }

    @SuppressLint({"SetTextI18n", "StringFormatMatches"})
    @Override
    public void onBindViewHolder(final ViewHolder holder, int i) {
        Launch launchItem = launchList.get(i);
        Timber.i("Binding %s", launchItem.getName());
        String title;
        try {
            if (launchItem.isValid()) {
                if (launchItem.getRocket().getConfiguration() != null) {
                    if (launchItem.getRocket().getConfiguration().getLaunchServiceProvider() != null) {
                        title = launchItem.getRocket().getConfiguration().getLaunchServiceProvider().getName() + " | " + (launchItem.getRocket().getConfiguration().getName());
                    } else {
                        title = launchItem.getRocket().getConfiguration().getName();
                    }
                } else if (launchItem.getName() != null) {
                    title = launchItem.getName();
                } else {
                    Timber.e("Error - launch item is effectively null.");
                    title = context.getString(R.string.error_unknown_launch);
                }

                holder.title.setText(title);

                //Retrieve missionType
                if (launchItem.getMission() != null) {
                    Utils.setCategoryIcon(holder.categoryIcon, launchItem.getMission().getTypeName(), true);
                } else {
                    holder.categoryIcon.setImageResource(R.drawable.ic_unknown_white);
                }

                double dlat = 0;
                double dlon = 0;
                if (launchItem.getPad() != null) {
                    dlat = Double.parseDouble(launchItem.getPad().getLatitude());
                    dlon = Double.parseDouble(launchItem.getPad().getLongitude());
                }

                holder.landingView.setVisibility(View.INVISIBLE);
                if (launchItem.getRocket().getFirstStage() != null && launchItem.getRocket().getFirstStage().size() >= 1){
                    if (launchItem.getRocket().getFirstStage().size() > 1){
                        String stagesText = "";
                        for (Stage stage :  launchItem.getRocket().getFirstStage()){
                            if (stage.getLanding().getLandingLocation() != null) {
                                stagesText = stagesText + stage.getLanding().getLandingLocation().getAbbrev() + " ";
                            }
                        }
                        holder.landingView.setVisibility(View.VISIBLE);
                        holder.landing.setText(stagesText);
                    } else if (launchItem.getRocket().getFirstStage().size() == 1){
                        if (launchItem.getRocket().getFirstStage().first().getLanding() != null){
                            Landing landing = launchItem.getRocket().getFirstStage().first().getLanding();
                            if (landing.getLandingLocation() != null) {
                                holder.landingView.setVisibility(View.VISIBLE);
                                holder.landing.setText(landing.getLandingLocation().getAbbrev());
                            }
                        }
                    }
                }

                if (launchItem.getRocket().getConfiguration().getImageUrl() != null) {
                    holder.launchImage.setVisibility(View.VISIBLE);
                    GlideApp.with(context)
                            .load(launchItem.getRocket().getConfiguration().getImageUrl())
                            .placeholder(R.drawable.placeholder)
                            .into(holder.launchImage);
                } else if (launchItem.getRocket().getConfiguration().getLaunchServiceProvider().getImageUrl() != null) {
                    holder.launchImage.setVisibility(View.VISIBLE);
                    GlideApp.with(context)
                            .load(launchItem.getRocket().getConfiguration().getLaunchServiceProvider().getImageUrl())
                            .placeholder(R.drawable.placeholder)
                            .into(holder.launchImage);
                } else if (launchItem.getRocket().getConfiguration().getLaunchServiceProvider().getLogoUrl() != null) {
                    holder.launchImage.setVisibility(View.VISIBLE);
                    GlideApp.with(context)
                            .load(launchItem.getRocket().getConfiguration().getLaunchServiceProvider().getLogoUrl())
                            .placeholder(R.drawable.placeholder)
                            .into(holder.launchImage);
                } else {
                    holder.launchImage.setVisibility(View.GONE);
                }

                holder.status.setText(LaunchStatus.getLaunchStatusTitle(context, launchItem.getStatus().getId()));
                holder.statusPill.setCardBackgroundColor(LaunchStatus.getLaunchStatusColor(context, launchItem.getStatus().getId()));

                //If timestamp is available calculate TMinus and date.
                holder.countdownStatus.setText("");
                holder.countdownStatus.setVisibility(View.GONE);
                if (launchItem.getNet().getTime() > 0) {
                    //TODO VERIFY THIS STILL WORKS
                    long longdate = launchItem.getNet().getTime();
                    final Date date = new Date(longdate);

                    Calendar launchDate = DateToCalendar(date);
                    Calendar now = rightNow;

                    now.setTimeInMillis(System.currentTimeMillis());
                    if (holder.timer != null) {
                        Timber.v("Timer is not null, cancelling.");
                        holder.timer.cancel();
                    }

                    if (holder.var != null){
                        holder.var.dispose();
                    }

                    final int status = launchItem.getStatus().getId();
                    final String hold = launchItem.getHoldreason();

                    long timeToFinish = launchDate.getTimeInMillis() - now.getTimeInMillis();

                    if (timeToFinish > 0 && launchItem.getStatus().getId() == 1) {
                        holder.timer = new CountDownTimer(timeToFinish, 1000) {
                            StringBuilder time = new StringBuilder();

                            @Override
                            public void onFinish() {
                                Timber.v("Countdown finished.");
                                holder.countdownDays.setText("00");
                                holder.countdownHours.setText("00");
                                holder.countdownMinutes.setText("00");
                                holder.countdownSeconds.setText("00");
                                holder.countdownStatus.setVisibility(View.VISIBLE);
                                holder.countdownStatus.setText("+");
                                countUpTimer(holder, longdate);
                            }

                            @Override
                            public void onTick(long millisUntilFinished) {
                                time.setLength(0);
                                setCountdownView(millisUntilFinished, holder);
                            }
                        }.start();
                    } else if (launchItem.getStatus().getId() == 3 || launchItem.getStatus().getId() == 4 || launchItem.getStatus().getId() == 7) {
                        holder.countdownDays.setText("00");
                        holder.countdownHours.setText("00");
                        holder.countdownMinutes.setText("00");
                        holder.countdownSeconds.setText("00");
                    } else if (launchItem.getStatus().getId() == 6 || launchItem.getStatus().getId() == 1) {
                        holder.countdownStatus.setVisibility(View.VISIBLE);
                        holder.countdownStatus.setText("+");
                        countUpTimer(holder, longdate);
                    } else {
                        holder.countdownDays.setText("- -");
                        holder.countdownHours.setText("- -");
                        holder.countdownMinutes.setText("- -");
                        holder.countdownSeconds.setText("- -");
                    }
                }

                //Get launch date
                if (launchItem.getStatus().getId() == 2) {

                    if (launchItem.getNet() != null) {
                        //Get launch date

                        SimpleDateFormat sdf = Utils.getSimpleDateFormatForUI("MMMM d, yyyy");
                        Date date = launchItem.getNet();
                        String launchTime = sdf.format(date);
                        if (launchItem.getTbddate()) {
                            launchTime = launchTime + " " + context.getString(R.string.unconfirmed);
                        }
                        holder.launchDateCompact.setText(launchTime);
                    }
                } else {
                    SimpleDateFormat sdf;
                    if (DateFormat.is24HourFormat(context)) {
                        sdf = Utils.getSimpleDateFormatForUI("MMMM d, yyyy HH:mm zzz");
                    } else {
                        sdf = Utils.getSimpleDateFormatForUI("MMMM d, yyyy h:mm a zzz");
                    }
                    sdf.toLocalizedPattern();
                    Date date = launchItem.getNet();
                    holder.launchDateCompact.setText(sdf.format(date));
                }

                if (launchItem.getVidURLs() != null) {
                    if (launchItem.getVidURLs().size() == 0) {
                        holder.watchButton.setVisibility(View.GONE);
                    } else {
                        holder.watchButton.setVisibility(View.VISIBLE);
                    }
                } else {
                    holder.watchButton.setVisibility(View.GONE);
                }


                if (launchItem.getMission() != null) {
                    holder.contentMission.setText(launchItem.getMission().getName());
                    String description = launchItem.getMission().getDescription();
                    if (description.length() > 0) {
                        holder.contentMissionDescription.setText(description);
                    }
                } else {
                    String[] separated = launchItem.getName().split(" \\| ");
                    try {
                        if (separated.length > 0 && separated[1].length() > 4) {
                            holder.contentMission.setText(separated[1].trim());
                        } else {
                            holder.contentMission.setText("Unknown Mission");
                        }
                    } catch (ArrayIndexOutOfBoundsException exception) {
                        holder.contentMission.setText("Unknown Mission");
                    }
                }

                //If pad and agency_menu exist add it to location, otherwise get whats always available
                if (launchItem.getPad().getLocation() != null) {
                    holder.location.setText(launchItem.getPad().getLocation().getName());
                } else {
                    holder.location.setText("");
                }
            }
        } catch (NullPointerException e) {
            Crashlytics.logException(e);
        }
    }

    private void countUpTimer(ViewHolder holder, long longdate) {
        holder.var = Observable
                .interval(1, TimeUnit.SECONDS, AndroidSchedulers.mainThread())
                .subscribe(
                        time -> {
                            Calendar currentTime = Calendar.getInstance();
                            long timeSince = currentTime.getTimeInMillis() - longdate;
                            setCountdownView(timeSince, holder);
                        });
    }

    private void setCountdownView(long millisUntilFinished, ViewHolder holder) {
        // Calculate the Days/Hours/Mins/Seconds numerically.
        long longDays = millisUntilFinished / 86400000;
        long longHours = (millisUntilFinished / 3600000) % 24;
        long longMins = (millisUntilFinished / 60000) % 60;
        long longSeconds = (millisUntilFinished / 1000) % 60;

        String days;
        String hours;
        String minutes;
        String seconds;

        if (longDays < 10) {
            days = "0" + String.valueOf(longDays);
        } else {
            days = String.valueOf(longDays);
        }


        // Translate those numerical values to string values.
        if (longHours < 10) {
            hours = "0" + String.valueOf(longHours);
        } else {
            hours = String.valueOf(longHours);
        }

        if (longMins < 10) {
            minutes = "0" + String.valueOf(longMins);
        } else {
            minutes = String.valueOf(longMins);
        }

        if (longSeconds < 10) {
            seconds = "0" + String.valueOf(longSeconds);
        } else {
            seconds = String.valueOf(longSeconds);
        }


        // Update the views
        if (Integer.valueOf(days) > 0) {
            holder.countdownDays.setText(days);
        } else {
            holder.countdownDays.setText("00");
        }

        if (Integer.valueOf(hours) > 0) {
            holder.countdownHours.setText(hours);
        } else if (Integer.valueOf(days) > 0) {
            holder.countdownHours.setText("00");
        } else {
            holder.countdownHours.setText("00");
        }

        if (Integer.valueOf(minutes) > 0) {
            holder.countdownMinutes.setText(minutes);
        } else if (Integer.valueOf(hours) > 0 || Integer.valueOf(days) > 0) {
            holder.countdownMinutes.setText("00");
        } else {
            holder.countdownMinutes.setText("00");
        }

        if (Integer.valueOf(seconds) > 0) {
            holder.countdownSeconds.setText(seconds);
        } else if (Integer.valueOf(minutes) > 0 || Integer.valueOf(hours) > 0 || Integer.valueOf(days) > 0) {
            holder.countdownSeconds.setText("00");
        } else {
            holder.countdownSeconds.setText("00");
        }
    }

    @Override
    public int getItemCount() {
        return launchList.size();
    }

    @Override
    public Object[] getSections() {
        return new Object[0];
    }

    @Override
    public int getPositionForSection(int sectionIndex) {
        return 0;
    }

    @Override
    public int getSectionForPosition(int position) {

        if (position >= getItemCount()) {
            position = getItemCount() - 1;
        }
        return 0;
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        @BindView(R.id.categoryIcon)
        ImageView categoryIcon;
        @BindView(R.id.launch_rocket)
        TextView title;
        @BindView(R.id.location)
        TextView location;
        @BindView(R.id.launch_date_compact)
        TextView launchDateCompact;
        @BindView(R.id.launch_image)
        ImageView launchImage;
        @BindView(R.id.countdown_days)
        TextView countdownDays;
        @BindView(R.id.countdown_hours)
        TextView countdownHours;
        @BindView(R.id.countdown_minutes)
        TextView countdownMinutes;
        @BindView(R.id.countdown_seconds)
        TextView countdownSeconds;
        @BindView(R.id.countdown_status)
        TextView countdownStatus;
        @BindView(R.id.status)
        TextView status;
        @BindView(R.id.watchButton)
        Button watchButton;
        @BindView(R.id.shareButton)
        Button shareButton;
        @BindView(R.id.exploreButton)
        Button exploreButton;
        @BindView(R.id.content_mission)
        TextView contentMission;
        @BindView(R.id.content_mission_description)
        TextView contentMissionDescription;
        @BindView(R.id.landing_icon)
        ImageView landingIcon;
        @BindView(R.id.landing)
        TextView landing;
        @BindView(R.id.landing_pill)
        View landingView;
        @BindView(R.id.status_pill)
        CardView statusPill;
        public View layout;
        public CountDownTimer timer;
        public Disposable var;

        //Add content to the card
        public ViewHolder(View view) {
            super(view);
            layout = view;
            ButterKnife.bind(this, view);

            shareButton.setOnClickListener(this);
            exploreButton.setOnClickListener(this);
            watchButton.setOnClickListener(this);
        }

        //React to click events.
        @Override
        public void onClick(View v) {
            final int position = getAdapterPosition();
            Timber.d("onClick at %s", position);

            final Launch launch = launchList.get(position);
            Intent sendIntent = new Intent();

            SimpleDateFormat df = Utils.getSimpleDateFormatForUI("EEEE, MMMM d, yyyy - hh:mm a zzz");
            df.toLocalizedPattern();

            Date date = launch.getNet();
            String launchDate = df.format(date);

            switch (v.getId()) {
                case R.id.watchButton:
                    Timber.d("Watch: %s", launch.getVidURLs().size());
                    Analytics.getInstance().sendButtonClicked("Watch Button - Opening Dialogue");
                    if (launch.getVidURLs().size() > 0) {
                        final DialogAdapter adapter = new DialogAdapter((index, item, longClick) -> {
                            try {
                                if (longClick) {
                                    Intent sendIntent1 = new Intent();
                                    sendIntent1.setAction(Intent.ACTION_SEND);
                                    sendIntent1.putExtra(Intent.EXTRA_TEXT, launch.getVidURLs().get(index).getVal()); // Simple text and URL to share
                                    sendIntent1.setType("text/plain");
                                    context.startActivity(sendIntent1);
                                    Analytics.getInstance().sendButtonClickedWithURL("Watch Button - URL Long Clicked", launch.getVidURLs().get(index).getVal());
                                } else {
                                    Uri watchUri = Uri.parse(launch.getVidURLs().get(index).getVal());
                                    Intent i = new Intent(Intent.ACTION_VIEW, watchUri);
                                    context.startActivity(i);
                                    Analytics.getInstance().sendButtonClickedWithURL("Watch Button - URL", launch.getVidURLs().get(index).getVal());
                                }
                            } catch (ArrayIndexOutOfBoundsException e) {
                                Timber.e(e);
                                Toast.makeText(context, "Ops, an error occurred.", Toast.LENGTH_SHORT).show();
                            }
                        });
                        for (RealmStr s : launch.getVidURLs()) {
                            //Do your stuff here
                            adapter.add(new MaterialSimpleListItem.Builder(context)
                                    .content(s.getVal())
                                    .build());
                        }

                        MaterialDialog.Builder builder = new MaterialDialog.Builder(context)
                                .title("Select a Source")
                                .content("Long press for additional options.")
                                .adapter(adapter, null)
                                .negativeText("Cancel");
                        builder.show();
                    }
                    break;
                case R.id.exploreButton:
                    Timber.d("Explore: %s", launchList.get(position).getId());
                    Analytics.getInstance().sendButtonClicked("Explore Button", launch.getName());

                    Intent exploreIntent = new Intent(context, LaunchDetailActivity.class);
                    exploreIntent.putExtra("TYPE", "launch");
                    exploreIntent.putExtra("launchID", launch.getId());
                    context.startActivity(exploreIntent);
                    break;
                case R.id.shareButton:
                    String message;
                    if (launch.getVidURLs().size() > 0) {
                        if (launch.getPad().getLocation() != null) {

                            message = launch.getName() + " launching from "
                                    + launch.getPad().getLocation().getName() + "\n\n"
                                    + launchDate;
                        } else if (launch.getPad().getLocation() != null) {
                            message = launch.getName() + " launching from "
                                    + launch.getPad().getLocation().getName() + "\n\n"
                                    + launchDate;
                        } else {
                            message = launch.getName()
                                    + "\n\n"
                                    + launchDate;
                        }
                    } else {
                        if (launch.getPad().getLocation() != null) {

                            message = launch.getName() + " launching from "
                                    + launch.getPad().getLocation().getName() + "\n\n"
                                    + launchDate;
                        } else if (launch.getPad().getLocation() != null) {
                            message = launch.getName() + " launching from "
                                    + launch.getPad().getLocation().getName() + "\n\n"
                                    + launchDate;
                        } else {
                            message = launch.getName()
                                    + "\n\n"
                                    + launchDate;
                        }
                    }
                    ShareCompat.IntentBuilder.from((Activity) context)
                            .setType("text/plain")
                            .setChooserTitle("Share: " + launch.getName())
                            .setText(String.format("%s\n\nWatch Live: %s", message, launch.getUrl()))
                            .startChooser();
                    Analytics.getInstance().sendLaunchShared("Explore Button", launch.getName() + "-" + launch.getId().toString());
                    break;
                case R.id.TitleCard:
                    Timber.d("Explore: %s", launchList.get(position).getId());
                    Analytics.getInstance().sendButtonClicked("Title Card", launch.getName());

                    Intent titleIntent = new Intent(context, LaunchDetailActivity.class);
                    titleIntent.putExtra("TYPE", "launch");
                    titleIntent.putExtra("launchID", launch.getId());
                    context.startActivity(titleIntent);
                    break;
            }
        }
    }
}
