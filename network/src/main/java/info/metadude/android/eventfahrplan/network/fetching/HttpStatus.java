package info.metadude.android.eventfahrplan.network.fetching;

public enum HttpStatus {

    HTTP_OK,
    HTTP_LOGIN_FAIL_UNTRUSTED_CERTIFICATE,
    HTTP_DNS_FAILURE,
    HTTP_COULD_NOT_CONNECT,
    HTTP_SSL_SETUP_FAILURE,
    HTTP_CANNOT_PARSE_CONTENT,
    HTTP_WRONG_HTTP_CREDENTIALS,
    HTTP_CONNECT_TIMEOUT,
    HTTP_NOT_MODIFIED,
    HTTP_NOT_FOUND,
    HTTP_CLEARTEXT_NOT_PERMITTED;

    public boolean isSuccessful() {
        return HTTP_OK.ordinal() == ordinal();
    }

    public boolean isNotModified() {
        return HTTP_NOT_MODIFIED.ordinal() == ordinal();
    }

}
