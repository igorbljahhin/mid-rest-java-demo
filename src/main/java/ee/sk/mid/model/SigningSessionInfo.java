package ee.sk.mid.model;

import org.digidoc4j.Container;
import org.digidoc4j.DataToSign;

public class SigningSessionInfo {

    private String sessionID;
    private String verificationCode;
    private DataToSign dataToSign;
    private Container container;

    private SigningSessionInfo(Builder builder) {
        this.sessionID = builder.sessionID;
        this.verificationCode = builder.verificationCode;
        this.dataToSign = builder.dataToSign;
        this.container = builder.container;
    }

    public String getSessionID() {
        return sessionID;
    }

    public String getVerificationCode() {
        return verificationCode;
    }

    public DataToSign getDataToSign() {
        return dataToSign;
    }

    public Container getContainer() {
        return container;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private String sessionID;
        private String verificationCode;
        private DataToSign dataToSign;
        private Container container;

        private Builder() {
        }

        public Builder withSessionID(String sessionID) {
            this.sessionID = sessionID;
            return this;
        }

        public Builder withVerificationCode(String verificationCode) {
            this.verificationCode = verificationCode;
            return this;
        }

        public Builder withDataToSign(DataToSign dataToSign) {
            this.dataToSign = dataToSign;
            return this;
        }
        public Builder withContainer(Container container) {
            this.container = container;
            return this;
        }

        public SigningSessionInfo build() {
            return new SigningSessionInfo(this);
        }
    }

}
