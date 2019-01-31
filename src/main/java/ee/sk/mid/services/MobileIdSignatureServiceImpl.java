package ee.sk.mid.services;

import ee.sk.mid.*;
import ee.sk.mid.exception.*;
import ee.sk.mid.model.UserRequest;
import ee.sk.mid.rest.dao.SessionStatus;
import ee.sk.mid.rest.dao.request.SignatureRequest;
import ee.sk.mid.rest.dao.response.SignatureResponse;
import eu.europa.esig.dss.MimeType;
import org.digidoc4j.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;
import java.io.File;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

@Service
public class MobileIdSignatureServiceImpl implements MobileIdSignatureService {

    @Value("${mid.relyingPartyUuid}")
    private String midRelyingPartyUuid;

    @Value("${mid.relyingPartyName}")
    private String midRelyingPartyName;

    @Value("${mid.applicationProvider.host}")
    private String midApplicationProviderHost;

    @Value("${mid.sign.displayText}")
    private String midSignDisplayText;

    private MobileIdCertificateService certificateService;
    private MobileIdClient client;

    private Container container;
    private DataToSign dataToSign;
    private SignatureRequest request;


    public MobileIdSignatureServiceImpl(MobileIdCertificateService certificateService) {
        this.certificateService = certificateService;
    }

    @PostConstruct
    public void init() {
        client = MobileIdClient.newBuilder()
                .withRelyingPartyUUID(midRelyingPartyUuid)
                .withRelyingPartyName(midRelyingPartyName)
                .withHostUrl(midApplicationProviderHost)
                .build();
    }

    @Override
    public String sign(UserRequest userRequest, String filePath, MimeType mimeType) {

        Configuration configuration = new Configuration(Configuration.Mode.TEST);

        container = ContainerBuilder
                .aContainer()
                .withConfiguration(configuration)
                .withDataFile(filePath, mimeType.getMimeTypeString())
                .build();

        X509Certificate signingCert;
        try {
            signingCert = certificateService.getCertificate(userRequest);
        } catch (ParameterMissingException e) {
            return "Input parameters are missing";
        } catch (InternalServerErrorException | ResponseRetrievingException e) {
            return "Error getting response from cert-store/MSSP";
        } catch (NotFoundException e) {
            return "Response not found ";
        } catch (BadRequestException e) {
            return "Request is invalid";
        } catch (NotAuthorizedException e) {
            return "Request is unauthorized";
        } catch (SessionTimeoutException e) {
            return "Session timeout";
        } catch (ExpiredException e) {
            return "Inactive certificate found";
        } catch (RuntimeException e) {
            return e.getMessage();
        }

        dataToSign = SignatureBuilder
                .aSignature(container)
                .withSigningCertificate(signingCert)
                .withSignatureDigestAlgorithm(DigestAlgorithm.SHA256)
                .buildDataToSign();

        byte[] signatureToSign = dataToSign.getDataToSign();

        String verificationCode;
        try {
            verificationCode = sign(signatureToSign, userRequest);
        } catch (ParameterMissingException e) {
            return "Input parameters are missing";
        } catch (RuntimeException e) {
            return e.getMessage();
        }

        return verificationCode;
    }

    @Override
    public String sign(byte[] signatureToSign, UserRequest userRequest) {
        SignableData dataToSign = new SignableData(signatureToSign);
        dataToSign.setHashType(HashType.SHA256);

        request = SignatureRequest.newBuilder()
                .withRelyingPartyUUID(client.getRelyingPartyUUID())
                .withRelyingPartyName(client.getRelyingPartyName())
                .withPhoneNumber(userRequest.getPhoneNumber())
                .withNationalIdentityNumber(userRequest.getNationalIdentityNumber())
                .withSignableData(dataToSign)
                .withLanguage(Language.ENG)
                .withDisplayText(midSignDisplayText)
                .build();

        return dataToSign.calculateVerificationCode();
    }

    @Override
    public Map<String, String> sign() {
        Map<String, String> result = new HashMap<>();

        MobileIdSignature mobileIdSignature;
        try {
            SignatureResponse response = client.getMobileIdConnector().sign(request);

            SessionStatus sessionStatus = client.getSessionStatusPoller().fetchFinalSessionStatus(response.getSessionID(),
                    "/mid-api/signature/session/{sessionId}");

            mobileIdSignature = client.createMobileIdSignature(sessionStatus);
        } catch (InternalServerErrorException | ResponseRetrievingException e) {
            result.put("result", "Error getting response from cert-store/MSSP");
            return result;
        } catch (NotFoundException e) {
            result.put("result", "Response not found");
            return result;
        } catch (BadRequestException e) {
            result.put("result", "Request is invalid");
            return result;
        } catch (NotAuthorizedException e) {
            result.put("result", "Request is unauthorized");
            return result;
        } catch (SessionTimeoutException e) {
            result.put("result", "Session timeout");
            return result;
        } catch (NotMIDClientException e) {
            result.put("result", "Given user has no active certificates and is not MID client");
            return result;
        } catch (ExpiredException e) {
            result.put("result", "MSSP transaction timed out");
            return result;
        } catch (UserCancellationException e) {
            result.put("result", "User cancelled the operation");
            return result;
        } catch (MIDNotReadyException e) {
            result.put("result", "Mobile-ID not ready");
            return result;
        } catch (SimNotAvailableException e) {
            result.put("result", "Sim not available");
            return result;
        } catch (DeliveryException e) {
            result.put("result", "SMS sending error");
            return result;
        } catch (InvalidCardResponseException e) {
            result.put("result", "Invalid response from card");
            return result;
        } catch (SignatureHashMismatchException e) {
            result.put("result", "Hash does not match with certificate type");
            return result;
        } catch (RuntimeException e) {
            result.put("result", e.getMessage());
            return result;
        }

        Signature signature = dataToSign.finalize(mobileIdSignature.getValue());
        container.addSignature(signature);

        String filePath  = null;

        try {
            File containerFile = File.createTempFile("mid-demo-", ".asice");
            filePath = containerFile.getAbsolutePath();
            container.saveAsFile(filePath);

        } catch (IOException e) {
            e.printStackTrace();
        }

        result.put("result", "Signing successful");
        result.put("isValid", isSignatureValid(signature));
        result.put("timestamp", "Signed on: " + signature.getTimeStampCreationTime().toString());
        result.put("filename", "Container was saved to: " + filePath);

        return result;
    }

    private String isSignatureValid(Signature signature) {
        if (signature.validateSignature().isValid()) {
            return "Signature is valid";
        } else {
            return "Signature is not valid";
        }
    }
}
