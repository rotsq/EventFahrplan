package nerd.tuxmobil.fahrplan.congress.autoupdate;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.SafeJobIntentService;

import java.util.List;

import kotlin.Unit;
import nerd.tuxmobil.fahrplan.congress.MyApp;
import nerd.tuxmobil.fahrplan.congress.MyApp.TASKS;
import nerd.tuxmobil.fahrplan.congress.R;
import nerd.tuxmobil.fahrplan.congress.models.Session;
import nerd.tuxmobil.fahrplan.congress.net.ConnectivityObserver;
import nerd.tuxmobil.fahrplan.congress.net.FetchScheduleResult;
import nerd.tuxmobil.fahrplan.congress.net.HttpStatus;
import nerd.tuxmobil.fahrplan.congress.net.LoadShiftsResult;
import nerd.tuxmobil.fahrplan.congress.net.ParseResult;
import nerd.tuxmobil.fahrplan.congress.net.ParseScheduleResult;
import nerd.tuxmobil.fahrplan.congress.net.ParseShiftsResult;
import nerd.tuxmobil.fahrplan.congress.notifications.NotificationHelper;
import nerd.tuxmobil.fahrplan.congress.repositories.AppRepository;
import nerd.tuxmobil.fahrplan.congress.schedule.MainActivity;
import nerd.tuxmobil.fahrplan.congress.utils.FahrplanMisc;

public class UpdateService extends SafeJobIntentService {

    private static final int JOB_ID = 2119;

    private static final String LOG_TAG = "UpdateService";

    @SuppressWarnings("squid:S1170")
    private final AppRepository appRepository = AppRepository.INSTANCE;

    public void onParseDone(@NonNull ParseResult result) {
        int numDays = appRepository.readMeta().getNumDays();
        MyApp.LogDebug(LOG_TAG, "parseDone: " + result.isSuccess() + " , numDays=" + numDays);
        MyApp.task_running = TASKS.NONE;
        List<Session> changesList = appRepository.loadChangedSessions();
        if (!changesList.isEmpty() && result instanceof ParseScheduleResult) {
            showScheduleUpdateNotification(((ParseScheduleResult) result).getVersion(), changesList.size());
        }
        MyApp.LogDebug(LOG_TAG, "background update complete");
        stopSelf();
    }

    private void showScheduleUpdateNotification(String version, int changesCount) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        PendingIntent contentIntent = PendingIntent
                .getActivity(this, 0, notificationIntent, PendingIntent.FLAG_ONE_SHOT);

        String contentText;
        if (TextUtils.isEmpty(version)) {
            contentText = getString(R.string.schedule_updated);
        } else {
            contentText = getString(R.string.schedule_updated_to, version);
        }

        Uri soundUri = appRepository.readAlarmToneUri();

        NotificationHelper notificationHelper = new NotificationHelper(this);
        NotificationCompat.Builder builder = notificationHelper.getScheduleUpdateNotificationBuilder(contentIntent, contentText, changesCount, soundUri);
        notificationHelper.notify(NotificationHelper.SCHEDULE_UPDATE_ID, builder);
    }

    public void onGotResponse(@NonNull FetchScheduleResult fetchScheduleResult) {
        HttpStatus status = fetchScheduleResult.getHttpStatus();
        MyApp.task_running = TASKS.NONE;
        if (status != HttpStatus.HTTP_OK) {
            MyApp.LogDebug(LOG_TAG, "Background schedule update failed. HTTP status code: " + status);
            stopSelf();
            return;
        }

        // Parser is automatically invoked when response has been received.
        MyApp.task_running = TASKS.PARSE;
    }

    private void onLoadShiftsDone(@NonNull LoadShiftsResult result) {
        onParseDone(ParseShiftsResult.of(result));
    }

    private void fetchFahrplan() {
        if (MyApp.task_running == TASKS.NONE) {
            MyApp.task_running = TASKS.FETCH;
            String url = appRepository.readScheduleUrl();
            appRepository.loadSchedule(url,
                    fetchScheduleResult -> {
                        onGotResponse(fetchScheduleResult);
                        return Unit.INSTANCE;
                    },
                    parseScheduleResult -> {
                        onParseDone(parseScheduleResult);
                        return Unit.INSTANCE;
                    },
                    loadShiftsResult -> {
                        onLoadShiftsDone(loadShiftsResult);
                        return Unit.INSTANCE;
                    });
        } else {
            MyApp.LogDebug(LOG_TAG, "Fetching already in progress.");
        }
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        ConnectivityObserver connectivityObserver = new ConnectivityObserver(this, () -> {
            MyApp.LogDebug(LOG_TAG, "Network is available");
            fetchSchedule();
            return Unit.INSTANCE;
        }, () -> {
            MyApp.LogDebug(LOG_TAG, "Network is not available");
            stopSelf();
            return Unit.INSTANCE;
        }, true);
        connectivityObserver.start();
    }

    @Override
    public void onDestroy() {
        appRepository.cancelLoading();
        super.onDestroy();
    }

    private void fetchSchedule() {
        MyApp.LogDebug(LOG_TAG, "Fetching schedule ...");
        FahrplanMisc.setUpdateAlarm(this, false);
        fetchFahrplan();
    }

    public static void start(@NonNull Context context) {
        UpdateService.enqueueWork(context, UpdateService.class, JOB_ID, new Intent());
    }

}
