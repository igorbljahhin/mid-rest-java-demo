package ee.sk.mid.model;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.io.File;

public class UserRequest {

    private String phoneNumber;

    @NotNull
    @Pattern(regexp="[0-9]{11}",
            message="Invalid national identity number")
    private String nationalIdentityNumber;
    private File file;

    @NotNull
    @Pattern(regexp="\\+37[0-9]{5,10}",
            message="Invalid phone number")
    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getNationalIdentityNumber() {
        return nationalIdentityNumber;
    }

    public void setNationalIdentityNumber(String nationalIdentityNumber) {
        this.nationalIdentityNumber = nationalIdentityNumber;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }
}
