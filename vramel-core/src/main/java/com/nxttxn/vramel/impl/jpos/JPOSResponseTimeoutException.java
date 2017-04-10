package com.nxttxn.vramel.impl.jpos;

/**
 * JPOSResponseTimeoutException is thrown when a JPOS response is expected and does not arrive within the timeout period.
 */
class JPOSResponseTimeoutException extends RuntimeException {
    JPOSResponseTimeoutException(String errorMessage) {
        super(errorMessage);
    }
}
