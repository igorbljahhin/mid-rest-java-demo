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

import java.security.cert.X509Certificate;

import ee.sk.mid.MobileIdClient;
import ee.sk.mid.exception.MidInternalErrorException;
import ee.sk.mid.exception.MissingOrInvalidParameterException;
import ee.sk.mid.exception.NotMidClientException;
import ee.sk.mid.exception.UnauthorizedException;
import ee.sk.mid.rest.dao.request.CertificateRequest;
import ee.sk.mid.rest.dao.response.CertificateChoiceResponse;
import ee.sk.middemo.exception.MidOperationException;
import ee.sk.middemo.model.UserRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MobileIdCertificateServiceImpl implements MobileIdCertificateService {
    Logger logger = LoggerFactory.getLogger(MobileIdCertificateServiceImpl.class);

    @Autowired
    private MobileIdClient client;

    @Override
    public X509Certificate getCertificate(UserRequest userRequest) {
        CertificateRequest request = CertificateRequest.newBuilder()
                .withPhoneNumber(userRequest.getPhoneNumber())
                .withNationalIdentityNumber(userRequest.getNationalIdentityNumber())
                .build();

        try {
            CertificateChoiceResponse response = client.getMobileIdConnector().getCertificate(request);
            return client.createMobileIdCertificate(response);
        }
        catch (NotMidClientException e) {
            logger.info("User is not a MID client or user's certificates are revoked");
            throw new MidOperationException("You are not a Mobile-ID client or your Mobile-ID certificates are revoked. Please contact your mobile operator.");
        }
        catch (MissingOrInvalidParameterException | UnauthorizedException e) {
            logger.error("Integrator-side error with MID integration (including insufficient input validation) or configuration", e);
            throw new MidOperationException("Client side error with mobile-ID integration.", e);
        }
        catch (MidInternalErrorException e) {
            logger.warn("MID service returned internal error that cannot be handled locally.");
            throw new MidOperationException("MID internal error", e);
        }

    }
}
