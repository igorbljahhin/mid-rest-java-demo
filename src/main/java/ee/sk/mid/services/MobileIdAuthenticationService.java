package ee.sk.mid.services;

import ee.sk.mid.MobileIdAuthenticationHash;
import ee.sk.mid.model.UserRequest;

public interface MobileIdAuthenticationService {

    MobileIdAuthenticationHash authenticate(UserRequest userRequest);

    String authenticate(MobileIdAuthenticationHash authenticationHash);
}
