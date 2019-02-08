package ee.sk.mid.services;

import ee.sk.mid.MobileIdClient;
import ee.sk.mid.model.UserRequest;
import ee.sk.mid.rest.dao.request.CertificateRequest;
import ee.sk.mid.rest.dao.response.CertificateChoiceResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.cert.X509Certificate;

@Service
public class MobileIdCertificateServiceImpl implements MobileIdCertificateService {

    @Autowired
    private MobileIdClient client;

    @Override
    public X509Certificate getCertificate(UserRequest userRequest) {
        CertificateRequest request = CertificateRequest.newBuilder()
                .withRelyingPartyUUID(client.getRelyingPartyUUID())
                .withRelyingPartyName(client.getRelyingPartyName())
                .withPhoneNumber(userRequest.getPhoneNumber())
                .withNationalIdentityNumber(userRequest.getNationalIdentityNumber())
                .build();

        CertificateChoiceResponse response = client.getMobileIdConnector().getCertificate(request);

        return client.createMobileIdCertificate(response);
    }
}
