package com.vijrams.digsig.service

import com.vijrams.digsig.CombinedFile
import org.springframework.stereotype.Service

import java.security.PrivateKey
import java.security.PublicKey
import java.security.Signature
import java.security.KeyStore
import org.springframework.beans.factory.annotation.Value

import java.text.SimpleDateFormat

@Service
class DigitalSignerService {

    private PrivateKey pvKey
    private PublicKey pbKey
    private def certs
    private final String JKS = "datacert"
    private final String KEY = "datacert"
    private final String SIGNER_ALIAS = "wk signer"

    public DigitalSignerService(@Value('${cert.truststore}') String certKeystore) throws Exception {
        final char[] password = JKS.toCharArray()
        final char[] keyPassword = KEY.toCharArray()
        def caks = KeyStore.getInstance("jks")
        def fis = new FileInputStream(certKeystore)
        caks.load(fis, password)
        pvKey = caks.getKey(SIGNER_ALIAS, keyPassword) as PrivateKey
        pbKey = caks.getCertificate(SIGNER_ALIAS).getPublicKey()
        certs = caks.getCertificateChain(SIGNER_ALIAS)
    }

    byte[] signFile(byte[] fileData, def fileInfo, boolean oneFile) throws Exception {
        if(new String(fileData).contains("--SIGNATURE--")) throw new Exception("File is already signed")
        Signature signature = Signature.getInstance("SHA256withRSA")
        signature.initSign(pvKey)
        signature.update(fileData)
        def s = signature.sign()
        def cf = new CombinedFile(fileData, s, getCertInfo(fileInfo))
        oneFile? cf.toByteArray() : cf?.signatureAsText?.substring(2)?.getBytes()
    }

    String verifySignature(byte[] fileData, byte[] signature = null) throws Exception {
        CombinedFile cf = new CombinedFile(fileData, signature)
        Signature sig = Signature.getInstance("SHA256withRSA")
        sig.initVerify(pbKey)
        sig.update(cf.data)

        def signValid = sig.verify(cf.signature)
        def valid = signValid?"Valid":"Invalid"
        cf.signatureInfo.put("valid", signValid)
        cf.signatureInfo.put("status", "Signature ${signValid?'Valid':'Invalid'}")
        cf.signatureInfo.put("desc", "The document signature is ${signValid ? 'Valid':'Invalid'}")
        cf.signatureInfo.put("timeVerified", new SimpleDateFormat("yyyy-MM-dd\'T\'HH:mm:ssZ").format(new Date()))
        return cf.getSignatureInfoAsText()

    }

    private def getCertInfo(def sInfo = [:]){
        sInfo += ["hashAlgorithm": "SHA256", "signatureAlgorithm": "RSA", "keyLength": "2048", "validationModel": "Chain validation model", "signingPolicy": "", "validationPolicy": ""]
        try {
            sInfo.put("timeCreated", new SimpleDateFormat("yyyy-MM-dd\'T\'HH:mm:ssZ").format(new Date()))
            sInfo.put("certificate", ["Subject": certs?.first()?.subjectDN?.name, "SerialNumber": certs?.first()?.serialNumber?.toString(16)?.toUpperCase(), "ValidFrom": certs?.first()?.notBefore, "ValidTo": certs?.first()?.notAfter])
            sInfo.put("issuer", ["Subject": certs?.first()?.issuerDN?.name, "SerialNumber": certs[1]?.serialNumber?.toString(16)?.toUpperCase(), "ValidFrom": certs[1]?.notBefore, "ValidTo": certs[1]?.notAfter])
        }catch (Exception e ) {
            println e.message
        }
        return sInfo
    }

}
