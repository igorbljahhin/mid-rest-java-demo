package ee.sk.mid.exception;

import java.util.List;

public class MidAuthException extends RuntimeException {

    public MidAuthException(Exception e) {
        super(e);
    }

    public MidAuthException(UserCancellationException e) {

    }

    public MidAuthException(List<String> errors) {
        super("Invalid authentication. " + String.join(", ", errors));
    }

    public String getMessage() {
        return this.getCause().getMessage();
    }

}