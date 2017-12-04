package net.qihoo.xlearning.common.exceptions;

public class RequestOverLimitException extends XLearningExecException {

  private static final long serialVersionUID = 1L;

  public RequestOverLimitException(String message) {
    super(message);
  }
}
