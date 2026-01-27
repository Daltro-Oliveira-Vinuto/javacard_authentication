package card;

import javacard.framework.*;
import javacard.security.*;

public class AuthApplet extends Applet {
    private static final byte INS_SETUP_KEY = (byte) 0x10;
    private static final byte INS_SIGN      = (byte) 0x20;

    private ECPrivateKey privateKey;
    private Signature signature;
    private boolean isKeySetup = false;

    // --- PARÂMETROS DA CURVA NIST P-256 (secp256r1) ---
    static final byte[] SEC_P = {
        (byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x01,
        (byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,
        (byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,
        (byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF
    };
    static final byte[] SEC_A = {
        (byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x01,
        (byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,
        (byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,
        (byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFC
    };
    static final byte[] SEC_B = {
        (byte)0x5A,(byte)0xC6,(byte)0x35,(byte)0xD8,(byte)0xAA,(byte)0x3A,(byte)0x93,(byte)0xE7,
        (byte)0xB3,(byte)0xEB,(byte)0xBD,(byte)0x55,(byte)0x76,(byte)0x98,(byte)0x86,(byte)0xBC,
        (byte)0x65,(byte)0x1D,(byte)0x06,(byte)0xB0,(byte)0xCC,(byte)0x53,(byte)0xB0,(byte)0xF6,
        (byte)0x3B,(byte)0xCE,(byte)0x3C,(byte)0x3E,(byte)0x27,(byte)0xD2,(byte)0x60,(byte)0x4B
    };
    static final byte[] SEC_G = {
        (byte)0x04, // Uncompressed
        (byte)0x6B,(byte)0x17,(byte)0xD1,(byte)0xF2,(byte)0xE1,(byte)0x2C,(byte)0x42,(byte)0x47,
        (byte)0xF8,(byte)0xBC,(byte)0xE6,(byte)0xE5,(byte)0x63,(byte)0xA4,(byte)0x40,(byte)0xF2,
        (byte)0x77,(byte)0x03,(byte)0x7D,(byte)0x81,(byte)0x2D,(byte)0xEB,(byte)0x33,(byte)0xA0,
        (byte)0xF4,(byte)0xA1,(byte)0x39,(byte)0x45,(byte)0xD8,(byte)0x98,(byte)0xC2,(byte)0x96,
        (byte)0x4F,(byte)0xE3,(byte)0x42,(byte)0xE2,(byte)0xFE,(byte)0x1A,(byte)0x7F,(byte)0x9B,
        (byte)0x8E,(byte)0xE7,(byte)0xEB,(byte)0x4A,(byte)0x7C,(byte)0x0F,(byte)0x9E,(byte)0x16,
        (byte)0x2B,(byte)0xCE,(byte)0x33,(byte)0x57,(byte)0x6B,(byte)0x31,(byte)0x5E,(byte)0xCE,
        (byte)0xCB,(byte)0xB6,(byte)0x40,(byte)0x68,(byte)0x37,(byte)0xBF,(byte)0x51,(byte)0xF5
    };
    static final byte[] SEC_R = {
        (byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,
        (byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,
        (byte)0xBC,(byte)0xE6,(byte)0xFA,(byte)0xAD,(byte)0xA7,(byte)0x17,(byte)0x9E,(byte)0x84,
        (byte)0xF3,(byte)0xB9,(byte)0xCA,(byte)0xC2,(byte)0xFC,(byte)0x63,(byte)0x25,(byte)0x51
    };
    
    static final byte k = 0x01;

    public AuthApplet() {
        privateKey = (ECPrivateKey) KeyBuilder.buildKey(KeyBuilder.TYPE_EC_FP_PRIVATE, KeyBuilder.LENGTH_EC_FP_256, false);
        
        // Configura parâmetros da curva
        privateKey.setFieldFP(SEC_P, (short)0, (short)SEC_P.length);
        privateKey.setA(SEC_A, (short)0, (short)SEC_A.length);
        privateKey.setB(SEC_B, (short)0, (short)SEC_B.length);
        privateKey.setG(SEC_G, (short)0, (short)SEC_G.length);
        privateKey.setR(SEC_R, (short)0, (short)SEC_R.length);
        privateKey.setK(k);

        signature = Signature.getInstance(Signature.ALG_ECDSA_SHA_256, false);
    }

    public static void install(byte[] bArray, short bOffset, byte bLength) {
        new AuthApplet().register();
    }

    public void process(APDU apdu) {
        if (selectingApplet()) return;

        byte[] buffer = apdu.getBuffer();
        byte ins = buffer[ISO7816.OFFSET_INS];

        if (ins == INS_SETUP_KEY) {
            if (isKeySetup) ISOException.throwIt(ISO7816.SW_CONDITIONS_NOT_SATISFIED);
            short len = apdu.setIncomingAndReceive();
            if (len != 32) ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);

            privateKey.setS(buffer, ISO7816.OFFSET_CDATA, len);
            isKeySetup = true;
        } 
        else if (ins == INS_SIGN) {
            if (!isKeySetup) ISOException.throwIt(ISO7816.SW_CONDITIONS_NOT_SATISFIED);

            short dataLen = apdu.setIncomingAndReceive();
            
            try {
                signature.init(privateKey, Signature.MODE_SIGN);
                short sigLen = signature.sign(buffer, ISO7816.OFFSET_CDATA, dataLen, buffer, (short) 0);
                apdu.setOutgoingAndSend((short) 0, sigLen);
            } catch (CryptoException e) {
                // CORREÇÃO: Usamos SW_UNKNOWN (0x6F00) em vez de 0x9000
                // 0x6F00 cabe num short, evitando o erro de "unsupported int type"
                ISOException.throwIt((short) (ISO7816.SW_UNKNOWN + e.getReason()));
            }
        } 
        else {
            ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
        }
    }
}



