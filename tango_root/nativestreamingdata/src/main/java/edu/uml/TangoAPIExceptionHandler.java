package edu.uml;

/**
 * Created by emarcoux on 10/30/14.
 */
public interface TangoAPIExceptionHandler {
    // should be compared to TangoApiException
    public void handleException(String eventKey, String eventValue);
}
