package ee.sk.mid.services;

import ee.sk.mid.*;
import ee.sk.mid.exception.*;
import ee.sk.mid.model.UserRequest;
import ee.sk.mid.rest.dao.SessionStatus;
import ee.sk.mid.rest.dao.request.SignatureRequest;
import ee.sk.mid.rest.dao.response.SignatureResponse;
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

    public MobileIdSignatureServiceImpl(MobileIdCertificateService certificateService) {
        this.certificateService = certificateService;
    }

    @Override
    public List<String> sign(UserRequest userRequest) {
        List<String> result = new ArrayList<>();
        Configuration configuration = new Configuration(Configuration.Mode.TEST);

        Container container = ContainerBuilder
                .aContainer()
                .withConfiguration(configuration)
                .withDataFile("../mid-rest-java-client/mid-rest-java-demo/src/main/resources/static/file_to_sign.txt", "text/plain")
                .build();

        X509Certificate signingCert;
        try {
            signingCert = certificateService.getCertificate(userRequest);
        } catch (ParameterMissingException e) {
            result.add("Input parameters are missing");
            return result;
        } catch (InternalServerErrorException | ResponseRetrievingException e) {
            result.add("Error getting response from cert-store/MSSP");
            return result;
        } catch (NotFoundException e) {
            result.add("Response not found ");
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
        } catch (ExpiredException e) {
            result.add("Inactive certificate found");
            return result;
        } catch (RuntimeException e) {
            result.add(e.getMessage());
            return result;
        }

        DataToSign dataToSign = SignatureBuilder
                .aSignature(container)
                .withSigningCertificate(signingCert)
                .withSignatureDigestAlgorithm(DigestAlgorithm.SHA256)
                .buildDataToSign();

        byte[] signatureToSign = dataToSign.getDataToSign();

        MobileIdSignature mobileIdSignature;
        try {
            mobileIdSignature = sign(signatureToSign, userRequest);
        } catch (ParameterMissingException e) {
            result.add("Input parameters are missing");
            return result;
        } catch (InternalServerErrorException | ResponseRetrievingException e) {
            result.add("Error getting response from cert-store/MSSP");
            return result;
        } catch (NotFoundException e) {
            result.add("Response not found ");
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

    public MobileIdSignature sign(byte[] signatureToSign, UserRequest userRequest) {
        MobileIdClient client = MobileIdClient.newBuilder()
                .withRelyingPartyUUID("00000000-0000-0000-0000-000000000000")
                .withRelyingPartyName("DEMO")
                .withHostUrl("https://tsp.demo.sk.ee")
                .build();

        SignableData dataToSign = new SignableData(signatureToSign);
        dataToSign.setHashType(HashType.SHA256);

        String verificationCode = dataToSign.calculateVerificationCode();

        SignatureRequest request = SignatureRequest.newBuilder()
                .withRelyingPartyUUID(client.getRelyingPartyUUID())
                .withRelyingPartyName(client.getRelyingPartyName())
                .withPhoneNumber(userRequest.getPhoneNumber())
                .withNationalIdentityNumber(userRequest.getNationalIdentityNumber())
                .withSignableData(dataToSign)
                .withLanguage(Language.ENG)
                .build();

        SignatureResponse response = client.getMobileIdConnector().sign(request);

        SessionStatus sessionStatus = client.getSessionStatusPoller().fetchFinalSessionStatus(response.getSessionID(),
                "/mid-api/signature/session/{sessionId}");

        return client.createMobileIdSignature(sessionStatus);
    }
}
