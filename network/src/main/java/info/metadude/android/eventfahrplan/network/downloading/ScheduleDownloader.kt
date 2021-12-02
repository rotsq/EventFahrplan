package info.metadude.android.eventfahrplan.network.downloading

import androidx.annotation.WorkerThread
import info.metadude.android.eventfahrplan.commons.logging.Logging
import info.metadude.android.eventfahrplan.network.downloading.Result.Failure
import info.metadude.android.eventfahrplan.network.downloading.Result.Success.NotModified
import info.metadude.android.eventfahrplan.network.downloading.Result.Success.Ok
import info.metadude.android.eventfahrplan.network.exceptions.HttpCannotParseContentException
import info.metadude.android.eventfahrplan.network.exceptions.HttpCleartextNotPermittedException
import info.metadude.android.eventfahrplan.network.exceptions.HttpConnectionTimeoutException
import info.metadude.android.eventfahrplan.network.exceptions.HttpCouldNotConnectException
import info.metadude.android.eventfahrplan.network.exceptions.HttpDnsUnknownHostException
import info.metadude.android.eventfahrplan.network.exceptions.HttpLoginFailUntrustedCertificateException
import info.metadude.android.eventfahrplan.network.exceptions.HttpNotFoundException
import info.metadude.android.eventfahrplan.network.exceptions.HttpWrongCredentialsException
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.net.UnknownServiceException
import javax.net.ssl.SSLException

class ScheduleDownloader(

    private val okHttpClient: OkHttpClient,
    private val logging: Logging = Logging.get()

) {

    private companion object {
        const val LOG_TAG = "ScheduleDownloader"
        const val HEADER_NAME_IF_NONE_MATCH = "If-None-Match"
        const val HEADER_NAME_ETAG = "ETag"
    }

    /**
     * Returns a [Result] containing either
     * - nothing, if the server has no new schedule data based on the given [eTag]
     * - the schedule XML string and the updated ETag if the download was successful
     * - an exception if something went wrong while downloading or unpacking
     */
    @WorkerThread
    fun download(url: String, eTag: String): Result {
        logging.d(LOG_TAG, "Downloading -> url = '$url', eTag = '$eTag'")
        val requestBuilder = Request.Builder().url(url)
        if (eTag.isNotEmpty()) {
            requestBuilder.addHeader(HEADER_NAME_IF_NONE_MATCH, eTag)
        }
        val request = requestBuilder.build()

        val response: Response
        try {
            val call = okHttpClient.newCall(request)
            response = call.execute()
        } catch (e: SSLException) {
            return Failure(HttpLoginFailUntrustedCertificateException())
        } catch (e: SocketTimeoutException) {
            return Failure(HttpConnectionTimeoutException())
        } catch (e: UnknownHostException) {
            return Failure(HttpDnsUnknownHostException())
        } catch (e: UnknownServiceException) {
            return Failure(HttpCleartextNotPermittedException())
        } catch (e: IOException) {
            return Failure(HttpCouldNotConnectException())
        }

        val statusCode = response.code()
        if (statusCode == 304) {
            return NotModified
        }

        if (statusCode != 200) {
            logging.e(LOG_TAG, "Download error -> statusCode = '$statusCode'")
            if (statusCode == 401) {
                return Failure(HttpWrongCredentialsException())
            }
            if (statusCode == 404) {
                return Failure(HttpNotFoundException())
            }
            return Failure(Exception(HttpCouldNotConnectException()))
        }

        val updatedETag = response.header(HEADER_NAME_ETAG).orEmpty()
        logging.d(LOG_TAG, "New eTag = '$updatedETag'")

        val responseStr: String
        try {
            responseStr = response.body()!!.string()
        } catch (e: NullPointerException) {
            return Failure(HttpCannotParseContentException())
        } catch (e: IOException) {
            return Failure(HttpCannotParseContentException())
        } finally {
            response.body()?.close()
        }
        logging.d(LOG_TAG, "Download finished successfully.")
        return Ok(responseStr, updatedETag)
    }
}

