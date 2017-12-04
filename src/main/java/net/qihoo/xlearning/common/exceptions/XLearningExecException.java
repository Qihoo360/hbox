
package net.qihoo.xlearning.common.exceptions;

public class XLearningExecException extends RuntimeException {

  private static final long serialVersionUID = 1L;


  public XLearningExecException() {
  }

  public XLearningExecException(String message) {
    super(message);
  }

  public XLearningExecException(String message, Throwable cause) {
    super(message, cause);
  }

  public XLearningExecException(Throwable cause) {
    super(cause);
  }

  public XLearningExecException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
