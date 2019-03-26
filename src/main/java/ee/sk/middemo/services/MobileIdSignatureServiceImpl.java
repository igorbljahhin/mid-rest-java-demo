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

import ee.sk.mid.*;
import ee.sk.mid.exception.*;
import ee.sk.mid.rest.dao.MidSessionStatus;
import ee.sk.mid.rest.dao.request.MidSignatureRequest;
import ee.sk.mid.rest.dao.response.MidSignatureResponse;
import ee.sk.middemo.exception.FileUploadException;
import ee.sk.middemo.exception.MidOperationException;
import ee.sk.middemo.model.SigningResult;
import ee.sk.middemo.model.SigningSessionInfo;
import ee.sk.middemo.model.UserRequest;
import org.digidoc4j.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.security.cert.X509Certificate;

@Service
public class MobileIdSignatureServiceImpl implements MobileIdSignatureService {

    Logger logger = LoggerFactory.getLogger(MobileIdSignatureServiceImpl.class);


    @Value("${mid.sign.displayText}")
    private String midSignDisplayText;

    private MobileIdCertificateService certificateService;

    @Autowired
    private MidClient client;

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

        MidHashToSign hashToSign = MidHashToSign.newBuilder()
            .withDataToHash(dataToSignExternally.getDataToSign())
            .withHashType( MidHashType.SHA256)
            .build();

        MidSignatureRequest signatureRequest = MidSignatureRequest.newBuilder()
            .withPhoneNumber(userRequest.getPhoneNumber())
            .withNationalIdentityNumber(userRequest.getNationalIdentityNumber())
            .withHashToSign(hashToSign)
            .withLanguage( MidLanguage.ENG)
            .withDisplayText(midSignDisplayText)
            .withDisplayTextFormat( MidDisplayTextFormat.GSM7)
            .build();

        MidSignatureResponse response = client.getMobileIdConnector().sign(signatureRequest);

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
        String filePath;
        Signature signature;

        try {
            MidSessionStatus sessionStatus = client.getSessionStatusPoller()
                .fetchFinalSignatureSessionStatus(signingSessionInfo.getSessionID());

            MidSignature mobileIdSignature = client.createMobileIdSignature(sessionStatus);

            signature = signingSessionInfo.getDataToSign().finalize(mobileIdSignature.getValue());
            signingSessionInfo.getContainer().addSignature(signature);

            File containerFile = File.createTempFile("mid-demo-container-", ".asice");
            filePath = containerFile.getAbsolutePath();
            signingSessionInfo.getContainer().saveAsFile(filePath);

        }
        catch (MidUserCancellationException e) {
            logger.info("User cancelled operation from his/her phone.");
            throw new MidOperationException("You cancelled operation from your phone.");
        }
        catch (MidNotMidClientException e) {
            logger.info("User is not a MID client or user's certificates are revoked");
            throw new MidOperationException("You are not a Mobile-ID client or your Mobile-ID certificates are revoked. Please contact your mobile operator.");
        }
        catch (MidSessionTimeoutException e) {
            logger.info("User did not type in PIN code or communication error.");
            throw new MidOperationException("You didn't type in PIN code into your phone or there was a communication error.");
        }
        catch (MidPhoneNotAvailableException e) {
            logger.info("Unable to reach phone/SIM card. User needs to check if phone has coverage.");
            throw new MidOperationException("Unable to reach your phone. Please make sure your phone has mobile coverage.");
        }
        catch (MidDeliveryException e) {
            logger.info("Error communicating with the phone/SIM card.");
            throw new MidOperationException("Communication error. Unable to reach your phone.");
        }
        catch (MidInvalidUserConfigurationException e) {
            logger.info("Mobile-ID configuration on user's SIM card differs from what is configured on service provider side. User needs to contact his/her mobile operator.");
            throw new MidOperationException("Mobile-ID configuration on your SIM card differs from what is configured on service provider's side. Please contact your mobile operator.");
        }
        catch (MidSessionNotFoundException | MidMissingOrInvalidParameterException | MidUnauthorizedException e) {
            logger.error("Integrator-side error with MID integration or configuration", e);
            throw new MidOperationException("Client side error with mobile-ID integration.", e);
        }
        catch (MidInternalErrorException e) {
            logger.warn("MID service returned internal error that cannot be handled locally.");
            throw new MidOperationException("MID internal error", e);
        }
        catch (IOException e) {
            throw new MidOperationException("Could not create container file.", e);
        }

        return SigningResult.newBuilder()
            .withResult("Signing successful")
            .withValid(signature.validateSignature().isValid())
            .withTimestamp(signature.getTimeStampCreationTime())
            .withContainerFilePath(filePath)
            .build();

    }

}
