package ee.sk.middemo;

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

import java.io.InputStream;
import java.security.KeyStore;

import ee.sk.mid.MidAuthenticationResponseValidator;
import ee.sk.mid.MidClient;
import ee.sk.middemo.model.UserMidSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.core.io.Resource;
import org.springframework.web.context.WebApplicationContext;

@Configuration
public class Config {

    @Value("${mid.client.relyingPartyUuid}")
    private String midRelyingPartyUuid;

    @Value("${mid.client.relyingPartyName}")
    private String midRelyingPartyName;

    @Value("${mid.client.applicationProviderHost}")
    private String midApplicationProviderHost;

    @Value("${mid.truststore.trusted-server-ssl-certs.filename}")
    private Resource midTrustedServerSslCertsFilename;

    @Value("${mid.truststore.trusted-server-ssl-certs.password}")
    private String midTrustedServerSslCertsPassword;

    @Value("${mid.truststore.trusted-root-certs.filename}")
    private Resource midTrustedRootCertsFilename;

    @Value("${mid.truststore.trusted-root-certs.password}")
    private String midTrustedRootCertsPassword;

    @Bean
    public MidClient mobileIdClient() throws Exception {

        InputStream is = midTrustedServerSslCertsFilename.getInputStream();
        KeyStore trustStore = KeyStore.getInstance("PKCS12");
        trustStore.load(is, midTrustedServerSslCertsPassword.toCharArray());

        return MidClient.newBuilder()
                .withRelyingPartyUUID(midRelyingPartyUuid)
                .withRelyingPartyName(midRelyingPartyName)
                .withHostUrl(midApplicationProviderHost)
                .withLongPollingTimeoutSeconds(60)
                .withTrustStore(trustStore)
                .build();
    }

    @Bean
    @Scope(value = WebApplicationContext.SCOPE_SESSION,
            proxyMode = ScopedProxyMode.TARGET_CLASS)
    public UserMidSession userSessionSigning() {
        return new UserMidSession();
    }

    @Bean
    public MidAuthenticationResponseValidator midResponseValidator() throws Exception {
        InputStream is = midTrustedRootCertsFilename.getInputStream();
        KeyStore trustStore = KeyStore.getInstance("PKCS12");
        trustStore.load(is, midTrustedRootCertsPassword.toCharArray());

        return new MidAuthenticationResponseValidator(trustStore);
    }

}
