package com.bm.copilot.common;

/** The request body is larger than the configured limit. Answered with 413. */
class PayloadTooLargeException extends RuntimeException {

    PayloadTooLargeException(String message) {
        super(message);
    }
}
