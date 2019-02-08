package ee.sk.mid.controller;

import ee.sk.mid.AuthenticationIdentity;
import ee.sk.mid.exception.FileUploadException;
import ee.sk.mid.exception.MidAuthException;
import ee.sk.mid.model.*;
import ee.sk.mid.services.MobileIdAuthenticationService;
import ee.sk.mid.services.MobileIdSignatureService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.validation.Valid;

@RestController
public class MobileIdController {
    private MobileIdSignatureService signatureService;
    private MobileIdAuthenticationService authenticationService;

    private UserMidSession userMidSession;

    @Autowired
    public MobileIdController(MobileIdSignatureService signatureService, MobileIdAuthenticationService authenticationService, UserMidSession userMidSession) {
        this.signatureService = signatureService;
        this.authenticationService = authenticationService;
        this.userMidSession = userMidSession; // session scope, autowired
    }

    @GetMapping(value = "/")
    public ModelAndView userRequestForm() {
        return new ModelAndView("index", "userRequest", new UserRequest());
    }

    @PostMapping(value = "/signatureRequest")
    public ModelAndView sendSignatureRequest(@ModelAttribute("userRequest") UserRequest userRequest,
                                             BindingResult bindingResult, ModelMap model) {

        if (userRequest.getFile() == null || userRequest.getFile().getOriginalFilename() == null || userRequest.getFile().isEmpty()) {
            bindingResult.rejectValue("file", "error.file", "Please select a file to upload");
        }

        if (bindingResult.hasErrors()) {
            return new ModelAndView("index", "userRequest", userRequest);
        }

        SigningSessionInfo signingSessionInfo = signatureService.sendSignatureRequest(userRequest);

        userMidSession.setSigningSessionInfo(signingSessionInfo);

        model.addAttribute("signingSessionInfo", signingSessionInfo);

        return new ModelAndView("/signature", model);
    }

    @PostMapping(value = "/sign")
    public ModelAndView sign(ModelMap model) {

        SigningResult signingResult = signatureService.sign(userMidSession.getSigningSessionInfo());

        userMidSession.clearSigningSession();

        model.addAttribute("signingResult", signingResult);

        return new ModelAndView("signingResult", model);
    }

    @PostMapping(value = "/authenticationRequest")
    public ModelAndView sendAuthenticationRequest(@ModelAttribute("userRequest") @Valid UserRequest userRequest,
                                                  BindingResult bindingResult, ModelMap model) {

        if (bindingResult.hasErrors()) {
            System.out.println("Input validation error");
            return new ModelAndView("index", "userRequest", userRequest);
        }

        AuthenticationSessionInfo authenticationSessionInfo = authenticationService.startAuthentication(userRequest);
        userMidSession.setAuthenticationSessionInfo(authenticationSessionInfo);

        model.addAttribute("verificationCode", authenticationSessionInfo.getVerificationCode());

        return new ModelAndView("/authentication", model);
    }

    @PostMapping(value = "/authenticate")
    public ModelAndView authenticate(ModelMap model) {
        AuthenticationIdentity person = authenticationService.authenticate(userMidSession.getAuthenticationSessionInfo());
        model.addAttribute("person", person);

        userMidSession.clearAuthenticationSessionInfo();

        return new ModelAndView("authenticationResult", model);
    }

    @ExceptionHandler(FileUploadException.class)
    public String handleStorageFileNotFound(FileUploadException exc) {
        return "error";
    }


    @ExceptionHandler(MidAuthException.class)
    public ModelAndView handleException(MidAuthException exception) {

        ModelMap model = new ModelMap();

        model.addAttribute("errorMessage", exception.getMessage());

        return new ModelAndView("authenticationError", model);
    }
}
