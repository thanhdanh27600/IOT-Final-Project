package com.example.iot_final;

public class EncryptionUART {
    private String message;
    private char key;
    public EncryptionUART(String m, char k){
        this.message = m;
        this.key = k;
    }

    public String decryptUART() {
        String decrypted_content = "";
        char ascii_code;
        int ascii_temp;
        for (char ch : this.message.toCharArray()) {
            ascii_temp = (ch - this.key);
            if (ascii_temp < 36) {
                ascii_code = (char) ((125 + 1) - (36 - ascii_temp));
            } else {
                ascii_code = (char) ascii_temp;
            }
            decrypted_content += ascii_code;
        }

        return decrypted_content;
    }

    public String encryptUART() {
        String encrypted_content = "";
        char ascii_code;
        int ascii_temp;
        for (char ch :  this.message.toCharArray()) {
            ascii_temp = (ch + this.key);
            if (ascii_temp > 125) {
                ascii_code = (char) (36 + ascii_temp % (125 + 1));
            } else {
                ascii_code = (char) ascii_temp;
            }
            encrypted_content += ascii_code;
        }
        return encrypted_content;
    }

}
