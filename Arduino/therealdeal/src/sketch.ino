#include <EEPROM.h>
#include "AES.h"

#define KEY_SIZE (256/8)    // int is (I believe) 8 bits, so use byte size instead of bit size

#define LOCKED_PIN 		12
#define UNLOCKED_PIN	8
#define STATE_LOCKED	0
#define STATE_UNLOCKED	1

#define	EN12			2	// Enable H-bridge outputs 1 and 2
#define A1				3	// H-bridge control 1A
#define A2				4	// H-bridge control 2A
#define MOTOR_READING	5	// analog input for reading motor current draw

#define MI_OUT_PIN		13
#define	TRANSMIT_FREQ	101	// sampling freq of phone, in hertz
#define PERIOD (1000000/TRANSMIT_FREQ)	// period, in us

AES aes;

byte key[32];
uint8_t inBytes[32];
uint8_t passcode[32] = {'1', '2', '3', '4', '5', '6', '7', '8'};	// initially set to init sequence
uint8_t passcodeLen;

uint8_t state;

void initLock(void);
void getData(int);
void lock(void);
void unlock(void);
int checkData(uint8_t *, uint8_t *, int);
void sendMIChallenge(uint8_t *);
void transmitData(uint8_t *);
void transmitByte(uint8_t);
void preamble(void);

void setup()
{
	Serial.begin(115200);
	pinMode(LOCKED_PIN, OUTPUT);
	pinMode(UNLOCKED_PIN, OUTPUT);

	pinMode(EN12, OUTPUT);  // 1,2EN pin
    pinMode(A1, OUTPUT);    // 1A pin
    pinMode(A2, OUTPUT);    // 2A pin
    digitalWrite(EN12, HIGH);   // Enable

	pinMode(MI_OUT_PIN, OUTPUT);
	digitalWrite(MI_OUT_PIN, LOW);

	initLock();

	aes.set_key(key, KEY_SIZE);
}

void loop()
{
	uint8_t decrypted[16];
	uint8_t inByte;
	uint8_t numValid;	// how many valid responses to the MI challenge did we get?

	/* Get encrypted passcode... */
	getData(16);
	aes.decrypt(inBytes, decrypted);
	
	/* If passcode was correct, then continue */
	if (checkData(decrypted, passcode, passcodeLen)) {
		Serial.write("ACK");	// send two, so hopefully one ends up in the read buffer on phone completely...
		Serial.write("ACK");

		/* After sending ACK, send the MI challenge and then wait for it to be echoed back */
		sendMIChallenge((uint8_t *) "ABCDEFGH");	// send challenge over MI
		getData(8);
		numValid = checkData(inBytes, (uint8_t *) "ABCDEFGH", 8);
		sendMIChallenge((uint8_t *) "ABCDEFGH");	// send challenge over MI
		getData(8);
		numValid += checkData(inBytes, (uint8_t *) "ABCDEFGH", 8);
		sendMIChallenge((uint8_t *) "ABCDEFGH");	// send challenge over MI
		getData(8);
		numValid += checkData(inBytes, (uint8_t *) "ABCDEFGH", 8);

		/* Was at least one response valid? */
		if (numValid > 0) {

			/* Yup, so wait for command (lock/unlock) now */
			Serial.write("ACK");
			Serial.write("ACK");
			getData(16);
			aes.decrypt(inBytes, decrypted);

			if (checkData(decrypted, (uint8_t *) "asdasd", 6)) {
				lock();
				Serial.write("ACK");
				Serial.write("ACK");
			} else if (checkData(decrypted, (uint8_t *) "dsadsa", 6)) {
				unlock();
				Serial.write("ACK");
				Serial.write("ACK");
			} else {
				Serial.write("NAK");
				Serial.write("NAK");
				Serial.write("BAD COMMAND BAD COMMAND");
				Serial.write((uint8_t *) decrypted, 16);
				Serial.write((uint8_t *) inBytes, 6);
			}

		} else {	/* Nope, send a NAK */
			Serial.write("NAK");
			Serial.write("NAK");
			Serial.write("BAD RESPONSE BAD RESPONSE");
			Serial.write((uint8_t *) inBytes, 8);
		}
	} else {	/* Otherwise, send a NAK and wait for passcode again */
		Serial.write("NAK");
		Serial.write("NAK");
		Serial.write("BAD KEY BAD KEY BAD KEY");
		//Serial.write((uint8_t *) decrypted, 16);
		//Serial.write((uint8_t *) inBytes, 16);
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

void unlock(void)
{
	int numHundreds = 0;	// keep track of how many current readings in a row are > 100

	digitalWrite(UNLOCKED_PIN, HIGH);	// set LEDs accordingly
	digitalWrite(LOCKED_PIN, LOW);
/*
	digitalWrite(A1, LOW);	// spin motor one way...
    digitalWrite(A2, HIGH);
    delay(3000);	// wait before checking current draw, to avoid the spikes when motor first starts
	while (1) {
		if (analogRead(MOTOR_READING) > 100)
			numHundreds++;
		else
			numHundreds = 0;
		if (numHundreds == 2)
			break;
	}
	digitalWrite(A1, LOW);	// turn motor off
    digitalWrite(A2, LOW);*/
}

void lock(void)
{
	int numHundreds = 0;	// keep track of how many current readings in a row are > 100
/*
	digitalWrite(A1, HIGH);	// spin motor one way...
    digitalWrite(A2, LOW);
    delay(3000);	// wait before checking current draw, to avoid the spikes when motor first starts
	while (1) {
		if (analogRead(MOTOR_READING) > 100)
			numHundreds++;
		else
			numHundreds = 0;
		if (numHundreds == 2)
			break;
	}
	digitalWrite(A1, LOW);	// turn motor off
    digitalWrite(A2, LOW);*/

	digitalWrite(UNLOCKED_PIN, LOW);	// set LEDs accordingly
	digitalWrite(LOCKED_PIN, HIGH);
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

void sendMIChallenge(uint8_t *challenge)
{
	preamble();
	//for (uint8_t i=1; i<9; i++)
	//	transmitByte(i);
	//for (uint8_t i=0xdd; i<0xdd+8; i++)
	//	transmitByte(i);
	transmitData(challenge);
}

void transmitData(uint8_t *data)
{
    while (*data)
        transmitByte(*data++);
}

void transmitByte(uint8_t data)
{
    for(int i=0; i<8; i++) {
		digitalWrite(13, data & 0x01);
		delayMicroseconds(PERIOD);
        data >>= 1;
    }
}

void preamble(void)
{
    for(int i=0; i<8; i++) {
        digitalWrite(13, HIGH);
        delayMicroseconds(PERIOD);
        digitalWrite(13, LOW);
        delayMicroseconds(PERIOD);
    }
}
