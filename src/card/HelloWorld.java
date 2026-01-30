package card;

import javacard.framework.*;

public class HelloWorld extends Applet {
    // Definimos uma instrução para pedir o texto (0x01)
    private static final byte INS_SAY_HELLO = (byte) 0x01;
    private static final byte[] helloText = {
        '0', 'a', 'A', 'z', 'a', 'a', 'a', 'A', 'A', '1'
    };

    public static void install(byte[] bArray, short bOffset, byte bLength) {
        new HelloWorld().register();
    }

    public void process(APDU apdu) {
        if (selectingApplet()) {
            return;
        }

        byte[] buffer = apdu.getBuffer();
        
        // Verifica se a instrução enviada é a 0x01
        if (buffer[ISO7816.OFFSET_INS] == INS_SAY_HELLO) {
            short len = (short) helloText.length;
            // Copia o "Hello World" para o buffer de saída
            Util.arrayCopyNonAtomic(helloText, (short) 0, buffer, (short) 0, len);
            // Envia os bytes de volta
            apdu.setOutgoingAndSend((short) 0, len);
        } else {
            ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
        }
    }
}
