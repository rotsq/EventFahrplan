package nerd.tuxmobil.fahrplan.congress.schedule.observables

import nerd.tuxmobil.fahrplan.congress.changes.ChangeStatistic
import nerd.tuxmobil.fahrplan.congress.schedule.MainActivity
import nerd.tuxmobil.fahrplan.congress.schedule.MainActivityViewModel

/**
 * Payload of the observable [scheduleChangesParameter][MainActivityViewModel.scheduleChangesParameter]
 * property in the [MainActivityViewModel] which is observed by the [MainActivity].
 */
data class ScheduleChangesParameter(

    val scheduleVersion: String,
    val changeStatistic: ChangeStatistic

)
