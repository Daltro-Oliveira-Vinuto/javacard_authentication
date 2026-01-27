package card;

import javacard.framework.*;
import javacard.security.*;

public class AuthApplet extends Applet {
    // Instruções
    private static final byte INS_SETUP_KEY  = (byte) 0x10; // Grava chave (SOMENTE 1 VEZ)
    private static final byte INS_SETUP_USER = (byte) 0x11; // Grava usuário (SOMENTE 1 VEZ)
    private static final byte INS_SIGN_VERIFY= (byte) 0x20; // Assina

    private ECPrivateKey privateKey;
    private Signature signature;
    
    // Buffer para guardar o nome do usuário
    private byte[] userID;
    private short userIDLen;

    // Flags de segurança (persistem na EEPROM)
    private boolean isKeySetup = false;
    private boolean isUserSetup = false;

    public AuthApplet() {
        // Aloca memória persistente
        privateKey = (ECPrivateKey) KeyBuilder.buildKey(KeyBuilder.TYPE_EC_FP_PRIVATE, KeyBuilder.LENGTH_EC_FP_256, false);
        signature = Signature.getInstance(Signature.ALG_ECDSA_SHA_256, false);
        userID = new byte[20]; // Max 20 caracteres para ID
    }

    public static void install(byte[] bArray, short bOffset, byte bLength) {
        new AuthApplet().register();
    }

    public void process(APDU apdu) {
        if (selectingApplet()) return;
        byte[] buffer = apdu.getBuffer();

        switch (buffer[ISO7816.OFFSET_INS]) {
            case INS_SETUP_KEY:
                // SE JÁ TIVER CHAVE, BLOQUEIA (PROTEÇÃO CONTRA RE-GRAVAÇÃO)
                if (isKeySetup) ISOException.throwIt(ISO7816.SW_CONDITIONS_NOT_SATISFIED);

                short len = apdu.setIncomingAndReceive();
                privateKey.setS(buffer, ISO7816.OFFSET_CDATA, len);
                isKeySetup = true; // Trava para sempre
                break;

            case INS_SETUP_USER:
                // SE JÁ TIVER USUÁRIO, BLOQUEIA
                if (isUserSetup) ISOException.throwIt(ISO7816.SW_CONDITIONS_NOT_SATISFIED);

                short uLen = apdu.setIncomingAndReceive();
                Util.arrayCopy(buffer, ISO7816.OFFSET_CDATA, userID, (short)0, uLen);
                userIDLen = uLen;
                isUserSetup = true; // Trava para sempre
                break;

            case INS_SIGN_VERIFY:
                // Só assina se estiver configurado
                if (!isKeySetup || !isUserSetup) ISOException.throwIt(ISO7816.SW_CONDITIONS_NOT_SATISFIED);
                
                short dataLen = apdu.setIncomingAndReceive();
                
                // 1. Assina o desafio
                signature.init(privateKey, Signature.MODE_SIGN);
                short sigLen = signature.sign(buffer, ISO7816.OFFSET_CDATA, dataLen, buffer, (short) 0);
                
                // 2. Anexa o ID do usuário LOGO APÓS a assinatura
                Util.arrayCopy(userID, (short)0, buffer, sigLen, userIDLen);
                
                // 3. Envia (Assinatura + ID)
                apdu.setOutgoingAndSend((short) 0, (short)(sigLen + userIDLen));
                break;

            default:
                ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
        }
    }
}