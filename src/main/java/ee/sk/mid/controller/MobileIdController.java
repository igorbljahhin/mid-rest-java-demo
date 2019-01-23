package ee.sk.mid.controller;

import ee.sk.mid.model.UserRequest;
import ee.sk.mid.services.MobileIdAuthenticationService;
import ee.sk.mid.services.MobileIdSignatureService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;

@RestController
public class MobileIdController {

    private MobileIdSignatureService signatureService;
    private MobileIdAuthenticationService authenticationService;

    @Autowired
    public MobileIdController(MobileIdSignatureService signatureService, MobileIdAuthenticationService authenticationService) {
        this.signatureService = signatureService;
        this.authenticationService = authenticationService;
    }

    @GetMapping(value = "/")
    public ModelAndView userRequestForm() {
        return new ModelAndView("index", "userRequest", new UserRequest());
    }

    @PostMapping(value = "/sign")
    public ModelAndView sign(@ModelAttribute("userRequest") UserRequest userRequest, ModelMap model) {
        List<String> result = signatureService.sign(userRequest);
        if (result.size() > 1) {
            model.addAttribute("result", result.get(0));
            model.addAttribute("isValid", result.get(1));
            model.addAttribute("timestamp", result.get(2));
        } else {
            model.addAttribute("result", result.get(0));
        }
        return new ModelAndView("/response", model);
    }

    @PostMapping(value = "/authenticate")
    public ModelAndView authenticate(@ModelAttribute("userRequest") UserRequest userRequest, ModelMap model) {
        String result = authenticationService.authenticate(userRequest);
        model.addAttribute("result", result);
        return new ModelAndView("/response", model);
    }
}
