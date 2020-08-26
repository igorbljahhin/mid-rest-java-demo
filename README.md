# Mobile-ID (MID) Java Demo

Sample application to demonstrate how to implement authentication and signing a document with Java.

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

## Troubleshooting

### Error 'unable to find valid certification path to requested target'

This application only connects to servers it trusts. That is the SSL cert of the
server must be imported into file src/main/resources/trusted_server_certs.p12.

If you change this application to connect to some other server 
(or if the SSL cert of the demo server has expired and replaced with new one)
then you need to import server's cert into the trust store.

More info how to do this can be found from [mid-rest-java-client documentation](https://github.com/SK-EID/mid-rest-java-client).
