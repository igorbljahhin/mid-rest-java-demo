package ee.sk.mid.controller;

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

import javax.validation.Valid;

import ee.sk.mid.AuthenticationIdentity;
import ee.sk.mid.exception.FileUploadException;
import ee.sk.mid.exception.MidAuthException;
import ee.sk.mid.model.AuthenticationSessionInfo;
import ee.sk.mid.model.SigningResult;
import ee.sk.mid.model.SigningSessionInfo;
import ee.sk.mid.model.UserMidSession;
import ee.sk.mid.model.UserRequest;
import ee.sk.mid.services.MobileIdAuthenticationService;
import ee.sk.mid.services.MobileIdSignatureService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

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
