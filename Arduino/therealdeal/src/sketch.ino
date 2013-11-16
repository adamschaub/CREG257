#include "AES.h"

AES aes;

#define KEY_SIZE (256/8)    // int is (I believe) 8 bits, so use byte size instead of bit size

byte key[] =
{
    0x01, 0x23, 0x45, 0x67, 0x89, 0xab, 0xcd, 0xef,
    0x01, 0x23, 0x45, 0x67, 0x89, 0xab, 0xcd, 0xef,
    0x01, 0x23, 0x45, 0x67, 0x89, 0xab, 0xcd, 0xef,
    0x01, 0x23, 0x45, 0x67, 0x89, 0xab, 0xcd, 0xef,
};

uint8_t inBytes[32];
uint8_t *passcode = (uint8_t *) "yoopenthedoor";

void setup()
{
	Serial.begin(115200);
	pinMode(13, OUTPUT);
	digitalWrite(13, LOW);

	aes.set_key(key, KEY_SIZE);
	delay(1000);
}

void loop()
{
	uint8_t decrypted[16];
	uint8_t inByte;

	getData(16);
	aes.decrypt(inBytes, decrypted);
	
	if (checkData(decrypted, sizeof(passcode))) {
		digitalWrite(13, HIGH);
		Serial.write("ACK");
	} else {
		Serial.write("NAK");
		Serial.write((uint8_t *) inBytes, 16);
	}
}

void getData(int bytesToGet)
{
	for (int i=0; i<bytesToGet; i++) {
		while (!Serial.available() > 0);
		if (Serial.available() > 0)
			inBytes[i] = Serial.read();
	}
}

/* Returns 1 if equal, 0 otherwise */
int checkData(uint8_t *received, int len)
{
	for (int i=0; i<len; i++) {
		if (received[i] != passcode[i])
			return 0;
	}

	return 1;
}
