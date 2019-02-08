package ee.sk.mid;

import ee.sk.mid.model.UserMidSession;
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
    public MobileIdClient mobileIdClient() {
        return MobileIdClient.newBuilder()
                .withRelyingPartyUUID(midRelyingPartyUuid)
                .withRelyingPartyName(midRelyingPartyName)
                .withHostUrl(midApplicationProviderHost)
                .build();
    }

    @Bean
    @Scope(value = WebApplicationContext.SCOPE_SESSION,
            proxyMode = ScopedProxyMode.TARGET_CLASS)
    public UserMidSession userSessionSigning() {
        return new UserMidSession();
    }

}
