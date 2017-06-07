package me.calebjones.spacelaunchnow.content.services;

import android.app.IntentService;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.realm.Realm;
import io.realm.RealmQuery;
import io.realm.RealmResults;
import io.realm.Sort;
import me.calebjones.spacelaunchnow.calendar.CalendarSyncService;
import me.calebjones.spacelaunchnow.content.database.SwitchPreferences;
import me.calebjones.spacelaunchnow.content.jobs.NextLaunchJob;
import me.calebjones.spacelaunchnow.content.jobs.SyncJob;
import me.calebjones.spacelaunchnow.content.util.NotificationBuilder;
import me.calebjones.spacelaunchnow.data.models.Launch;
import me.calebjones.spacelaunchnow.data.models.LaunchNotification;
import me.calebjones.spacelaunchnow.widget.LaunchCardCompactWidgetProvider;
import me.calebjones.spacelaunchnow.widget.LaunchTimerWidgetProvider;
import me.calebjones.spacelaunchnow.widget.LaunchWordTimerWidgetProvider;
import timber.log.Timber;

public class NextLaunchTracker extends IntentService {

    private SharedPreferences sharedPref;
    private SwitchPreferences switchPreferences;
    private Calendar rightNow;
    private RealmResults<Launch> launchRealms;
    private long interval;
    private Realm realm;

public NextLaunchTracker() {
        super("NextLaunchTracker");
    }

    public void onCreate() {
        super.onCreate();
        Timber.d("NextLaunchTracker - onCreate");
        rightNow = Calendar.getInstance();
        super.onCreate();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Timber.d("onHandleIntent - %s", intent.describeContents());
        realm = Realm.getDefaultInstance();
        this.switchPreferences = SwitchPreferences.getInstance(getApplicationContext());
        this.sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

        Calendar calDay = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        calDay.add(Calendar.HOUR, 72);
        Date date = new Date();
        Date dateDay = calDay.getTime();

        if (switchPreferences.getAllSwitch()) {
            RealmQuery<Launch> query = realm.where(Launch.class)
                    .between("net", date, dateDay);
            if (switchPreferences.getNoGoSwitch()) {
                query.equalTo("status", 1);
            }
            launchRealms = query.findAllSorted("net", Sort.ASCENDING);
        } else {
            filterLaunchRealm(date, dateDay, realm);
        }

        if (launchRealms.size() > 0) {
            for (Launch realm : launchRealms) {
                checkNextLaunches(realm);
            }
        } else {

            //If Calendar Sync is enabled sync it up
            if (switchPreferences.getCalendarStatus()) {
                syncCalendar();
            }
        }
        SyncJob.schedulePeriodicJob(this);
        updateWidgets();
        realm.close();
    }

    private void updateWidgets() {
        Intent cardIntent = new Intent(this, LaunchCardCompactWidgetProvider.class);
        cardIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        int cardIds[] = AppWidgetManager.getInstance(getApplication()).getAppWidgetIds(new ComponentName(getApplication(), LaunchCardCompactWidgetProvider.class));
        cardIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, cardIds);
        sendBroadcast(cardIntent);

        Intent timerIntent = new Intent(this, LaunchTimerWidgetProvider.class);
        timerIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        int timerIds[] = AppWidgetManager.getInstance(getApplication()).getAppWidgetIds(new ComponentName(getApplication(), LaunchTimerWidgetProvider.class));
        timerIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, timerIds);
        sendBroadcast(timerIntent);

        Intent wordIntent = new Intent(this, LaunchWordTimerWidgetProvider.class);
        wordIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        int wordIds[] = AppWidgetManager.getInstance(getApplication()).getAppWidgetIds(new ComponentName(getApplication(), LaunchWordTimerWidgetProvider.class));
        wordIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, wordIds);
        sendBroadcast(wordIntent);
    }

    private void filterLaunchRealm(Date date, Date dateDay, Realm realm) {
        boolean first = true;
        RealmQuery<Launch> query = realm.where(Launch.class)
                .between("net", date, dateDay);

        if (switchPreferences.getNoGoSwitch()) {
            query.equalTo("status", 1).findAll();
        }

        query.beginGroup();

        if (switchPreferences.getSwitchNasa()) {
            first = false;
            query.equalTo("rocket.agencies.id", 44)
                    .or()
                    .equalTo("location.pads.agencies.id", 44);
        }

        if (switchPreferences.getSwitchArianespace()) {
            if (!first) {
                query.or();
            } else {
                first = false;
            }
            query.equalTo("rocket.agencies.id", 115)
                    .or()
                    .equalTo("location.pads.agencies.id", 115);
        }

        if (switchPreferences.getSwitchSpaceX()) {
            if (!first) {
                query.or();
            } else {
                first = false;
            }
            query.equalTo("rocket.agencies.id", 121)
                    .or()
                    .equalTo("location.pads.agencies.id", 121);
        }

        if (switchPreferences.getSwitchULA()) {
            if (!first) {
                query.or();
            } else {
                first = false;
            }
            query.equalTo("rocket.agencies.id", 124)
                    .or()
                    .equalTo("location.pads.agencies.id", 124);
        }

        if (switchPreferences.getSwitchRoscosmos()) {
            if (!first) {
                query.or();
            } else {
                first = false;
            }
            query.equalTo("rocket.agencies.id", 111)
                    .or()
                    .equalTo("location.pads.agencies.id", 111)
                    .or()
                    .equalTo("rocket.agencies.id", 163)
                    .or()
                    .equalTo("location.pads.agencies.id", 163)
                    .or()
                    .equalTo("rocket.agencies.id", 63)
                    .or()
                    .equalTo("location.pads.agencies.id", 63);
        }
        if (switchPreferences.getSwitchCASC()) {
            if (!first) {
                query.or();
            } else {
                first = false;
            }
            query.equalTo("rocket.agencies.id", 88)
                    .or()
                    .equalTo("location.pads.agencies.id", 88);
        }

        if (switchPreferences.getSwitchISRO()) {
            if (!first) {
                query.or();
            } else {
                first = false;
            }
            query.equalTo("rocket.agencies.id", 31)
                    .or()
                    .equalTo("location.pads.agencies.id", 31);
        }

        if (switchPreferences.getSwitchKSC()) {
            if (!first) {
                query.or();
            } else {
                first = false;
            }
            query.equalTo("location.id", 17);
        }

        if (switchPreferences.getSwitchCape()) {
            if (!first) {
                query.or();
            } else {
                first = false;
            }
            query.equalTo("location.id", 16);
        }

        if (switchPreferences.getSwitchPles()) {
            if (!first) {
                query.or();
            } else {
                first = false;
            }
            query.equalTo("location.id", 11);
        }

        if (switchPreferences.getSwitchVan()) {
            if (!first) {
                query.or();
            }
            query.equalTo("location.id", 18);
        }

        launchRealms = query.endGroup().findAllSorted("net", Sort.ASCENDING);
    }

    private void checkNextLaunches(Launch launch) {
        Timber.i("Checking launch - %s", launch.getName());
        if (launch != null) {
            checkStatus(launch);
        }

        if (switchPreferences.getCalendarStatus()) {
            syncCalendar();
        }

        scheduleWear();
    }

    private void syncCalendar() {
        CalendarSyncService.startActionSyncAll(this);
    }

    private void checkStatus(final Launch launch) {
        if (launch != null && launch.getNetstamp() != null && launch.getNetstamp() > 0) {

            LaunchNotification notification = realm.where(LaunchNotification.class).equalTo("id", launch.getId()).findFirst();
            if (notification == null) {
                realm.executeTransaction(new Realm.Transaction() {
                    @Override
                    public void execute(Realm realm) {
                        realm.createObject(LaunchNotification.class, launch.getId());
                    }
                });
                notification = realm.where(LaunchNotification.class).equalTo("id", launch.getId()).findFirst();
            }

            long longdate = launch.getNetstamp();
            longdate = longdate * 1000;
            final Date date = new Date(longdate);

            Calendar future = DateToCalendar(date);
            Calendar now = rightNow;

            now.setTimeInMillis(System.currentTimeMillis());
            long timeToFinish = future.getTimeInMillis() - now.getTimeInMillis();
            boolean notify = (this.sharedPref.getBoolean("notifications_new_message", true) && launch.isNotifiable());

            //nextLaunch is in less then one hour
            if (timeToFinish > 0) {
                if (timeToFinish <= 600000) {
                    if (notify) {
                        //Check settings to see if user should be notified.
                        if (!notification.isNotifiedTenMinute() && this.sharedPref.getBoolean("notifications_launch_minute", false)) {
                            NotificationBuilder.notifyUser(this, launch, timeToFinish);
                            realm.beginTransaction();
                            notification.setNotifiedTenMinute(true);
                            realm.commitTransaction();
                        } else if (!notification.isNotifiedHour() && this.sharedPref.getBoolean("notifications_launch_imminent", true)) {
                            NotificationBuilder.notifyUser(this, launch, timeToFinish);
                            realm.beginTransaction();
                            notification.setNotifiedHour(true);
                            realm.commitTransaction();
                        }
                    }
                    NextLaunchJob.scheduleIntervalJob((future.getTimeInMillis() - 60000) - now.getTimeInMillis(), launch.getId());
                } else if (timeToFinish < 3600000) {
                    if (notify) {
                        //Check settings to see if user should be notified.
                        if (this.sharedPref.getBoolean("notifications_launch_imminent", true)) {
                            if (!notification.isNotifiedHour()) {
                                NotificationBuilder.notifyUser(this, launch, timeToFinish);
                                realm.beginTransaction();
                                notification.setNotifiedHour(true);
                                realm.commitTransaction();
                            }
                        }
                    }
                    NextLaunchJob.scheduleIntervalJob((future.getTimeInMillis() - 600000) - now.getTimeInMillis(), launch.getId());
                    //Launch is in less then 24 hours
                } else if (timeToFinish < 86400000) {
                    Timber.v("Less than 24 hours.");
                    if (notify) {
                        //Check settings to see if user should be notified.
                        if (this.sharedPref.getBoolean("notifications_launch_day", true)) {
                            if (!notification.isNotifiedDay()) {
                                NotificationBuilder.notifyUser(this, launch, timeToFinish);
                                realm.beginTransaction();
                                notification.setNotifiedDay(true);
                                realm.commitTransaction();
                            }
                        }
                    }
                    interval = timeToFinish - 3601000;
//                    if (interval < 3600000) {
//                        interval = 3500000;
//                    }
                    NextLaunchJob.scheduleIntervalJob(interval, launch.getId());
                    //Launch is within 48 hours
                } else if (timeToFinish < 172800000) {
                    NextLaunchJob.scheduleIntervalJob((future.getTimeInMillis() - 86400000) - now.getTimeInMillis(), launch.getId());
                } else {
                    NextLaunchJob.scheduleIntervalJob((timeToFinish / 2) + 43200000, launch.getId());
                }
            }
        } else {
            //Get sync period.
            String notificationTimer = this.sharedPref.getString("notification_sync_time", "24");

            Pattern p = Pattern.compile("(\\d+)");
            Matcher m = p.matcher(notificationTimer);

            if (m.matches()) {
                int hrs = Integer.parseInt(m.group(1));
                interval = (long) hrs * 60 * 60 * 1000;
                NextLaunchJob.scheduleIntervalJob(interval, launch.getId());
            }
        }
    }

    public static Calendar DateToCalendar(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return cal;
    }

    public void scheduleWear() {
        this.startService(new Intent(this, UpdateWearService.class));
    }

}
