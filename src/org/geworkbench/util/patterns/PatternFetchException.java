package org.geworkbench.util.patterns;


/**
 * This exception is thrown when a pattern can't be fetched
 * from a pattern source.
 */

public class PatternFetchException extends RuntimeException {

	private static final long serialVersionUID = -6269930506232901690L;

	public PatternFetchException() {
    }

    public PatternFetchException(String message) {
        super(message);
    }

    public PatternFetchException(String message, Throwable cause) {
        super(message, cause);
    }

    public PatternFetchException(Throwable cause) {
        super(cause);
    }
}
