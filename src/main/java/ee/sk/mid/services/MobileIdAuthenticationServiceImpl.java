package ee.sk.mid.services;

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
