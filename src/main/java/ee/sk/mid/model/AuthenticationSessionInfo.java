package ee.sk.mid.model;

/*-
 * #%L
 * Mobile ID sample Java client
 * %%
 * Copyright (C) 2018 - 2019 SK ID Solutions AS
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

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
