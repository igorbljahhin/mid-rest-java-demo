package ee.sk.mid.services;

import ee.sk.mid.model.UserRequest;

import java.util.List;

public interface MobileIdSignatureService {

    List<String> sign(UserRequest userRequest);
}
