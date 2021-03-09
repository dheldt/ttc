package de.konfidas.ttc.messages;

import de.konfidas.ttc.utilities.ByteArrayOutputStream;
import de.konfidas.ttc.TTC;
import de.konfidas.ttc.exceptions.BadFormatForLogMessageException;
import de.konfidas.ttc.utilities.oid;
import org.apache.commons.codec.binary.Hex;
import org.bouncycastle.asn1.*;

import java.io.File;
import java.io.IOException;
import java.math.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Diese Klasse repräsentiert eine LogMessage. Der Konstruktur erhält den Inhalt der LogMessage und den Dateinamen, aus der
 * die LogMessage gelesen wurde. Die LogMessage wird geparst. Dabei wird das folgende Format erwartet
 * // ╔═══════════════════════╤══════╤══════════════════════════════════╤════════════╗
 * // ║ Data field            │ Tag  │ Data Type                        │ Mandatory? ║
 * // ╠═══════════════════════╪══════╪══════════════════════════════════╪════════════╣
 * // ║ LogMessage            │ 0x30 │ SEQUENCE                         │ m          ║
 * // ╟───────────────────────┼──────┼──────────────────────────────────┼────────────╢
 * // ║    version            │ 0x02 │ INTEGER                          │ m          ║
 * // ╟───────────────────────┼──────┼──────────────────────────────────┼────────────╢
 * // ║    certifiedDataType  │ 0x06 │ OBJECT IDENTIFIER                │ m          ║
 * // ╟───────────────────────┼──────┼──────────────────────────────────┼────────────╢
 * // ║    certifiedData      │      │ ANY DEFINED BY certifiedDataType │ o          ║
 * // ╟───────────────────────┼──────┼──────────────────────────────────┼────────────╢
 * // ║    serialNumber       │ 0x04 │ OCTET STRING                     │ m          ║
 * // ╟───────────────────────┼──────┼──────────────────────────────────┼────────────╢
 * // ║    signatureAlgorithm │ 0x30 │ SEQUENCE                         │ m          ║
 * // ╟───────────────────────┼──────┼──────────────────────────────────┼────────────╢
 * // ║       algorithm       │ 0x06 │ OBJECT IDENTIFIER                │ m          ║
 * // ╟───────────────────────┼──────┼──────────────────────────────────┼────────────╢
 * // ║       parameters      │      │ ANY DEFINED BY algorithm         │ o          ║
 * // ╟───────────────────────┼──────┼──────────────────────────────────┼────────────╢
 * // ║    seAuditData        │ 0x04 │ OCTET STRING                     │ c          ║
 * // ╟───────────────────────┼──────┼──────────────────────────────────┼────────────╢
 * // ║    signatureCounter   │ 0x02 │ INTEGER                          │ c          ║
 * // ╟───────────────────────┼──────┼──────────────────────────────────┼────────────╢
 * // ║    logTime            │      │ CHOICE                           │ m          ║
 * // ╟───────────────────────┼──────┼──────────────────────────────────┼────────────╢
 * // ║       utcTime         │ 0x17 │ UTCTime                          │            ║
 * // ╟───────────────────────┼──────┼──────────────────────────────────┼────────────╢
 * // ║       generalizedTime │ 0x18 │ GeneralizedTime                  │            ║
 * // ╟───────────────────────┼──────┼──────────────────────────────────┼────────────╢
 * // ║       unixTime        │ 0x02 │ INTEGER                          │            ║
 * // ╟───────────────────────┼──────┼──────────────────────────────────┼────────────╢
 * // ║    signatureValue     │ 0x04 │ OCTET STRING                     │ m          ║
 * // ╚═══════════════════════╧══════╧══════════════════════════════════╧════════════╝
 */
public abstract class LogMessage {
    final static Logger logger = LoggerFactory.getLogger(TTC.class);

    String[] allowedCertifiedDataType = {"0.4.0.127.0.7.3.7.1.1", "0.4.0.127.0.7.3.7.1.2", "0.4.0.127.0.7.3.7.1.3"};
    String[] allowedAlgorithms = {"0.4.0.127.0.7.1.1.4.1.2", "0.4.0.127.0.7.1.1.4.1.3", "0.4.0.127.0.7.1.1.4.1.4", "0.4.0.127.0.7.1.1.4.1.5","0.4.0.127.0.7.1.1.4.1.8", "0.4.0.127.0.7.1.1.4.1.9", "0.4.0.127.0.7.1.1.4.1.10", "0.4.0.127.0.7.1.1.4.1.11","0.4.0.127.0.7.1.1.4.4.1", "0.4.0.127.0.7.1.1.4.4.2", "0.4.0.127.0.7.1.1.4.4.3", "0.4.0.127.0.7.1.1.4.4.4", "0.4.0.127.0.7.1.1.4.4.5", "0.4.0.127.0.7.1.1.4.4.6", "0.4.0.127.0.7.1.1.4.4.7", "0.4.0.127.0.7.1.1.4.4.8" };

    int version = 0;
    oid certifiedDataType;
    ArrayList<ASN1Primitive> certifiedData = new ArrayList<>();
    byte[] serialNumber;
    String signatureAlgorithm = "";
    ArrayList<ASN1Primitive> signatureAlgorithmParameters = new ArrayList<>();
    String logTimeType = "";
    String logTimeUTC = "";
    String logTimeGeneralizedTime = "";
    int logTimeUnixTime = 0;
    byte[] signatureValue = null;
      BigInteger signatureCounter = new BigInteger("5");
    byte[] seAuditData = null;
    byte[] dtbs = null;
    String filename = "";


    public LogMessage(File file) throws IOException, BadFormatForLogMessageException {
        this(Files.readAllBytes(file.toPath()), file.getName());
    }

    public LogMessage(byte[] content, String filename) throws BadFormatForLogMessageException {
        this.filename = filename;
        parse(content);
        checkContent();
    }

    /**
     * @return die toString Methode wurde überschrieben. Sie gibt den Dateinamen zurück, aus dem die LogMessage stammt
     */
    public String toString() {
        return this.filename;
    }

    public byte[] getSerialNumber(){
        return this.serialNumber;
    }

    public String getFileName(){
        return this.filename;
    }

    public byte[] getSignatureValue() {
        return this.signatureValue;
    }

    public byte[] getDTBS() {
        return this.dtbs;
    }

    public String getSignatureAlgorithm(){
        return this.signatureAlgorithm;
    }

    public int getLogTimeUnixTime() { return logTimeUnixTime; }

    public BigInteger getSignatureCounter(){ return signatureCounter; }

    void parse(byte[] content) throws LogMessageParsingException{
        try (ByteArrayOutputStream dtbsStream = new ByteArrayOutputStream()) {
            final ASN1InputStream decoder = new ASN1InputStream(content);
            ASN1Primitive primitive = decoder.readObject();

            if (primitive instanceof ASN1Sequence) {
                Enumeration<ASN1Primitive> asn1Primitives = ((ASN1Sequence) primitive).getObjects();

                ASN1Primitive element = asn1Primitives.nextElement();

                //The first element has to be the version number
                if (element instanceof ASN1Integer) {
                    this.version = ((ASN1Integer) element).intValueExact();
                    byte[] elementValue = Arrays.copyOfRange(element.getEncoded(), 2, element.getEncoded().length);
                    dtbsStream.write(elementValue);
                }
                else {
                    throw new LogMessageParsingException("Das version Element in der logMessage konnte nicht gefunden werden.");
                }

                parseCertifiedDataType(dtbsStream, asn1Primitives);
                element = parseCertifiedData(dtbsStream, asn1Primitives); // TODO: CertifiedData ends with optional element. So parseCertifiedData fetches one element to much.
                parseSerialNumber(dtbsStream,element);

                element = asn1Primitives.nextElement();
                // Then, the sequence for the signatureAlgorithm  is expected
                if (element instanceof ASN1Sequence) {

                    Enumeration<ASN1Primitive> sigAlgorithmEnumeration = ((ASN1Sequence) element).getObjects();

                    element = sigAlgorithmEnumeration.nextElement();
                    // First, we read the signatureAlgorithm itself

                    if (element instanceof ASN1ObjectIdentifier) {
                        this.signatureAlgorithm = element.toString();
                        byte[] elementValue = Arrays.copyOfRange(element.getEncoded(), 2, element.getEncoded().length);
                        dtbsStream.write(elementValue);
                    }
                    else {
                        throw new LogMessageParsingException("signatureAlgorithm wurde nicht gefunden.");
                    }

                    if (!Arrays.asList(allowedAlgorithms).contains(this.signatureAlgorithm)) {
                        throw new LogMessageParsingException(String.format("Die OID für signatureAlgorithm lautet %s. Dies ist keine erlaubte OID", this.signatureAlgorithm));
                    }


                    //Then, we loop over the rest of the sequence for the options
                    while (sigAlgorithmEnumeration.hasMoreElements()) {
                        element = sigAlgorithmEnumeration.nextElement();
                        this.signatureAlgorithmParameters.add(element);
                    }

                }
                else {
                    throw new LogMessageParsingException("Die Sequenz für den signatureAlgortihm wurde nicht gefunden.");
                }

                element = asn1Primitives.nextElement();
                // Then, we are checking whether we have seAuditData

                if (element instanceof ASN1OctetString) {
                    this.seAuditData = ((ASN1OctetString) element).getOctets();
                    byte[] elementValue = Arrays.copyOfRange(element.getEncoded(), 2, element.getEncoded().length);
                    dtbsStream.write(elementValue);
                    element = asn1Primitives.nextElement();

                } else {
                    logger.info(String.format("Information für %s. seAuditData wurde nicht gefunden.", filename));
                }

                // Then, we are checking whether we have signatureCounter
                if(! asn1Primitives.hasMoreElements()){
                    throw new LogMessageParsingException("No More Elements, signature missing!");
                }
                ASN1Primitive nextElement = asn1Primitives.nextElement();

                boolean hasSignatureCounter = false;
                // check if nextElement is logTime. If not, element has to be time and no signature counter is present:
                if(nextElement instanceof  ASN1Integer || nextElement instanceof ASN1UTCTime || nextElement instanceof ASN1GeneralizedTime) {
                    hasSignatureCounter = true;
                    parseSignatureCounter(dtbsStream, element);
                }

                if(hasSignatureCounter){
                    parseTime(dtbsStream, nextElement);
                    element = asn1Primitives.nextElement();
                }else {
                    parseTime(dtbsStream, element);
                    element = nextElement;
                }

                // Now, the last element shall be the signature
                if (element instanceof ASN1OctetString) {
                    this.signatureValue = Arrays.copyOfRange(element.getEncoded(), 2, element.getEncoded().length);
                }
                else {
                    throw new LogMessageParsingException("signature wurde nicht gefunden.");
                }

                //Speichern des DTBS aus dem BufferedWriter
                this.dtbs = dtbsStream.toByteArray();
            }
        }catch(IOException e){
            throw new LogMessageParsingException("failed to parse log message",e);
        }
    }

    private void parseSignatureCounter(ByteArrayOutputStream dtbsStream, ASN1Primitive element) throws LogMessageParsingException, IOException {
        if (!(element instanceof ASN1Integer)) {
            throw new LogMessageParsingException("SignatureCounter has to be ASN1Integer, but is " + element.getClass());
        }
        this.signatureCounter = ((ASN1Integer) element).getValue();
        byte[] elementValue = Arrays.copyOfRange(element.getEncoded(), 2, element.getEncoded().length); // TODO: fails on extended length.
        dtbsStream.write(elementValue);

        if (signatureCounter == null) {
            throw new LogMessageParsingException("SignatureCounter is missing.");
        }
    }

    private void parseTime(ByteArrayOutputStream dtbsStream, ASN1Primitive element) throws IOException, LogMessageParsingException {
        // Now, we expect the logTime as one of three typey
        if (element instanceof ASN1Integer) {
            this.logTimeUnixTime = ((ASN1Integer) element).getValue().intValue();
            byte[] elementValue = Arrays.copyOfRange(element.getEncoded(), 2, element.getEncoded().length);
            dtbsStream.write(elementValue);
            this.logTimeType = "unixTime";
        }
        else if (element instanceof ASN1UTCTime) {
            this.logTimeUTC = ((ASN1UTCTime) element).getTime();
            byte[] elementValue = Arrays.copyOfRange(element.getEncoded(), 2, element.getEncoded().length);
            dtbsStream.write(elementValue);
            this.logTimeType = "utcTime";
        }
        else if (element instanceof ASN1GeneralizedTime) {
            this.logTimeGeneralizedTime = ((ASN1GeneralizedTime) element).getTime();
            byte[] elementValue = Arrays.copyOfRange(element.getEncoded(), 2, element.getEncoded().length);
            dtbsStream.write(elementValue);
            this.logTimeType = "generalizedTime";
        }
        else {
            throw new LogMessageParsingException("logTime Element wurde nicht gefunden.");
        }
    }

    private void parseSerialNumber(ByteArrayOutputStream dtbsStream,ASN1Primitive element) throws IOException, SerialNumberParsingException {
        if(element == null){
            throw new SerialNumberParsingException("Failed to Parse Certified Data Type, no more elements in ASN1 Object", null);
        }

        // Then, the serial number is expected
        if (element instanceof ASN1OctetString) {
            this.serialNumber = ((ASN1OctetString) element).getOctets();
            dtbsStream.write(this.serialNumber);
        } else {
            throw new SerialNumberParsingException(String.format("Fehler beim Parsen von %s. serialNumber wurde nicht gefunden.", filename),null);
        }
    }

    void parseCertifiedDataType(ByteArrayOutputStream dtbsStream, Enumeration<ASN1Primitive> asn1Primitives) throws IOException, CertifiedDataTypeParsingException {
        if(!asn1Primitives.hasMoreElements()){
            throw new CertifiedDataTypeParsingException("Failed to Parse Certified Data Type, no more elements in ASN1 Object", null);
        }
        ASN1Primitive element = asn1Primitives.nextElement();

        // Then, the object identifier for the certified data type shall follow
        if (element instanceof ASN1ObjectIdentifier) {

            byte[] elementValue = Arrays.copyOfRange(element.getEncoded(), 2, element.getEncoded().length);

            try {
                this.certifiedDataType = oid.fromBytes(element.getEncoded());
            }
            catch (oid.UnknownOidException e) {
                throw new CertifiedDataTypeParsingException("OID unknown",e);
            }

            dtbsStream.write(elementValue);
        } else {
            throw new CertifiedDataTypeParsingException(String.format("Fehler beim Parsen von %s. certifiedDataType Element wurde nicht gefunden.", filename), null);
        }
    }


    abstract ASN1Primitive parseCertifiedData(ByteArrayOutputStream dtbsStream, Enumeration<ASN1Primitive> test) throws IOException, LogMessageParsingException;


    void checkContent() throws LogMessageParsingException {
        // Die Versionsnummer muss 2 sein
        if (this.version != 2) {
            throw new LogMessageParsingException("Die Versionsnummer ist nicht 2");
        }

        // TODO: no longer required here. Done as part of parsing.
        // Prüfen, dass der certifiedDataType ein erlaubter Wert ist
        if (!Arrays.asList(allowedCertifiedDataType).contains(this.certifiedDataType.getReadable())) {
            throw new LogMessageParsingException(String.format("Der Wert von certifiedDataType ist nicht erlaubt. er lautet %s", this.certifiedDataType));
        }

        // Prüfen, dass die Serial Number auch da ist.
        if (this.serialNumber == null) {
            throw new LogMessageParsingException("Die Serial Number ist null");
        }

        if (logTimeType == null) {
            throw new LogMessageParsingException("Es ist kein Typ für die LogZeit vorhanden");
        }
    }

    public class LogMessageParsingException extends BadFormatForLogMessageException{
        public LogMessageParsingException(String message) {
            super("Parsing Message "+filename+" failed: "+ message, null);
        }

        public LogMessageParsingException(String message, Exception reason) {
            super("Parsing Message "+filename+" failed: "+ message, reason);
        }
    }

    public class CertifiedDataTypeParsingException extends LogMessageParsingException{
        public CertifiedDataTypeParsingException(String message, Exception reason) {
            super(message, reason);
        }
    }

    public class SerialNumberParsingException extends LogMessageParsingException{
        public SerialNumberParsingException(String message, Exception reason) {
            super(message, reason);
        }
    }
}

