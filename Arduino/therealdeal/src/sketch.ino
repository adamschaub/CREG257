#include <EEPROM.h>
#include <SoftwareSerial.h>
#include "AES.h"

#define KEY_SIZE (256/8)	// int is (I believe) 8 bits, so use byte size instead of bit size

#define LOCKED_PIN 		12
#define UNLOCKED_PIN	8

#define	EN12			6	// Enable H-bridge outputs 1 and 2
#define A1				7	// H-bridge control 1A
#define A2				4	// H-bridge control 2A
#define MOTOR_READING	5	// analog input for reading motor current draw

#define MI_OUT_PIN		13
#define	TRANSMIT_FREQ	101	// sampling freq of phone, in hertz
#define PERIOD (1000000/TRANSMIT_FREQ)	// period, in us

#define BT_RX		   2
#define BT_TX		   3
#define BT_CMDBUF_LEN   50

#define UNCONNECTED	0
#define CONNECTING	1
#define CONNECTED	2

SoftwareSerial btSerial(BT_RX, BT_TX);
HardwareSerial dbSerial = Serial;

uint8_t db_buf[50];
uint8_t db_buf_pos = 0;
uint8_t bt_buf[50];
uint8_t bt_buf_pos = 0;

uint8_t bt_state = UNCONNECTED;

uint8_t *phone_mac_addr = (uint8_t *) "78521A53544B";

AES aes;

byte key[32];
uint8_t inBytes[32];
uint8_t passcode[32] = {'1', '2', '3', '4', '5', '6', '7', '8'};	// initially set to init sequence
uint8_t passcodeLen;

void initLock(void);
void lock(void);
void unlock(void);
int readBTSerial(void);
int readBTSerialEnc(void);
int checkData(uint8_t *, uint8_t *, int);
void sendMIChallenge(uint8_t *);
void transmitData(uint8_t *);
void transmitByte(uint8_t);
void preamble(void);

void setup()
{
	dbSerial.begin(115200);
	btSerial.begin(9600);   // SoftwareSerial was having trouble at 115200

	pinMode(LOCKED_PIN, OUTPUT);
	pinMode(UNLOCKED_PIN, OUTPUT);

	pinMode(EN12, OUTPUT);  // 1,2EN pin
	pinMode(A1, OUTPUT);	// 1A pin
	pinMode(A2, OUTPUT);	// 2A pin
	digitalWrite(EN12, HIGH);   // Enable

	pinMode(MI_OUT_PIN, OUTPUT);
	digitalWrite(MI_OUT_PIN, LOW);

	analogReference(INTERNAL);

	initLock();

	aes.set_key(key, KEY_SIZE);

	btSerial.write("$$$");
	delay(2000);
}

void loop()
{
	uint8_t inByte;
	uint8_t numValid;	// how many valid responses to the MI challenge did we get?

	if (bt_state == UNCONNECTED) {
		bt_state = CONNECTING;
		btSerial.write("C,");
		btSerial.write((char *) phone_mac_addr);
		btSerial.write("\r");
		delay(100);
	}

	/* Wait for the connect message from the BT module.
	 * This will be unencrypted, because it's coming directly from the module */
	if (readBTSerial()) {

		if (checkData(bt_buf, (uint8_t *) "+CONNECT", 8)) {
			bt_state = CONNECTED;

			/* Start spamming the MI challenge */
			while(1) {
				sendMIChallenge((uint8_t *) "ABCDEFGH");

				/* Read BT to get response.  This WILL be encrypted,
				 * because this message comes from the phone */
				if (readBTSerialEnc()) {

					if (checkData(bt_buf, (uint8_t *) "ABCDEFGH", 8)) {	// Good response to MI
						btSerial.write("ACKACKACK");

						/* Since the MI response was good, just wait for a command from phone */
						while(1) {

							/* Read the command (lock or unlock) that the phone sent.
							 * This will also be encrypted. */
							if (readBTSerial()) {
								if (checkData(bt_buf, (uint8_t *) "dsadsa", 6)) {
									unlock();
									goto out;
								} else if (checkData(bt_buf, (uint8_t *) "asdasd", 6)) {
									lock();
									goto out;
								}
							}
						}
					} else {	// Bad response to MI, respond with a NAK and keep spamming
						btSerial.write("NAKNAKNAK");
					}
				}
			}
		} else if (checkData(bt_buf, (uint8_t *) "CONNECT failed", 14)) {
			bt_state = UNCONNECTED;
		} else if (checkData(bt_buf, (uint8_t *) "+REBOOT", 7)) {
			btSerial.write("$$$");
			bt_state = UNCONNECTED;
		}
	}

out:;

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
			//getData(8);
		} while (!checkData(inBytes, passcode, 8));
		dbSerial.write("Got:");
		dbSerial.write((uint8_t *) inBytes, 8);
		
		//getData(32);
		for (int i=0; i<32; i++) {
			if (inBytes[i] == 0) {
				passcodeLen = i-1;
				EEPROM.write(0, passcodeLen);
				break;
			}
			passcode[i] = inBytes[i];
			EEPROM.write(1+i, passcode[i]);
		}
		dbSerial.write("Passcode:");
		dbSerial.write((uint8_t *) passcode, passcodeLen);

		//getData(32);
		for (int i=0; i<32; i++) {
			key[i] = inBytes[i];
			EEPROM.write(1+passcodeLen+i, key[i]);
		}

		dbSerial.write("Key:");
		dbSerial.write((uint8_t *) key, 32);
	} else {
		for (int i=0; i<passcodeLen; i++)
			passcode[i] = EEPROM.read(1+i);
		for (int i=0; i<32; i++)
			key[i] = EEPROM.read(1+passcodeLen+i);
	}

	digitalWrite(UNLOCKED_PIN, LOW);
}

void unlock(void)
{
	int numHundreds = 0;	// keep track of how many current readings in a row are > 100

	digitalWrite(UNLOCKED_PIN, HIGH);	// set LEDs accordingly
	digitalWrite(LOCKED_PIN, LOW);

/*	digitalWrite(A1, LOW);	// spin motor one way...
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

	digitalWrite(UNLOCKED_PIN, LOW);	// set LEDs accordingly
	digitalWrite(LOCKED_PIN, HIGH);

/*	digitalWrite(A1, HIGH);	// spin motor one way...
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
}

/* Read output from the BT module.  Returns 1 if we reach
 * and end of line character. */
int readBTSerial()
{
	while(btSerial.available()) {
		bt_buf[bt_buf_pos] = (char)btSerial.read();
		dbSerial.write(bt_buf[bt_buf_pos]);

		if(bt_buf[bt_buf_pos] == '\r' || bt_buf[bt_buf_pos] == '\n') {
			bt_buf_pos = 0;
			return 1;
		}
		else if(bt_buf_pos >= BT_CMDBUF_LEN-2)
			bt_buf_pos = 0;
		else
			bt_buf_pos++;
	}
	return 0;
}

/* Read 16 bytes from BT module, and then decrypt the buffer. */
int readBTSerialEnc()
{
	uint8_t decrypted[16];
	uint8_t i;

	bt_buf_pos = 0;
	while(btSerial.available()) {
		bt_buf[bt_buf_pos] = btSerial.read();
		dbSerial.write(bt_buf[bt_buf_pos]);

		bt_buf_pos++;

		if (bt_buf_pos == 16) {
			bt_buf_pos = 0;
			aes.decrypt(bt_buf, decrypted);
			/* Copy decrypted message back to bt_buf */
			for (i=0; i<16; i++) {
				bt_buf[i] = decrypted[i];
			}

			return 1;
		}
	}
	return 0;
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
