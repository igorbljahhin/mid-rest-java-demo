package ee.sk.mid.model;

import java.util.Date;

public class SigningResult {

    private String result;
    private Boolean valid;
    private Date timestamp;
    private String containerFilePath;

    private SigningResult(Builder builder) {
        this.result = builder.result;
        this.valid = builder.valid;
        this.timestamp = builder.timestamp;
        this.containerFilePath = builder.containerFilePath;
    }

    public String getResult() {
        return result;
    }

    public Boolean getValid() {
        return valid;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public String getContainerFilePath() {
        return containerFilePath;
    }

    public static Builder newBuilder() {
        return new Builder();
    }


    public static class Builder {
        private String result;
        private Boolean valid;
        private Date timestamp;
        private String containerFilePath;

        public Builder withResult(String result) {
            this.result = result;
            return this;
        }

        public Builder withValid(Boolean valid) {
            this.valid = valid;
            return this;
        }

        public Builder withTimestamp(Date timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder withContainerFilePath(String containerFilePath) {
            this.containerFilePath = containerFilePath;
            return this;
        }

        public SigningResult build() {
            return new SigningResult(this);
        }
    }

}
