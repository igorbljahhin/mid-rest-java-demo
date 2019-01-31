package ee.sk.mid.services;

import ee.sk.mid.*;
import ee.sk.mid.exception.*;
import ee.sk.mid.model.UserRequest;
import ee.sk.mid.rest.dao.SessionStatus;
import ee.sk.mid.rest.dao.request.SignatureRequest;
import ee.sk.mid.rest.dao.response.SignatureResponse;
import eu.europa.esig.dss.MimeType;
import org.digidoc4j.*;
import org.springframework.stereotype.Service;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

@Service
public class MobileIdSignatureServiceImpl implements MobileIdSignatureService {

    private MobileIdCertificateService certificateService;
    private MobileIdClient client;

    private Container container;
    private DataToSign dataToSign;
    private SignatureRequest request;

    public MobileIdSignatureServiceImpl(MobileIdCertificateService certificateService) {
        this.certificateService = certificateService;

        client = MobileIdClient.newBuilder()
                .withRelyingPartyUUID("00000000-0000-0000-0000-000000000000")
                .withRelyingPartyName("DEMO")
                .withHostUrl("https://tsp.demo.sk.ee")
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
                .build();

        return dataToSign.calculateVerificationCode();
    }

    @Override
    public List<String> sign() {
        List<String> result = new ArrayList<>();

        MobileIdSignature mobileIdSignature;
        try {
            SignatureResponse response = client.getMobileIdConnector().sign(request);

            SessionStatus sessionStatus = client.getSessionStatusPoller().fetchFinalSessionStatus(response.getSessionID(),
                    "/mid-api/signature/session/{sessionId}");

            mobileIdSignature = client.createMobileIdSignature(sessionStatus);
        } catch (InternalServerErrorException | ResponseRetrievingException e) {
            result.add("Error getting response from cert-store/MSSP");
            return result;
        } catch (NotFoundException e) {
            result.add("Response not found");
            return result;
        } catch (BadRequestException e) {
            result.add("Request is invalid");
            return result;
        } catch (NotAuthorizedException e) {
            result.add("Request is unauthorized");
            return result;
        } catch (SessionTimeoutException e) {
            result.add("Session timeout");
            return result;
        } catch (NotMIDClientException e) {
            result.add("Given user has no active certificates and is not M-ID client");
            return result;
        } catch (ExpiredException e) {
            result.add("MSSP transaction timed out");
            return result;
        } catch (UserCancellationException e) {
            result.add("User cancelled the operation");
            return result;
        } catch (MIDNotReadyException e) {
            result.add("Mobile-ID not ready");
            return result;
        } catch (SimNotAvailableException e) {
            result.add("Sim not available");
            return result;
        } catch (DeliveryException e) {
            result.add("SMS sending error");
            return result;
        } catch (InvalidCardResponseException e) {
            result.add("Invalid response from card");
            return result;
        } catch (SignatureHashMismatchException e) {
            result.add("Hash does not match with certificate type");
            return result;
        } catch (RuntimeException e) {
            result.add(e.getMessage());
            return result;
        }

        Signature signature = dataToSign.finalize(mobileIdSignature.getValue());
        container.addSignature(signature);

        result.add("Signing successful");
        result.add(isSignatureValid(signature));
        result.add("Signed on: " + signature.getTimeStampCreationTime().toString());

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
