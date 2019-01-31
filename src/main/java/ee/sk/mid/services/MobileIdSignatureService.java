package ee.sk.mid.services;

import ee.sk.mid.model.UserRequest;
import eu.europa.esig.dss.MimeType;

import java.util.List;

public interface MobileIdSignatureService {

    String sign(UserRequest userRequest, String filePath, MimeType mimeType);

    String sign(byte[] signatureToSign, UserRequest userRequest);

    List<String> sign();
}
