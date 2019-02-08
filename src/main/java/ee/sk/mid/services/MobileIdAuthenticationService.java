package ee.sk.mid.services;

import ee.sk.mid.AuthenticationIdentity;
import ee.sk.mid.model.AuthenticationSessionInfo;
import ee.sk.mid.model.UserRequest;

public interface MobileIdAuthenticationService {

    AuthenticationSessionInfo startAuthentication(UserRequest userRequest);

    AuthenticationIdentity authenticate(AuthenticationSessionInfo authenticationSessionInfo);
}
