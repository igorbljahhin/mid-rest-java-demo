package ee.sk.mid.model;

import ee.sk.mid.MobileIdAuthenticationHash;

public class AuthenticationSessionInfo {

    private MobileIdAuthenticationHash authenticationHash;
    private String verificationCode;
    private UserRequest userRequest;

    private AuthenticationSessionInfo(Builder builder) {
        this.authenticationHash = builder.authenticationHash;
        this.verificationCode = builder.verificationCode;
        this.userRequest = builder.userRequest;
    }

    public MobileIdAuthenticationHash getAuthenticationHash() {
        return authenticationHash;
    }

    public String getVerificationCode() {
        return verificationCode;
    }

    public UserRequest getUserRequest() {
        return userRequest;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private String verificationCode;
        private UserRequest userRequest;
        private MobileIdAuthenticationHash authenticationHash;

        private Builder() {
        }

        public Builder withAuthenticationHash(MobileIdAuthenticationHash authenticationHash) {
            this.authenticationHash = authenticationHash;
            return this;
        }

        public Builder withVerificationCode(String verificationCode) {
            this.verificationCode = verificationCode;
            return this;
        }

        public Builder withUserRequest(UserRequest userRequest) {
            this.userRequest = userRequest;
            return this;
        }

        public AuthenticationSessionInfo build() {
            return new AuthenticationSessionInfo(this);
        }

    }

}
