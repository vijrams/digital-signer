package com.vijrams.digsig
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper

class CombinedFile {
    private byte[] data
    private byte[] signature
    private def signatureInfo
    private String SEPARATOR = "\n\n--SIGNATURE--\n"

    public CombinedFile(byte[] data, byte[] signature, def signatureInfo) {
        this.data = data
        this.signature = signature
        this.signatureInfo = signatureInfo
    }

    public CombinedFile(byte[] data, byte[] sign = null) {
        this.data = data
        if(sign == null) {
            String combinedContent = new String(data)
            String[] parts = combinedContent.split(SEPARATOR)
            if(parts.length != 2) throw new Exception("File is wrong format or not signed by this service")
            this.data = parts[0].getBytes()
            sign = parts[1]
        } else {
            sign = new String(sign).replaceAll("--SIGNATURE--", "").getBytes()
        }
        def sParts = new String(sign).replaceAll("\\s", "")
        if(sParts.contains("--SIGNATURE--") || !sParts.contains("@!")) throw new Exception("Signature is wrong format or not signed by this service")
        this.signature = Base64.getDecoder().decode(sParts.split('@!')[0])
        this.signatureInfo = new JsonSlurper().parseText(new String(Base64.getDecoder().decode(sParts.split('@!')[1])))
    }

    public byte[] getData() {
        return data
    }

    public String getDataAsText() {
        return new String(data)
    }

    public byte[] getSignature() {
        return signature
    }

    public String getSignatureAsText() {
        return base64Encode()
    }

    public def getSignatureInfo() {
        return signatureInfo
    }

    public String getSignatureInfoAsText() {
        return new JsonBuilder(signatureInfo).toPrettyString()
    }

    public String toString() {
        return getDataAsText()  + getSignatureAsText()
    }

    public byte[] toByteArray() {
        return (getDataAsText()  + getSignatureAsText()).getBytes()
    }

    private String base64Encode() {
        def b64Signature = Base64.getEncoder().encodeToString(signature)
        def b64SignatureInfo = Base64.getEncoder().encodeToString(getSignatureInfoAsText().getBytes())
        return SEPARATOR  + (b64Signature + '@!' + b64SignatureInfo).toList().collate(76)*.join('').join('\n')
    }

}
