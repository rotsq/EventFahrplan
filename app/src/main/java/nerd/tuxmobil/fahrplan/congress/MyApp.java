package nerd.tuxmobil.fahrplan.congress;

import android.app.Application;
import android.util.Log;

import androidx.annotation.CallSuper;

import org.ligi.tracedroid.TraceDroid;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import info.metadude.android.eventfahrplan.commons.logging.Logging;
import nerd.tuxmobil.fahrplan.congress.repositories.AppRepository;
import nerd.tuxmobil.fahrplan.congress.utils.ConferenceTimeFrame;

public class MyApp extends Application {

    public static final boolean DEBUG = false;

    private static final long FIRST_DAY_START = getMilliseconds("Europe/Paris",
            BuildConfig.SCHEDULE_FIRST_DAY_START_YEAR,
            BuildConfig.SCHEDULE_FIRST_DAY_START_MONTH,
            BuildConfig.SCHEDULE_FIRST_DAY_START_DAY);

    private static final long LAST_DAY_END = getMilliseconds("Europe/Paris",
            BuildConfig.SCHEDULE_LAST_DAY_END_YEAR,
            BuildConfig.SCHEDULE_LAST_DAY_END_MONTH,
            BuildConfig.SCHEDULE_LAST_DAY_END_DAY);

    public static final ConferenceTimeFrame conferenceTimeFrame =
            new ConferenceTimeFrame(FIRST_DAY_START, LAST_DAY_END);

    public enum TASKS {
        NONE,
        FETCH,
        PARSE,
        FETCH_CANCELLED
    }

    public static TASKS task_running = TASKS.NONE;

    @CallSuper
    @Override
    public void onCreate() {
        super.onCreate();
        TraceDroid.init(this);
        task_running = TASKS.NONE;
        AppRepository.INSTANCE.initialize(
                getApplicationContext(),
                Logging.get()
        );
    }

    private static long getMilliseconds(String timeZoneId, int year, int month, int day) {
        TimeZone zone = TimeZone.getTimeZone(timeZoneId);
        Calendar calendar = new GregorianCalendar(zone);
        int zeroBasedMonth = month - 1;
        calendar.set(year, zeroBasedMonth, day, 0, 0, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    public static void LogDebug(String tag, String message) {
        if (DEBUG) {
            Log.d(tag, message);
        }
    }
}
