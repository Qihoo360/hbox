package net.qihoo.hbox.common.exceptions;

public class RequestOverLimitException extends HboxExecException {

  private static final long serialVersionUID = 1L;

  public RequestOverLimitException(String message) {
    super(message);
  }
}
