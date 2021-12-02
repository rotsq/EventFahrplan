package info.metadude.android.eventfahrplan.network.downloading

/**
 * Result type for downloading the schedule XML.
 */
sealed interface Result {

    sealed interface Success : Result {

        data class Ok(val scheduleXml: String, val updatedETag: String) : Success

        object NotModified : Success
    }

    data class Failure(val exception: Exception) : Result

}
