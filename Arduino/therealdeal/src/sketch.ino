#include <EEPROM.h>
#include "AES.h"

#define KEY_SIZE (256/8)    // int is (I believe) 8 bits, so use byte size instead of bit size
#define LOCKED_PIN 		12
#define UNLOCKED_PIN	8
#define STATE_LOCKED	0
#define STATE_UNLOCKED	1

AES aes;

byte key[32];
uint8_t inBytes[32];
uint8_t passcode[32] = {'1', '2', '3', '4', '5', '6', '7', '8'};	// initially set to init sequence
uint8_t passcodeLen;

uint8_t state;

void initLock(void);
void getData(int);
int checkData(uint8_t *, uint8_t *, int);

void setup()
{
	Serial.begin(115200);
	pinMode(13, OUTPUT);
	pinMode(LOCKED_PIN, OUTPUT);
	pinMode(UNLOCKED_PIN, OUTPUT);

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
	
	if (checkData(decrypted, passcode, passcodeLen)) {
		Serial.write("ACK");	// send three, so hopefully one ends up in the read buffer on phone completely...
		Serial.write("ACK");
		Serial.write("ACK");
		getData(16);
		aes.decrypt(inBytes, decrypted);
		if (checkData(decrypted, (uint8_t *) "asdasd", 6)) {
			digitalWrite(UNLOCKED_PIN, LOW);
			digitalWrite(LOCKED_PIN, HIGH);
		} else if (checkData(decrypted, (uint8_t *) "dsadsa", 6)) {
			digitalWrite(UNLOCKED_PIN, HIGH);
			digitalWrite(LOCKED_PIN, LOW);
		} else {
			Serial.write("NAK");
			Serial.write("BAD COMMAND BAD COMMAND");
			Serial.write((uint8_t *) decrypted, 16);
			Serial.write((uint8_t *) inBytes, 6);
		}
	} else {
		Serial.write("NAK");
		Serial.write("BAD KEY BAD KEY BAD KEY");
		Serial.write((uint8_t *) inBytes, 16);
	}
}

void initLock(void)
{
	/* Both LEDs on to indicate uninitialized state */
	digitalWrite(LOCKED_PIN, HIGH);
	digitalWrite(UNLOCKED_PIN, HIGH);

	passcodeLen = EEPROM.read(0);

	/* If we don't have a key/passcode yet... */
	if (passcodeLen == 255) {
		do {
			getData(8);
		} while (!checkData(inBytes, passcode, 8));
		//Serial.write("Got:");
		//Serial.write((uint8_t *) inBytes, 8);
		
		getData(32);
		for (int i=0; i<32; i++) {
			if (inBytes[i] == 0) {
				passcodeLen = i-1;
				EEPROM.write(0, passcodeLen);
				break;
			}
			passcode[i] = inBytes[i];
			EEPROM.write(1+i, passcode[i]);
		}
		//Serial.write("Passcode:");
		//Serial.write((uint8_t *) passcode, passcodeLen);

		getData(32);
		for (int i=0; i<32; i++) {
			key[i] = inBytes[i];
			EEPROM.write(1+passcodeLen+i, key[i]);
		}

		//Serial.write("Key:");
		//Serial.write((uint8_t *) key, 32);
	} else {
		for (int i=0; i<passcodeLen; i++)
			passcode[i] = EEPROM.read(1+i);
		for (int i=0; i<32; i++)
			key[i] = EEPROM.read(1+passcodeLen+i);
	}

	state = STATE_LOCKED;
	digitalWrite(UNLOCKED_PIN, LOW);
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
int checkData(uint8_t *received, uint8_t *target, int len)
{
	for (int i=0; i<len; i++) {
		if (received[i] != target[i])
			return 0;
	}

	return 1;
}
