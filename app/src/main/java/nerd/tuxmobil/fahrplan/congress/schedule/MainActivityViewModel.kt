package nerd.tuxmobil.fahrplan.congress.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import info.metadude.android.eventfahrplan.commons.livedata.SingleLiveEvent
import info.metadude.android.eventfahrplan.commons.logging.Logging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import nerd.tuxmobil.fahrplan.congress.R
import nerd.tuxmobil.fahrplan.congress.changes.ChangeStatistic
import nerd.tuxmobil.fahrplan.congress.models.Meta
import nerd.tuxmobil.fahrplan.congress.net.ParseResult
import nerd.tuxmobil.fahrplan.congress.repositories.AppRepository
import nerd.tuxmobil.fahrplan.congress.repositories.ExecutionContext
import nerd.tuxmobil.fahrplan.congress.repositories.LoadScheduleStatus.FetchFailure
import nerd.tuxmobil.fahrplan.congress.repositories.LoadScheduleStatus.FetchSuccess
import nerd.tuxmobil.fahrplan.congress.repositories.LoadScheduleStatus.Fetching
import nerd.tuxmobil.fahrplan.congress.repositories.LoadScheduleStatus.InitialFetching
import nerd.tuxmobil.fahrplan.congress.repositories.LoadScheduleStatus.InitialParsing
import nerd.tuxmobil.fahrplan.congress.repositories.LoadScheduleStatus.ParseFailure
import nerd.tuxmobil.fahrplan.congress.repositories.LoadScheduleStatus.ParseSuccess
import nerd.tuxmobil.fahrplan.congress.repositories.LoadScheduleStatus.Parsing
import nerd.tuxmobil.fahrplan.congress.schedule.observables.ScheduleChangesParameter

class MainActivityViewModel(

    private val repository: AppRepository,
    private val executionContext: ExecutionContext,
    private val logging: Logging

) : ViewModel() {

    private companion object {
        const val LOG_TAG = "MainActivityViewModel"
    }

    val toggleProgressInfo = SingleLiveEvent<Int?>()
    val toggleProgressIndicator = SingleLiveEvent<Boolean>()
    val showFetchFailureInfo = SingleLiveEvent<FetchFailure>()
    val showParseFailureInfo = SingleLiveEvent<ParseResult>()
    val scheduleChangesParameter = SingleLiveEvent<ScheduleChangesParameter>()
    val showAbout = SingleLiveEvent<Meta>()
    val openSessionDetails = SingleLiveEvent<Unit>()

    init {
        observeLoadScheduleStatus()
    }

    private fun observeLoadScheduleStatus() {
        launch {
            repository.loadScheduleStatus.collect { status ->
                logging.e(LOG_TAG, ">> $status")
                when (status) {
                    InitialFetching ->
                        toggleProgressInfo.postValue(R.string.progress_loading_data)
                    Fetching ->
                        toggleProgressIndicator.postValue(true)
                    FetchSuccess -> {
                        toggleProgressInfo.postValue(null)
                        toggleProgressIndicator.postValue(false)
                    }
                    is FetchFailure -> {
                        toggleProgressInfo.postValue(null)
                        toggleProgressIndicator.postValue(false)
                        if (status.isUserRequest) {
                            // Don't bother the user with schedule up-to-date messages.
                            showFetchFailureInfo.postValue(status)
                        }
                    }

                    InitialParsing -> {
                        toggleProgressInfo.postValue(R.string.progress_processing_data)
                    }
                    Parsing -> {
                        toggleProgressIndicator.postValue(true)
                    }
                    ParseSuccess -> {
                        toggleProgressInfo.postValue(null)
                        toggleProgressIndicator.postValue(false)
                        onParsingDone()
                    }
                    is ParseFailure -> {
                        toggleProgressInfo.postValue(null)
                        toggleProgressIndicator.postValue(false)
                        showParseFailureInfo.postValue(status.parseResult)
                    }
                }
            }
        }
    }

    private fun onParsingDone() {
        if (!repository.readScheduleChangesSeen()) {
            val scheduleVersion = repository.readMeta().version
            val sessions = repository.loadChangedSessions()
            val statistic = ChangeStatistic.of(sessions, logging)
            val parameter = ScheduleChangesParameter(scheduleVersion, statistic)
            scheduleChangesParameter.postValue(parameter)
        }
    }

    /**
     * Requests loading the schedule from the [AppRepository] to update the UI. UI components must
     * observe the respective properties exposed by the [AppRepository] to receive schedule updates.
     * The [isUserRequest] must be set to `true` if the requests originates from a manual
     * interaction of the user with the UI; otherwise `false`.
     */
    fun requestScheduleUpdate(isUserRequest: Boolean) {
        // TODO Remove zombie callbacks when cleaning up AppRepository#loadSchedule
        launch {
            repository.loadSchedule(
                isUserRequest = isUserRequest,
                onFetchingDone = {},
                onParsingDone = {},
                onLoadingShiftsDone = {}
            )
        }
    }

    fun cancelLoading() {
        // AppRepository wraps the call in a CoroutineScope itself.
        repository.cancelLoading()
    }

    fun deleteSessionAlarmNotificationId(notificationId: Int) {
        launch {
            repository.deleteSessionAlarmNotificationId(notificationId)
        }
    }

    fun showAboutDialog() {
        launch {
            val meta = repository.readMeta()
            showAbout.postValue(meta)
        }
    }

    fun openSessionDetails(sessionId: String) {
        launch {
            val isUpdated = repository.updateSelectedSessionId(sessionId)
            if (isUpdated) {
                openSessionDetails.postValue(Unit)
            }
        }
    }

    private fun launch(block: suspend CoroutineScope.() -> Unit) {
        viewModelScope.launch(executionContext.database, block = block)
    }

}
