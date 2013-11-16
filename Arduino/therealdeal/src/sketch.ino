#include "AES.h"

AES aes;

#define KEY_SIZE (256/8)    // int is (I believe) 8 bits, so use byte size instead of bit size

/*byte key[] =
{
    0x01, 0x23, 0x45, 0x67, 0x89, 0xab, 0xcd, 0xef,
    0x01, 0x23, 0x45, 0x67, 0x89, 0xab, 0xcd, 0xef,
    0x01, 0x23, 0x45, 0x67, 0x89, 0xab, 0xcd, 0xef,
    0x01, 0x23, 0x45, 0x67, 0x89, 0xab, 0xcd, 0xef,
};*/

byte key[32];
uint8_t inBytes[32];
uint8_t passcode[32] = {'1', '2', '3', '4', '5', '6', '7', '8'};
uint8_t passcodeLen;

void initLock(void);
void getData(int);
int checkData(uint8_t *, int);

void setup()
{
	Serial.begin(115200);
	pinMode(13, OUTPUT);
	digitalWrite(13, LOW);

	initLock();

	aes.set_key(key, KEY_SIZE);
}

void loop()
{
	uint8_t decrypted[16];
	uint8_t inByte;

	getData(16);
	aes.decrypt(inBytes, decrypted);
	
	if (checkData(decrypted, passcodeLen)) {
		digitalWrite(13, HIGH);
		Serial.write("ACK");
	} else {
		Serial.write("NAK");
		Serial.write((uint8_t *) inBytes, 16);
	}
}

void initLock(void)
{
	do {
		getData(8);
	} while (!checkData(inBytes, 8));
	
	getData(32);
	for (int i=0; i<32; i++) {
		if (inBytes[i] == 0) {
			passcodeLen = i-1;
			break;
		}
		passcode[i] = inBytes[i];
	}

	getData(32);
	for (int i=0; i<32; i++)
		key[i] = inBytes[i];
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
