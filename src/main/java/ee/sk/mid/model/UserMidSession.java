package ee.sk.mid.model;

public class UserMidSession {
    private SigningSessionInfo signingSessionInfo;
    private AuthenticationSessionInfo authenticationSessionInfo;

    public SigningSessionInfo getSigningSessionInfo() {
        return signingSessionInfo;
    }

    public void setSigningSessionInfo(SigningSessionInfo signingSessionInfo) {
        this.signingSessionInfo = signingSessionInfo;
    }

    public AuthenticationSessionInfo getAuthenticationSessionInfo() {
        return authenticationSessionInfo;
    }

    public void setAuthenticationSessionInfo(AuthenticationSessionInfo authenticationSessionInfo) {
        this.authenticationSessionInfo = authenticationSessionInfo;
    }

    public void clearSigningSession() {
        this.signingSessionInfo = null;
    }

    public void clearAuthenticationSessionInfo() {
        this.authenticationSessionInfo = null;
    }

}
