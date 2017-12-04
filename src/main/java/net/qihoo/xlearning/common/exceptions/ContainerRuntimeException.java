package net.qihoo.xlearning.common.exceptions;

public class ContainerRuntimeException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public ContainerRuntimeException() {
  }

  public ContainerRuntimeException(String message) {
    super(message);
  }

  public ContainerRuntimeException(String message, Throwable cause) {
    super(message, cause);
  }

  public ContainerRuntimeException(Throwable cause) {
    super(cause);
  }

  public ContainerRuntimeException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
