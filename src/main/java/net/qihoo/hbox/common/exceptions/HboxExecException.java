
package net.qihoo.hbox.common.exceptions;

public class HboxExecException extends RuntimeException {

    private static final long serialVersionUID = 1L;


    public HboxExecException() {
    }

    public HboxExecException(String message) {
        super(message);
    }

    public HboxExecException(String message, Throwable cause) {
        super(message, cause);
    }

    public HboxExecException(Throwable cause) {
        super(cause);
    }

    public HboxExecException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
