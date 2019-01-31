package ee.sk.mid.controller;

import ee.sk.mid.MobileIdAuthenticationHash;
import ee.sk.mid.model.UserRequest;
import ee.sk.mid.services.MobileIdAuthenticationService;
import ee.sk.mid.services.MobileIdSignatureService;
import eu.europa.esig.dss.MimeType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import javax.validation.Valid;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

@RestController
public class MobileIdController {

    private MobileIdSignatureService signatureService;
    private MobileIdAuthenticationService authenticationService;

    private MobileIdAuthenticationHash authenticationHash;

    @Autowired
    public MobileIdController(MobileIdSignatureService signatureService, MobileIdAuthenticationService authenticationService) {
        this.signatureService = signatureService;
        this.authenticationService = authenticationService;
    }

    @GetMapping(value = "/")
    public ModelAndView userRequestForm() {
        return new ModelAndView("index", "userRequest", new UserRequest());
    }

    @PostMapping(value = "/signatureRequest")
    public ModelAndView sendSignatureRequest(@ModelAttribute("userRequest") UserRequest userRequest,
                                             @RequestParam("file") MultipartFile file, BindingResult bindingResult, ModelMap model) {

        if (bindingResult.hasErrors()) {
            System.out.println("Input validation error");
            return new ModelAndView("index", "userRequest", userRequest);
        }

        if (file == null || file.getOriginalFilename() == null || file.isEmpty()) {
            System.out.println("Please select a file to upload");
            return new ModelAndView("index", "userRequest", userRequest);
        }

        String filePath;
        MimeType mimeType;
        try {
            byte[] bytes = file.getBytes();
            filePath = "../mid-rest-java-demo/src/main/resources/static/" + file.getOriginalFilename();
            mimeType = MimeType.fromFileName(file.getOriginalFilename());
            Path path = Paths.get(filePath);
            Files.write(path, bytes);
        } catch (IOException e) {
            System.out.println("File reading error");
            return new ModelAndView("index", "userRequest", userRequest);
        }

        String result = signatureService.sign(userRequest, filePath, mimeType);

        if (result.length() > 4) {
            model.addAttribute("result", result);
            return new ModelAndView("/response", model);
        } else {
            String verificationCode = "Your control code is: " + result;
            model.addAttribute("verificationCode", verificationCode);
            return new ModelAndView("/signature", model);
        }
    }

    @PostMapping(value = "/sign")
    public ModelAndView sign(ModelMap model) {
        Map<String, String> result = signatureService.sign();

        if (result.containsKey("isValid")) {
            model.addAttribute("result", result.get("result"));
            model.addAttribute("isValid", result.get("isValid"));
            model.addAttribute("timestamp", result.get("timestamp"));
            model.addAttribute("filename", result.get("filename"));
        } else {
            model.addAttribute("result", result.get("result"));
        }
        return new ModelAndView("/response", model);
    }

    @PostMapping(value = "/authenticationRequest")
    public ModelAndView sendAuthenticationRequest(@ModelAttribute("userRequest") @Valid UserRequest userRequest,
                                                  BindingResult bindingResult, ModelMap model) {

        if (bindingResult.hasErrors()) {
            System.out.println("Input validation error");
            return new ModelAndView("index", "userRequest", userRequest);
        }

        authenticationHash = authenticationService.authenticate(userRequest);
        String verificationCode = "Control code: " + authenticationHash.calculateVerificationCode();

        model.addAttribute("verificationCode", verificationCode);

        return new ModelAndView("/authentication", model);
    }

    @PostMapping(value = "/authenticate")
    public ModelAndView authenticate(ModelMap model) {
        String result = authenticationService.authenticate(authenticationHash);
        model.addAttribute("result", result);
        return new ModelAndView("/response", model);
    }
}
