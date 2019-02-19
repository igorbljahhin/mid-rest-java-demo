package ee.sk.mid.services;

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

import ee.sk.mid.AuthenticationIdentity;
import ee.sk.mid.AuthenticationResponseValidator;
import ee.sk.mid.DisplayTextFormat;
import ee.sk.mid.Language;
import ee.sk.mid.MobileIdAuthentication;
import ee.sk.mid.MobileIdAuthenticationHash;
import ee.sk.mid.MobileIdAuthenticationResult;
import ee.sk.mid.MobileIdClient;
import ee.sk.mid.exception.MidAuthException;
import ee.sk.mid.model.AuthenticationSessionInfo;
import ee.sk.mid.model.UserRequest;
import ee.sk.mid.rest.dao.SessionStatus;
import ee.sk.mid.rest.dao.request.AuthenticationRequest;
import ee.sk.mid.rest.dao.response.AuthenticationResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class MobileIdAuthenticationServiceImpl implements MobileIdAuthenticationService {

    @Value("${mid.auth.displayText}")
    private String midAuthDisplayText;

    @Autowired
    private MobileIdClient client;


    @Override
    public AuthenticationSessionInfo startAuthentication(UserRequest userRequest) {
        MobileIdAuthenticationHash authenticationHash = MobileIdAuthenticationHash.generateRandomHashOfDefaultType();

        return AuthenticationSessionInfo.newBuilder()
                .withUserRequest(userRequest)
                .withAuthenticationHash(authenticationHash)
                .withVerificationCode(authenticationHash.calculateVerificationCode())
                .build();
    }

    @Override
    public AuthenticationIdentity authenticate(AuthenticationSessionInfo authenticationSessionInfo) {

        UserRequest userRequest = authenticationSessionInfo.getUserRequest();
        MobileIdAuthenticationHash authenticationHash = authenticationSessionInfo.getAuthenticationHash();

        AuthenticationRequest request = AuthenticationRequest.newBuilder()
                .withPhoneNumber(userRequest.getPhoneNumber())
                .withNationalIdentityNumber(userRequest.getNationalIdentityNumber())
                .withAuthenticationHash(authenticationHash)
                .withLanguage(Language.ENG)
                .withDisplayText(midAuthDisplayText)
                .withDisplayTextFormat(DisplayTextFormat.GSM7)
                .build();

        MobileIdAuthenticationResult authenticationResult;
        try {
            AuthenticationResponse response = client.getMobileIdConnector().authenticate(request);
            SessionStatus sessionStatus = client.getSessionStatusPoller().fetchFinalSessionStatus(response.getSessionID(),
                    "/mid-api/authentication/session/{sessionId}");
            MobileIdAuthentication authentication = client.createMobileIdAuthentication(sessionStatus, authenticationHash.getHashInBase64(),
                    authenticationHash.getHashType());

            AuthenticationResponseValidator validator = new AuthenticationResponseValidator();
            authenticationResult = validator.validate(authentication);

        } catch (Exception e) {
            throw new MidAuthException(e);
        }

        if (!authenticationResult.isValid()) {
            throw new MidAuthException(authenticationResult.getErrors());
        }

        return authenticationResult.getAuthenticationIdentity();

    }
}
