package ee.sk.mid.services;

import ee.sk.mid.*;
import ee.sk.mid.exception.FileUploadException;
import ee.sk.mid.exception.MidSignException;
import ee.sk.mid.model.SigningResult;
import ee.sk.mid.model.SigningSessionInfo;
import ee.sk.mid.model.UserRequest;
import ee.sk.mid.rest.dao.SessionStatus;
import ee.sk.mid.rest.dao.request.SignatureRequest;
import ee.sk.mid.rest.dao.response.SignatureResponse;
import eu.europa.esig.dss.MimeType;
import org.apache.commons.codec.binary.Base64;
import org.digidoc4j.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.xml.bind.DatatypeConverter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

@Service
public class MobileIdSignatureServiceImpl implements MobileIdSignatureService {

    Logger logger = LoggerFactory.getLogger(MobileIdSignatureServiceImpl.class);


    @Value("${mid.sign.displayText}")
    private String midSignDisplayText;

    private MobileIdCertificateService certificateService;

    @Autowired
    private MobileIdClient client;

    public MobileIdSignatureServiceImpl(MobileIdCertificateService certificateService) {
        this.certificateService = certificateService;
    }

    @Override
    public SigningSessionInfo sendSignatureRequest(UserRequest userRequest) {
        String uploadedFileAbsolutePath = storeFileInTempDirectory(userRequest.getFile());
        MimeType mimeType = MimeType.fromFileName(uploadedFileAbsolutePath);

        Configuration configuration = new Configuration(Configuration.Mode.TEST); // TODO move to configuration

        Container container = ContainerBuilder
                .aContainer()
                .withConfiguration(configuration)
                .withDataFile(uploadedFileAbsolutePath, mimeType.getMimeTypeString())
                .build();

        X509Certificate signingCert = certificateService.getCertificate(userRequest);

        DataToSign dataToSignExternally = SignatureBuilder
                .aSignature(container)
                .withSigningCertificate(signingCert)
                .withSignatureDigestAlgorithm(DigestAlgorithm.SHA256)
                .withSignatureProfile(SignatureProfile.LT)
                .buildDataToSign();


        byte[] hash = DigestCalculator.calculateDigest(dataToSignExternally.getDataToSign(), HashType.SHA256);
        SignableHash signableHash = new SignableHash(); // TODO make setHash() public and add builder!
        signableHash.setHashInBase64(Base64.encodeBase64String(hash)); // insteaed of hash
        signableHash.setHashType(HashType.SHA256);

        String hashHex = DatatypeConverter.printHexBinary(hash);

        logger.debug("HashHEX is {}", hashHex);

        SignatureRequest signatureRequest = SignatureRequest.newBuilder()
                .withRelyingPartyUUID(client.getRelyingPartyUUID())
                .withRelyingPartyName(client.getRelyingPartyName())
                .withPhoneNumber(userRequest.getPhoneNumber())
                .withNationalIdentityNumber(userRequest.getNationalIdentityNumber())
                .withSignableHash(signableHash)
                .withLanguage(Language.ENG)
                .withDisplayText(midSignDisplayText)
                .build();

        SignatureResponse response = client.getMobileIdConnector().sign(signatureRequest);

        return SigningSessionInfo.newBuilder()
                .withSessionID(response.getSessionID())
                .withVerificationCode(signableHash.calculateVerificationCode())
                .withDataToSign(dataToSignExternally)
                .withContainer(container)
                .build();
    }

    private String storeFileInTempDirectory(MultipartFile uploadedFile) {
        try {
            File uploadedFileTempLocation = File.createTempFile("mid-demo-uploaded-", uploadedFile.getOriginalFilename());
            String uploadedFileAbsolutePath = uploadedFileTempLocation.getAbsolutePath();

            Files.write(Paths.get(uploadedFileAbsolutePath), uploadedFile.getBytes());

            return uploadedFileAbsolutePath;

        } catch (IOException e) {
            throw new FileUploadException(e.getCause());
        }
    }


    @Override
    public SigningResult sign(SigningSessionInfo signingSessionInfo) {
        Map<String, String> result = new HashMap<>();

        MobileIdSignature mobileIdSignature;


        SessionStatus sessionStatus = client.getSessionStatusPoller().fetchFinalSessionStatus(signingSessionInfo.getSessionID(),
                "/mid-api/signature/session/{sessionId}");

        mobileIdSignature = client.createMobileIdSignature(sessionStatus);

        Signature signature = signingSessionInfo.getDataToSign().finalize(mobileIdSignature.getValue());
        signingSessionInfo.getContainer().addSignature(signature);

        String filePath = null;

        try {
            File containerFile = File.createTempFile("mid-demo-container-", ".asice");
            filePath = containerFile.getAbsolutePath();
            signingSessionInfo.getContainer().saveAsFile(filePath);

        } catch (IOException e) {
            throw new MidSignException(e);
        }

        return SigningResult.newBuilder()
                .withResult("Signing successful")
                .withValid(signature.validateSignature().isValid())
                .withTimestamp(signature.getTimeStampCreationTime())
                .withContainerFilePath(filePath)
                .build();

    }

}
