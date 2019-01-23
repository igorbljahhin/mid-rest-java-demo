package ee.sk.mid.services;

import ee.sk.mid.model.UserRequest;

public interface MobileIdAuthenticationService {

    String authenticate(UserRequest userRequest);
}
