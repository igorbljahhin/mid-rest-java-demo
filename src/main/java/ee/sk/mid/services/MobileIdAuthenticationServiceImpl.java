package ee.sk.mid.services;

import ee.sk.mid.Language;
import ee.sk.mid.MobileIdAuthentication;
import ee.sk.mid.MobileIdAuthenticationHash;
import ee.sk.mid.MobileIdClient;
import ee.sk.mid.exception.*;
import ee.sk.mid.model.UserRequest;
import ee.sk.mid.rest.dao.SessionStatus;
import ee.sk.mid.rest.dao.request.AuthenticationRequest;
import ee.sk.mid.rest.dao.response.AuthenticationResponse;
import org.springframework.stereotype.Service;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;

@Service
public class MobileIdAuthenticationServiceImpl implements MobileIdAuthenticationService {

    public MobileIdAuthenticationServiceImpl() {
    }

    @Override
    public String authenticate(UserRequest userRequest) {
        MobileIdClient client = MobileIdClient.newBuilder()
                .withRelyingPartyUUID("00000000-0000-0000-0000-000000000000")
                .withRelyingPartyName("DEMO")
                .withHostUrl("https://tsp.demo.sk.ee")
                .build();

        MobileIdAuthenticationHash authenticationHash = MobileIdAuthenticationHash.generateRandomHashOfDefaultType();

        String verificationCode = authenticationHash.calculateVerificationCode();

        AuthenticationRequest request = AuthenticationRequest.newBuilder()
                .withRelyingPartyUUID(client.getRelyingPartyUUID())
                .withRelyingPartyName(client.getRelyingPartyName())
                .withPhoneNumber(userRequest.getPhoneNumber())
                .withNationalIdentityNumber(userRequest.getNationalIdentityNumber())
                .withAuthenticationHash(authenticationHash)
                .withLanguage(Language.ENG)
                .build();

        try {
            AuthenticationResponse response = client.getMobileIdConnector().authenticate(request);
            SessionStatus sessionStatus = client.getSessionStatusPoller().fetchFinalSessionStatus(response.getSessionID(),
                    "/mid-api/authentication/session/{sessionId}");
            MobileIdAuthentication authentication = client.createMobileIdAuthentication(sessionStatus, authenticationHash.getHashInBase64(),
                    authenticationHash.getHashType());
        } catch (ParameterMissingException e) {
            return "Input parameters are missing";
        } catch (InternalServerErrorException | ResponseRetrievingException e) {
            return "Error getting response from cert-store/MSSP";
        } catch (NotFoundException e) {
            return "Response not found ";
        } catch (BadRequestException e) {
            return "Request is invalid";
        } catch (NotAuthorizedException e) {
            return "Request is unauthorized" ;
        } catch (SessionTimeoutException e) {
            return "Session timeout";
        } catch (NotMIDClientException e) {
            return "Given user has no active certificates and is not M-ID client";
        } catch (ExpiredException e) {
            return "MSSP transaction timed out";
        } catch (UserCancellationException e) {
            return "User cancelled the operation";
        } catch (MIDNotReadyException e) {
            return "Mobile-ID not ready";
        } catch (SimNotAvailableException e) {
            return "Sim not available";
        } catch (DeliveryException e) {
            return "SMS sending error";
        } catch (InvalidCardResponseException e) {
            return "Invalid response from card";
        } catch (SignatureHashMismatchException e) {
            return "Hash does not match with certificate type";
        } catch (RuntimeException e) {
            return e.getMessage();
        }

        return "Authentication successful";
    }
}
