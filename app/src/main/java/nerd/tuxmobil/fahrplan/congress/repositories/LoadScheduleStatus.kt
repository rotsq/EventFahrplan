package nerd.tuxmobil.fahrplan.congress.repositories

import nerd.tuxmobil.fahrplan.congress.net.HttpStatus
import nerd.tuxmobil.fahrplan.congress.net.ParseResult

sealed class LoadScheduleStatus {

    object InitialFetching : LoadScheduleStatus()

    object Fetching : LoadScheduleStatus()

    object FetchSuccess : LoadScheduleStatus()

    data class FetchFailure(
        val httpStatus: HttpStatus,
        val hostName: String,
        val exceptionMessage: String,
        val isUserRequest: Boolean
    ) : LoadScheduleStatus()

    object InitialParsing : LoadScheduleStatus()

    object Parsing : LoadScheduleStatus()

    object ParseSuccess : LoadScheduleStatus()

    // TODO Merge ParseResult innards into ParseFailure class
    data class ParseFailure(
        val parseResult: ParseResult
    ) : LoadScheduleStatus()

}
