package nerd.tuxmobil.fahrplan.congress.schedule

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import info.metadude.android.eventfahrplan.commons.testing.MainDispatcherTestRule
import info.metadude.android.eventfahrplan.commons.testing.assertLiveData
import info.metadude.android.eventfahrplan.commons.testing.createCoroutineScope
import info.metadude.android.eventfahrplan.commons.testing.sharedFlowOf
import info.metadude.android.eventfahrplan.commons.testing.verifyInvokedOnce
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharedFlow
import nerd.tuxmobil.fahrplan.congress.NoLogging
import nerd.tuxmobil.fahrplan.congress.R
import nerd.tuxmobil.fahrplan.congress.TestExecutionContext
import nerd.tuxmobil.fahrplan.congress.changes.ChangeStatistic
import nerd.tuxmobil.fahrplan.congress.models.Meta
import nerd.tuxmobil.fahrplan.congress.models.Session
import nerd.tuxmobil.fahrplan.congress.net.HttpStatus
import nerd.tuxmobil.fahrplan.congress.net.ParseResult
import nerd.tuxmobil.fahrplan.congress.repositories.AppRepository
import nerd.tuxmobil.fahrplan.congress.repositories.LoadScheduleStatus
import nerd.tuxmobil.fahrplan.congress.repositories.LoadScheduleStatus.FetchFailure
import nerd.tuxmobil.fahrplan.congress.repositories.LoadScheduleStatus.FetchSuccess
import nerd.tuxmobil.fahrplan.congress.repositories.LoadScheduleStatus.Fetching
import nerd.tuxmobil.fahrplan.congress.repositories.LoadScheduleStatus.InitialFetching
import nerd.tuxmobil.fahrplan.congress.repositories.LoadScheduleStatus.InitialParsing
import nerd.tuxmobil.fahrplan.congress.repositories.LoadScheduleStatus.ParseFailure
import nerd.tuxmobil.fahrplan.congress.repositories.LoadScheduleStatus.ParseSuccess
import nerd.tuxmobil.fahrplan.congress.repositories.LoadScheduleStatus.Parsing
import nerd.tuxmobil.fahrplan.congress.schedule.observables.ScheduleChangesParameter
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class MainActivityViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainDispatcherTestRule = MainDispatcherTestRule()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val coroutineScope = mainDispatcherTestRule.createCoroutineScope()

    private val logging = NoLogging

    @Test
    fun `initialization does not affect properties`() {
        val repository = createRepository()
        val viewModel = createViewModel(repository)
        assertLiveData(viewModel.toggleProgressInfo).isEqualTo(null)
        assertLiveData(viewModel.toggleProgressIndicator).isEqualTo(null)
        assertLiveData(viewModel.showFetchFailureInfo).isEqualTo(null)
        assertLiveData(viewModel.showParseFailureInfo).isEqualTo(null)
        assertLiveData(viewModel.scheduleChangesParameter).isEqualTo(null)
        verifyInvokedOnce(repository).loadScheduleStatus
    }

    @Test
    fun `InitialFetching posts message to toggleProgressInfo property`() {
        val repository = createRepository(loadScheduleStatusFlow = sharedFlowOf(InitialFetching))
        val viewModel = createViewModel(repository)
        assertLiveData(viewModel.toggleProgressInfo).isEqualTo(R.string.progress_loading_data)
        assertLiveData(viewModel.toggleProgressIndicator).isEqualTo(null)
        assertLiveData(viewModel.showFetchFailureInfo).isEqualTo(null)
        assertLiveData(viewModel.showParseFailureInfo).isEqualTo(null)
        assertLiveData(viewModel.scheduleChangesParameter).isEqualTo(null)
        verifyInvokedOnce(repository).loadScheduleStatus
    }

    @Test
    fun `Fetching posts true to toggleProgressIndicator property`() {
        val repository = createRepository(loadScheduleStatusFlow = sharedFlowOf(Fetching))
        val viewModel = createViewModel(repository)
        assertLiveData(viewModel.toggleProgressInfo).isEqualTo(null)
        assertLiveData(viewModel.toggleProgressIndicator).isEqualTo(true)
        assertLiveData(viewModel.showFetchFailureInfo).isEqualTo(null)
        assertLiveData(viewModel.showParseFailureInfo).isEqualTo(null)
        assertLiveData(viewModel.scheduleChangesParameter).isEqualTo(null)
        verifyInvokedOnce(repository).loadScheduleStatus
    }

    @Test
    fun `FetchSuccess posts false to toggleProgressIndicator property`() {
        val repository = createRepository(loadScheduleStatusFlow = sharedFlowOf(FetchSuccess))
        val viewModel = createViewModel(repository)
        assertLiveData(viewModel.toggleProgressInfo).isEqualTo(null)
        assertLiveData(viewModel.toggleProgressIndicator).isEqualTo(false)
        assertLiveData(viewModel.showFetchFailureInfo).isEqualTo(null)
        assertLiveData(viewModel.showParseFailureInfo).isEqualTo(null)
        assertLiveData(viewModel.scheduleChangesParameter).isEqualTo(null)
        verifyInvokedOnce(repository).loadScheduleStatus
    }

    @Test
    fun `FetchFailure posts false to toggleProgressIndicator and status to showFetchFailureInfo properties`() {
        val status = FetchFailure(HttpStatus.HTTP_DNS_FAILURE, "localhost", "some-error", isUserRequest = true)
        val repository = createRepository(loadScheduleStatusFlow = sharedFlowOf(status))
        val viewModel = createViewModel(repository)
        assertLiveData(viewModel.toggleProgressInfo).isEqualTo(null)
        assertLiveData(viewModel.toggleProgressIndicator).isEqualTo(false)
        val expectedFailure = FetchFailure(HttpStatus.HTTP_DNS_FAILURE, "localhost", "some-error", isUserRequest = true)
        assertLiveData(viewModel.showFetchFailureInfo).isEqualTo(expectedFailure)
        assertLiveData(viewModel.showParseFailureInfo).isEqualTo(null)
        assertLiveData(viewModel.scheduleChangesParameter).isEqualTo(null)
        verifyInvokedOnce(repository).loadScheduleStatus
    }

    @Test
    fun `FetchFailure posts false to toggleProgressIndicator but no status to showFetchFailureInfo properties`() {
        val status = FetchFailure(HttpStatus.HTTP_DNS_FAILURE, "localhost", "some-error", isUserRequest = false)
        val repository = createRepository(loadScheduleStatusFlow = sharedFlowOf(status))
        val viewModel = createViewModel(repository)
        assertLiveData(viewModel.toggleProgressInfo).isEqualTo(null)
        assertLiveData(viewModel.toggleProgressIndicator).isEqualTo(false)
        assertLiveData(viewModel.showFetchFailureInfo).isEqualTo(null)
        assertLiveData(viewModel.showParseFailureInfo).isEqualTo(null)
        assertLiveData(viewModel.scheduleChangesParameter).isEqualTo(null)
        verifyInvokedOnce(repository).loadScheduleStatus
    }

    @Test
    fun `InitialParsing posts message to toggleProgressInfo property`() {
        val repository = createRepository(loadScheduleStatusFlow = sharedFlowOf(InitialParsing))
        val viewModel = createViewModel(repository)
        assertLiveData(viewModel.toggleProgressInfo).isEqualTo(R.string.progress_processing_data)
        assertLiveData(viewModel.toggleProgressIndicator).isEqualTo(null)
        assertLiveData(viewModel.showFetchFailureInfo).isEqualTo(null)
        assertLiveData(viewModel.showParseFailureInfo).isEqualTo(null)
        assertLiveData(viewModel.scheduleChangesParameter).isEqualTo(null)
        verifyInvokedOnce(repository).loadScheduleStatus
    }

    @Test
    fun `Parsing posts true to toggleProgressIndicator property`() {
        val repository = createRepository(loadScheduleStatusFlow = sharedFlowOf(Parsing))
        val viewModel = createViewModel(repository)
        assertLiveData(viewModel.toggleProgressInfo).isEqualTo(null)
        assertLiveData(viewModel.toggleProgressIndicator).isEqualTo(true)
        assertLiveData(viewModel.showFetchFailureInfo).isEqualTo(null)
        assertLiveData(viewModel.showParseFailureInfo).isEqualTo(null)
        assertLiveData(viewModel.scheduleChangesParameter).isEqualTo(null)
        verifyInvokedOnce(repository).loadScheduleStatus
    }

    @Test
    fun `ParseSuccess posts false to toggleProgressIndicator property`() {
        val repository = createRepository(
            loadScheduleStatusFlow = sharedFlowOf(ParseSuccess),
            scheduleChangesSeen = true
        )
        val viewModel = createViewModel(repository)
        assertLiveData(viewModel.toggleProgressInfo).isEqualTo(null)
        assertLiveData(viewModel.toggleProgressIndicator).isEqualTo(false)
        assertLiveData(viewModel.showFetchFailureInfo).isEqualTo(null)
        assertLiveData(viewModel.showParseFailureInfo).isEqualTo(null)
        assertLiveData(viewModel.scheduleChangesParameter).isEqualTo(null)
        verifyInvokedOnce(repository).loadScheduleStatus
    }

    @Test
    fun `ParseSuccess posts false to toggleProgressIndicator and to scheduleChangesParameter properties`() {
        val repository = createRepository(
            loadScheduleStatusFlow = sharedFlowOf(ParseSuccess),
            scheduleChangesSeen = false,
            changedSessions = listOf(Session("changed-01").apply { changedIsNew = true })
        )
        val viewModel = createViewModel(repository)
        assertLiveData(viewModel.toggleProgressInfo).isEqualTo(null)
        assertLiveData(viewModel.toggleProgressIndicator).isEqualTo(false)
        assertLiveData(viewModel.showFetchFailureInfo).isEqualTo(null)
        val expectedSessions = listOf(Session("changed-01").apply { changedIsNew = true })
        val expectedChangeStatistic = ChangeStatistic.of(expectedSessions, logging)
        val expectedScheduleChangesParameter = ScheduleChangesParameter(scheduleVersion = "", expectedChangeStatistic)
        assertLiveData(viewModel.showParseFailureInfo).isEqualTo(null)
        assertLiveData(viewModel.scheduleChangesParameter).isEqualTo(expectedScheduleChangesParameter)
        verifyInvokedOnce(repository).loadScheduleStatus
    }

    @Test
    fun `ParseFailure posts false to toggleProgressIndicator and status to showFetchFailureInfo and parseResult to showParseFailureInfo properties`() {
        val parseResult = TestParseResult()
        val repository = createRepository(loadScheduleStatusFlow = sharedFlowOf(ParseFailure(parseResult)))
        val viewModel = createViewModel(repository)
        assertLiveData(viewModel.toggleProgressInfo).isEqualTo(null)
        assertLiveData(viewModel.toggleProgressIndicator).isEqualTo(false)
        assertLiveData(viewModel.showFetchFailureInfo).isEqualTo(null)
        assertLiveData(viewModel.showParseFailureInfo).isEqualTo(parseResult)
        assertLiveData(viewModel.scheduleChangesParameter).isEqualTo(null)
        verifyInvokedOnce(repository).loadScheduleStatus
    }

    @Test
    fun `requestScheduleUpdate invokes repository function`() {
        val repository = createRepository()
        val viewModel = createViewModel(repository)
        viewModel.requestScheduleUpdate(isUserRequest = true)
        verifyInvokedOnce(repository).loadSchedule(isUserRequest = true, onFetchingDone = {}, onParsingDone = {}, onLoadingShiftsDone = {})
    }

    @Test
    fun `cancelLoading invokes repository function`() {
        val repository = createRepository()
        val viewModel = createViewModel(repository)
        viewModel.cancelLoading()
        verifyInvokedOnce(repository).cancelLoading()
    }

    @Test
    fun `deleteSessionAlarmNotificationId invokes repository function`() {
        val repository = createRepository()
        val viewModel = createViewModel(repository)
        viewModel.deleteSessionAlarmNotificationId(7)
        verifyInvokedOnce(repository).deleteSessionAlarmNotificationId(7)
    }

    @Test
    fun `showAboutDialog posts to showAbout property`() {
        val repository = createRepository()
        val viewModel = createViewModel(repository)
        viewModel.showAboutDialog()
        assertLiveData(viewModel.showAbout).isEqualTo(Meta(version = ""))
    }

    @Test
    fun `openSessionDetails posts to openSessionDetails property`() {
        val repository = createRepository(updatedSelectedSessionId = true)
        val viewModel = createViewModel(repository)
        viewModel.openSessionDetails("S1")
        assertLiveData(viewModel.openSessionDetails).isEqualTo(Unit)
    }

    @Test
    fun `openSessionDetails does not post to openSessionDetails property`() {
        val repository = createRepository(updatedSelectedSessionId = false)
        val viewModel = createViewModel(repository)
        viewModel.openSessionDetails("S1")
        assertLiveData(viewModel.openSessionDetails).isEqualTo(null)
    }

    private class TestParseResult(
        override val isSuccess: Boolean = false
    ) : ParseResult

    private fun createRepository(
        loadScheduleStatusFlow: SharedFlow<LoadScheduleStatus> = sharedFlowOf(),
        scheduleChangesSeen: Boolean = true,
        changedSessions: List<Session> = emptyList(),
        updatedSelectedSessionId: Boolean = false
    ) = mock<AppRepository> {
        on { loadScheduleStatus } doReturn loadScheduleStatusFlow
        on { readScheduleChangesSeen() } doReturn scheduleChangesSeen
        on { readMeta() } doReturn Meta(version = "")
        on { loadChangedSessions() } doReturn changedSessions
        on { updateSelectedSessionId(any()) } doReturn updatedSelectedSessionId
    }

    private fun createViewModel(
        repository: AppRepository,
    ) = MainActivityViewModel(
        repository = repository,
        executionContext = TestExecutionContext,
        logging = logging
    )

    private fun sharedFlowOf(status: LoadScheduleStatus? = null): SharedFlow<LoadScheduleStatus> {
        return coroutineScope.sharedFlowOf(status)
    }

}
