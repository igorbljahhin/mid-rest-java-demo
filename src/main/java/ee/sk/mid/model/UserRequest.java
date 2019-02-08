package ee.sk.mid.model;

import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

public class UserRequest {

    private String phoneNumber;

    @NotNull
    @Pattern(regexp="[0-9]{11}",
            message="Invalid national identity number")
    private String nationalIdentityNumber;

    @NotNull
    @Pattern(regexp="\\+37[0-9]{5,10}",
            message="Invalid phone number")
    public String getPhoneNumber() {
        return phoneNumber;
    }

    private MultipartFile file;

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getNationalIdentityNumber() {
        return nationalIdentityNumber;
    }

    public void setNationalIdentityNumber(String nationalIdentityNumber) {
        this.nationalIdentityNumber = nationalIdentityNumber;
    }

    public MultipartFile getFile() {
        return file;
    }

    public void setFile(MultipartFile file) {
        this.file = file;
    }
}
