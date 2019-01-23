package ee.sk.mid.services;

import ee.sk.mid.model.UserRequest;

import java.security.cert.X509Certificate;

public interface MobileIdCertificateService {

    X509Certificate getCertificate(UserRequest userRequest);
}
