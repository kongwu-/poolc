package cc.leevi.common.poolc;

public class PoolcException extends RuntimeException{
    public PoolcException() {
    }

    public PoolcException(String message) {
        super(message);
    }

    public PoolcException(String message, Throwable cause) {
        super(message, cause);
    }

    public PoolcException(Throwable cause) {
        super(cause);
    }

    public PoolcException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
