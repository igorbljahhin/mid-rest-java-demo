[![Build Status](https://api.travis-ci.com/SK-EID/mid-rest-java-demo.svg?branch=master)](https://travis-ci.com/SK-EID/mid-rest-java-demo)

# Mobile-ID (MID) Java Demo

Sample application to demonstrate how to implement authentication and signing a document with
[mid-rest-java-client](https://github.com/SK-EID/mid-rest-java-client) library.

## How to start application

Option 1: `mvn spring-boot:run `

Option 2. run main method of `MidRestJavaDemoApplication`


## How to use

Start the application, open [http://localhost:8081/](http://localhost:8081/)
and authenticate or sign a document using 
[test numbers](https://github.com/SK-EID/MID/wiki/Test-number-for-automated-testing-in-DEMO).

### How to run tests with a real phone

If you have Estonian or Lithuanian Mobile ID then you can run real-life tests with your
own phone if you register your Mobile ID certificates in  [SK Demo environment](https://demo.sk.ee/MIDCertsReg/)
You can also change the status of the certificates from there.

After that you should be able to log in and sign (resulting signature is not legally valid)
using your own phone number and national id code.

## Building a real-life application

For real-life use case you need to change in class `MobileIdSignatureServiceImpl` in method `sendSignatureRequest`

the following line (constructor parameter needs to be PROD):

        Configuration configuration = new Configuration(Configuration.Mode.PROD);

You also need to create your own Trust Store (or two separate Trust Stores)
and only import the certificates you trust:

  * SSL certificate of SK MID API endpoint. [More info](https://github.com/SK-EID/mid-rest-java-client#verifying-the-ssl-connection-to-application-provider-sk).
  * MID root certificates (to validate that the returned certificate is issued by SK). [More info](https://github.com/SK-EID/mid-rest-java-client#validate-returned-certificate-is-a-trusted-mid-certificate).

## Troubleshooting

### Error 'unable to find valid certification path to requested target'

This application only connects to servers it trusts. That is the SSL cert of the
server must be imported into file src/main/resources/mid.trusted_server_certs.p12.

If you change this application to connect to some other server 
(or if the SSL cert of the demo server has expired and replaced with new one)
then you need to import server's cert into the trust store.

More info how to do this can be found from [mid-rest-java-client documentation](https://github.com/SK-EID/mid-rest-java-client).

## Trust Stores information

Demo application has two separate trust stores:
 * mid.trusted_server_certs.p12 holds all SSL certificates of servers it trusts 
 * mid.trusted_root_certs.p12 holds all known MID root certificates

Following documents how these two trust stores were created.

### Trust store for SSL certificates 
 
Without following step one would able to connect to Demo API server:
 * import demo env API endpoint SSL root certificate. See [instructions how to obtain the certificate](https://github.com/SK-EID/mid-rest-java-client#how-to-obtain-server-certificate).
 * Note that for demo we import ROOT certificate (DigiCert SHA2 Secure Server CA) from the chain. Importing root certificate is not recommended for production.

        keytool -importcert -storetype PKCS12 -keystore mid.trusted_server_certs.p12 \
         -storepass changeit -alias midDemoServerRootCert -file demo_root_cert.crt -noprompt

To be able to point this application (mid-rest-java-demo) against production environment
we also import production server SSL certificate.

 * Obtain mid.sk.ee certificate. See [instructions](https://github.com/SK-EID/mid-rest-java-client#how-to-obtain-server-certificate).
 * Import it:

        keytool -importcert -file mid_sk_ee.PEM -keystore mid.trusted_server_certs.p12 \
        -storepass changeit -alias "mid.sk.ee that expires 2021.03.25"  -noprompt

### Trust store for known MID certificates

Refer to the [documentation](https://github.com/SK-EID/mid-rest-java-client#Validate-returned-certificate-is-a-trusted-MID-certificate) for more info.

First we create a trust store and import one of two test root certifices.
Without following step you couldn't log in with Estonian (+37200000766) testuser.
 * import demo env "TEST of ESTEID-SK 2015" root certificate:

        keytool -importcert -storetype PKCS12 -keystore mid.trusted_root_certs.p12 \
         -storepass changeit -alias "TEST of ESTEID-SK 2015" -file TEST_of_ESTEID-SK_2015.pem -noprompt

We also need to import a second test root certificate. 
Without following step you couldn't log in with Lithuanian (+37060000666) testuser:
 * import demo env MID 2016 root certificate:
  
        keytool -importcert -file TEST_of_EID-SK_2016.pem.crt -keystore mid.trusted_root_certs.p12 \
         -storepass changeit -alias "TEST_of_EID-SK_2016" -noprompt
 
We also need to log in with real phones that hold production certificates
 (that are copied to demo environment).
 [More info how to copy your production certificate to demo environment](https://github.com/SK-EID/mid-rest-java-client#how-to-forward-requests-to-your-phone).

        keytool -importcert -file ESTEID-SK_2011.pem.crt -keystore mid.trusted_root_certs.p12 \
         -storepass changeit -alias "ESTEID-SK_2011" -noprompt

        keytool -importcert -file ESTEID-SK_2015.pem.crt -keystore mid.trusted_root_certs.p12 \
         -storepass changeit -alias "ESTEID-SK_2015" -noprompt

If new certificates become available then these need to be imported as well.
