package info.metadude.android.eventfahrplan.network.repositories

import androidx.annotation.WorkerThread
import androidx.core.net.toUri
import info.metadude.android.eventfahrplan.network.downloading.Result.Failure
import info.metadude.android.eventfahrplan.network.downloading.Result.Success.NotModified
import info.metadude.android.eventfahrplan.network.downloading.Result.Success.Ok
import info.metadude.android.eventfahrplan.network.downloading.ScheduleDownloader
import info.metadude.android.eventfahrplan.network.exceptions.HttpCannotParseContentException
import info.metadude.android.eventfahrplan.network.exceptions.HttpCleartextNotPermittedException
import info.metadude.android.eventfahrplan.network.exceptions.HttpConnectionTimeoutException
import info.metadude.android.eventfahrplan.network.exceptions.HttpCouldNotConnectException
import info.metadude.android.eventfahrplan.network.exceptions.HttpDnsUnknownHostException
import info.metadude.android.eventfahrplan.network.exceptions.HttpLoginFailUntrustedCertificateException
import info.metadude.android.eventfahrplan.network.exceptions.HttpNotFoundException
import info.metadude.android.eventfahrplan.network.exceptions.HttpWrongCredentialsException
import info.metadude.android.eventfahrplan.network.fetching.FetchScheduleResult
import info.metadude.android.eventfahrplan.network.fetching.HttpStatus.HTTP_CANNOT_PARSE_CONTENT
import info.metadude.android.eventfahrplan.network.fetching.HttpStatus.HTTP_CLEARTEXT_NOT_PERMITTED
import info.metadude.android.eventfahrplan.network.fetching.HttpStatus.HTTP_CONNECT_TIMEOUT
import info.metadude.android.eventfahrplan.network.fetching.HttpStatus.HTTP_COULD_NOT_CONNECT
import info.metadude.android.eventfahrplan.network.fetching.HttpStatus.HTTP_DNS_FAILURE
import info.metadude.android.eventfahrplan.network.fetching.HttpStatus.HTTP_LOGIN_FAIL_UNTRUSTED_CERTIFICATE
import info.metadude.android.eventfahrplan.network.fetching.HttpStatus.HTTP_NOT_FOUND
import info.metadude.android.eventfahrplan.network.fetching.HttpStatus.HTTP_NOT_MODIFIED
import info.metadude.android.eventfahrplan.network.fetching.HttpStatus.HTTP_OK
import info.metadude.android.eventfahrplan.network.fetching.HttpStatus.HTTP_WRONG_HTTP_CREDENTIALS
import info.metadude.android.eventfahrplan.network.models.Meta
import info.metadude.android.eventfahrplan.network.models.Session
import info.metadude.android.eventfahrplan.network.serialization.FahrplanParser
import javax.net.ssl.SSLException

class ScheduleNetworkRepository(

    private val scheduleDownloader: ScheduleDownloader

) {

    companion object {
        private val HTTP_STATUS_BY_HTTP_EXCEPTION_TYPE = mapOf(
            HttpLoginFailUntrustedCertificateException::class to HTTP_LOGIN_FAIL_UNTRUSTED_CERTIFICATE,
            HttpConnectionTimeoutException::class to HTTP_CONNECT_TIMEOUT,
            HttpDnsUnknownHostException::class to HTTP_DNS_FAILURE,
            HttpCleartextNotPermittedException::class to HTTP_CLEARTEXT_NOT_PERMITTED,
            HttpCouldNotConnectException::class to HTTP_COULD_NOT_CONNECT,
            HttpWrongCredentialsException::class to HTTP_WRONG_HTTP_CREDENTIALS,
            HttpNotFoundException::class to HTTP_NOT_FOUND,
            HttpCannotParseContentException::class to HTTP_CANNOT_PARSE_CONTENT,
        )
    }

    private val parser = FahrplanParser()

    @WorkerThread
    fun fetchSchedule(url: String, eTag: String): FetchScheduleResult {
        val hostName = requireNotNull(url.toUri().host) {
            throw NullPointerException("Host is null for url = '$url'")
        }
        val fetchScheduleResult = when (val result = scheduleDownloader.download(url, eTag)) {
            NotModified -> FetchScheduleResult(
                httpStatus = HTTP_NOT_MODIFIED,
                scheduleXml = "",
                eTag = "",
                hostName = hostName,
                exceptionMessage = ""
            )
            is Ok -> FetchScheduleResult(
                httpStatus = HTTP_OK,
                scheduleXml = result.scheduleXml,
                eTag = result.updatedETag,
                hostName = hostName,
                exceptionMessage = ""
            )
            is Failure -> FetchScheduleResult(
                httpStatus = HTTP_STATUS_BY_HTTP_EXCEPTION_TYPE[result.exception::class]!!,
                scheduleXml = "",
                eTag = "",
                hostName = hostName,
                exceptionMessage = if (result.exception is SSLException)
                    getExceptionMessage(result.exception)
                else result.exception.message.orEmpty()
            )
        }
        return fetchScheduleResult
    }

    fun parseSchedule(scheduleXml: String,
                      eTag: String,
                      onUpdateSessions: (sessions: List<Session>) -> Unit,
                      onUpdateMeta: (meta: Meta) -> Unit,
                      onParsingDone: (result: Boolean, version: String) -> Unit) {
        parser.setListener(object : FahrplanParser.OnParseCompleteListener {
            override fun onUpdateSessions(sessions: List<Session>) = onUpdateSessions.invoke(sessions)
            override fun onUpdateMeta(meta: Meta) = onUpdateMeta.invoke(meta)
            override fun onParseDone(result: Boolean, version: String) = onParsingDone.invoke(result, version)
        })
        parser.parse(scheduleXml, eTag)
    }

    private fun getExceptionMessage(exception: SSLException): String {
        return when (exception.cause) {
            null -> exception.message
            else -> {
                when (exception.cause!!.cause) {
                    null -> exception.cause!!.message
                    else -> exception.cause!!.cause!!.message
                }
            }
        }.orEmpty()
    }

}
