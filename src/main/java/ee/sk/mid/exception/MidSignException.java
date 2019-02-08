package ee.sk.mid.exception;

public class MidSignException extends RuntimeException {

    public MidSignException(Exception e) {
        super(e);
    }

    public MidSignException(String message) {
        super(message);
    }
}
