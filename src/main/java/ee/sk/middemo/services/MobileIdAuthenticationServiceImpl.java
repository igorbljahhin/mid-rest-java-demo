package ee.sk.middemo.services;

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

import ee.sk.mid.MidAuthentication;
import ee.sk.mid.MidAuthenticationHashToSign;
import ee.sk.mid.MidAuthenticationIdentity;
import ee.sk.mid.MidAuthenticationResponseValidator;
import ee.sk.mid.MidAuthenticationResult;
import ee.sk.mid.MidClient;
import ee.sk.mid.MidDisplayTextFormat;
import ee.sk.mid.MidLanguage;
import ee.sk.mid.exception.MidDeliveryException;
import ee.sk.mid.exception.MidInternalErrorException;
import ee.sk.mid.exception.MidInvalidUserConfigurationException;
import ee.sk.mid.exception.MidMissingOrInvalidParameterException;
import ee.sk.mid.exception.MidNotMidClientException;
import ee.sk.mid.exception.MidPhoneNotAvailableException;
import ee.sk.mid.exception.MidSessionNotFoundException;
import ee.sk.mid.exception.MidSessionTimeoutException;
import ee.sk.mid.exception.MidUnauthorizedException;
import ee.sk.mid.exception.MidUserCancellationException;
import ee.sk.mid.rest.dao.MidSessionStatus;
import ee.sk.mid.rest.dao.request.MidAuthenticationRequest;
import ee.sk.mid.rest.dao.response.MidAuthenticationResponse;
import ee.sk.middemo.exception.MidOperationException;
import ee.sk.middemo.model.AuthenticationSessionInfo;
import ee.sk.middemo.model.UserRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class MobileIdAuthenticationServiceImpl implements MobileIdAuthenticationService {

    Logger logger = LoggerFactory.getLogger(MobileIdSignatureServiceImpl.class);

    @Value("${mid.auth.displayText}")
    private String midAuthDisplayText;

    @Value("${mid.auth.displayTextFormat}")
    private MidDisplayTextFormat midAuthDisplayTextFormat;

    @Value("${mid.auth.displayTextLanguage}")
    private MidLanguage midAuthLanguage;

    @Autowired
    private MidClient client;

    @Autowired
    private MidAuthenticationResponseValidator midAuthenticationResponseValidator;

    @Override
    public AuthenticationSessionInfo startAuthentication(UserRequest userRequest) {
        MidAuthenticationHashToSign authenticationHash = MidAuthenticationHashToSign.generateRandomHashOfDefaultType();

        return AuthenticationSessionInfo.newBuilder()
                .withUserRequest(userRequest)
                .withAuthenticationHash(authenticationHash)
                .withVerificationCode(authenticationHash.calculateVerificationCode())
                .build();
    }

    @Override
    public MidAuthenticationIdentity authenticate(AuthenticationSessionInfo authenticationSessionInfo) {

        UserRequest userRequest = authenticationSessionInfo.getUserRequest();
        MidAuthenticationHashToSign authenticationHash = authenticationSessionInfo.getAuthenticationHash();

        MidAuthenticationRequest request = MidAuthenticationRequest.newBuilder()
                .withPhoneNumber(userRequest.getPhoneNumber())
                .withNationalIdentityNumber(userRequest.getNationalIdentityNumber())
                .withHashToSign(authenticationHash)
                .withLanguage( midAuthLanguage )
                .withDisplayText(midAuthDisplayText)
                .withDisplayTextFormat(midAuthDisplayTextFormat)
                .build();

        MidAuthenticationResult authenticationResult;

        try {
            MidAuthenticationResponse response = client.getMobileIdConnector().authenticate(request);
            MidSessionStatus sessionStatus = client.getSessionStatusPoller()
                .fetchFinalAuthenticationSessionStatus(response.getSessionID());
            MidAuthentication authentication = client.createMobileIdAuthentication(sessionStatus, authenticationHash);

            authenticationResult = midAuthenticationResponseValidator.validate(authentication);

        }
        catch (MidUserCancellationException e) {
            logger.info("User cancelled operation from his/her phone.");
            throw new MidOperationException("You cancelled operation from your phone.");
        }
        catch (MidNotMidClientException e) {
            logger.info("User is not a MID client or user's certificates are revoked");
            throw new MidOperationException("You are not a Mobile-ID client or your Mobile-ID certificates are revoked. Please contact your mobile operator.");
        }
        catch (MidSessionTimeoutException e) {
            logger.info("User did not type in PIN code or communication error.");
            throw new MidOperationException("You didn't type in PIN code into your phone or there was a communication error.");
        }
        catch (MidPhoneNotAvailableException e) {
            logger.info("Unable to reach phone/SIM card. User needs to check if phone has coverage.");
            throw new MidOperationException("Unable to reach your phone. Please make sure your phone has mobile coverage.");
        }
        catch (MidDeliveryException e) {
            logger.info("Error communicating with the phone/SIM card.");
            throw new MidOperationException("Communication error. Unable to reach your phone.");
        }
        catch (MidInvalidUserConfigurationException e) {
            logger.info("Mobile-ID configuration on user's SIM card differs from what is configured on service provider side. User needs to contact his/her mobile operator.");
            throw new MidOperationException("Mobile-ID configuration on your SIM card differs from what is configured on service provider's side. Please contact your mobile operator.");
        }
        catch (MidSessionNotFoundException | MidMissingOrInvalidParameterException | MidUnauthorizedException e) {
            logger.error("Integrator-side error with MID integration (including insufficient input validation) or configuration", e);
            throw new MidOperationException("Client side error with mobile-ID integration.", e);
        }
        catch (MidInternalErrorException e) {
            logger.warn("MID service returned internal error that cannot be handled locally.");
            throw new MidOperationException("MID internal error", e);
        }

        if (!authenticationResult.isValid()) {
            throw new MidOperationException(authenticationResult.getErrors());
        }

        return authenticationResult.getAuthenticationIdentity();
    }
}
