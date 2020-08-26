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

import ee.sk.mid.MidClient;
import ee.sk.middemo.model.UserMidSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.web.context.WebApplicationContext;

@Configuration
public class Config {

    @Value("${mid.client.relyingPartyUuid}")
    private String midRelyingPartyUuid;

    @Value("${mid.client.relyingPartyName}")
    private String midRelyingPartyName;

    @Value("${mid.client.applicationProviderHost}")
    private String midApplicationProviderHost;

    @Bean
    public MidClient mobileIdClient() throws Exception {

        InputStream is = Config.class.getResourceAsStream("/trusted_server_certs.p12");
        KeyStore trustStore = KeyStore.getInstance("PKCS12");
        trustStore.load(is, "changeit".toCharArray());

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

}
