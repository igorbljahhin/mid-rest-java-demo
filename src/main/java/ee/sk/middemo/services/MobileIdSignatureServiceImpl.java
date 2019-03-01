package ee.sk.middemo.services;

/*-
 * #%L
 * Mobile ID sample Java client
 * %%
 * Copyright (C) 2018 - 2019 SK ID Solutions AS
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import java.io.File;
import java.io.IOException;
import java.security.cert.X509Certificate;

import ee.sk.mid.DisplayTextFormat;
import ee.sk.mid.HashToSign;
import ee.sk.mid.HashType;
import ee.sk.mid.Language;
import ee.sk.mid.MobileIdClient;
import ee.sk.mid.MobileIdSignature;
import ee.sk.mid.exception.DeliveryException;
import ee.sk.mid.exception.MidInternalErrorException;
import ee.sk.mid.exception.MidSessionTimeoutException;
import ee.sk.mid.exception.NotMidClientException;
import ee.sk.mid.exception.PhoneNotAvailableException;
import ee.sk.mid.exception.UserCancellationException;
import ee.sk.mid.rest.dao.SessionStatus;
import ee.sk.mid.rest.dao.request.SignatureRequest;
import ee.sk.mid.rest.dao.response.SignatureResponse;
import ee.sk.middemo.exception.FileUploadException;
import ee.sk.middemo.exception.MidOperationException;
import ee.sk.middemo.model.SigningResult;
import ee.sk.middemo.model.SigningSessionInfo;
import ee.sk.middemo.model.UserRequest;
import org.digidoc4j.Configuration;
import org.digidoc4j.Container;
import org.digidoc4j.ContainerBuilder;
import org.digidoc4j.DataFile;
import org.digidoc4j.DataToSign;
import org.digidoc4j.DigestAlgorithm;
import org.digidoc4j.Signature;
import org.digidoc4j.SignatureBuilder;
import org.digidoc4j.SignatureProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

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
        DataFile uploadedFile = getUploadedDataFile(userRequest.getFile());

        Configuration configuration = new Configuration(Configuration.Mode.TEST);

        Container container = ContainerBuilder.aContainer()
            .withConfiguration(configuration)
            .withDataFile(uploadedFile)
            .build();

        X509Certificate signingCert = certificateService.getCertificate(userRequest);

        DataToSign dataToSignExternally = SignatureBuilder.aSignature(container)
            .withSigningCertificate(signingCert)
            .withSignatureDigestAlgorithm(DigestAlgorithm.SHA256)
            .withSignatureProfile(SignatureProfile.LT)
            .buildDataToSign();

        HashToSign hashToSign = HashToSign.newBuilder()
            .withDataToHash(dataToSignExternally.getDataToSign())
            .withHashType(HashType.SHA256)
            .build();

        SignatureRequest signatureRequest = SignatureRequest.newBuilder()
            .withPhoneNumber(userRequest.getPhoneNumber())
            .withNationalIdentityNumber(userRequest.getNationalIdentityNumber())
            .withHashToSign(hashToSign)
            .withLanguage(Language.ENG)
            .withDisplayText(midSignDisplayText)
            .withDisplayTextFormat(DisplayTextFormat.GSM7)
            .build();

        SignatureResponse response = client.getMobileIdConnector().sign(signatureRequest);

        return SigningSessionInfo.newBuilder()
            .withSessionID(response.getSessionID())
            .withVerificationCode(hashToSign.calculateVerificationCode())
            .withDataToSign(dataToSignExternally)
            .withContainer(container)
            .build();
    }

    private DataFile getUploadedDataFile(MultipartFile uploadedFile) {
        try {
            return new DataFile(uploadedFile.getInputStream(), uploadedFile.getOriginalFilename(), uploadedFile.getContentType());
        } catch (IOException e) {
            throw new FileUploadException(e.getCause());
        }
    }

    @Override
    public SigningResult sign(SigningSessionInfo signingSessionInfo) {
        SessionStatus sessionStatus = client.getSessionStatusPoller()
            .fetchFinalSignatureSessionStatus(signingSessionInfo.getSessionID());

        MobileIdSignature mobileIdSignature = client.createMobileIdSignature(sessionStatus);

        Signature signature = signingSessionInfo.getDataToSign().finalize(mobileIdSignature.getValue());
        signingSessionInfo.getContainer().addSignature(signature);

        String filePath;

        try {
            File containerFile = File.createTempFile("mid-demo-container-", ".asice");
            filePath = containerFile.getAbsolutePath();
            signingSessionInfo.getContainer().saveAsFile(filePath);

        }
        catch (UserCancellationException e) {
            logger.info("User cancelled operation");
            throw new MidOperationException("User cancelled operation.");
        }
        catch (NotMidClientException e) {
            logger.info("User is not a MID client or user's certificates are revoked");
            throw new MidOperationException("User is not a MID client or user's certificates are revoked.");
        }
        catch (MidSessionTimeoutException e) {
            logger.info("User did not type in PIN or communication error.");
            throw new MidOperationException("User didn't type in PIN or communication error.");        }
        catch (PhoneNotAvailableException | DeliveryException e) {
            logger.info("Unable to reach phone/SIM card");
            throw new MidOperationException("Communication error. Unable to reach phone.");
        }
        catch (MidInternalErrorException e) {
            logger.warn("MID service returned internal error that cannot be handled locally.");
            // navigate to error page
            throw new MidOperationException("MID internal error", e);
        }
        catch (IOException e) {
            throw new MidOperationException("Could not create container file.");
        }

        return SigningResult.newBuilder()
            .withResult("Signing successful")
            .withValid(signature.validateSignature().isValid())
            .withTimestamp(signature.getTimeStampCreationTime())
            .withContainerFilePath(filePath)
            .build();

    }

}
