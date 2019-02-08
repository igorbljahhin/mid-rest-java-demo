package ee.sk.mid.services;

import ee.sk.mid.model.SigningResult;
import ee.sk.mid.model.SigningSessionInfo;
import ee.sk.mid.model.UserRequest;

public interface MobileIdSignatureService {

    SigningSessionInfo sendSignatureRequest(UserRequest userRequest);

    SigningResult sign(SigningSessionInfo signingSessionInfo);
}
