#include <EEPROM.h>
#include <SoftwareSerial.h>
#include "AES.h"
#include "LowPower.h"

#define sbi(a,b) (a) |= (1 << (b))
#define cbi(a,b) (a) &= ~(1 << (b))

#define KEY_SIZE (256/8)	// int is (I believe) 8 bits, so use byte size instead of bit size

#define LOCKED_PIN 		12	// PB4
#define UNLOCKED_PIN	9	// PB1

#define	LOCK_INIT		0
#define LOCK_LOCKED		1
#define LOCK_UNLOCKED	2

#define	EN12			6	// Enable H-bridge outputs 1 and 2
#define A1				7	// H-bridge control 1A
#define A2				4	// H-bridge control 2A
#define MOTOR_READING	5	// analog input for reading motor current draw

#define MI_OUT_PIN		8	// PB0
#define	TRANSMIT_FREQ	101	// sampling freq of phone, in hertz
#define PERIOD (1000000/TRANSMIT_FREQ)	// period, in us

#define BT_RX		   	2
#define BT_TX		   	3
#define BT_CMDBUF_LEN   100
#define MAC_ADDR_SIZE	12

#define UNCONNECTED	0
#define CONNECTING	1
#define CONNECTED	2

#define WIFLY_RX	0
#define WIFLY_TX	1

#define CMD_LOCK	0
#define CMD_UNLOCK	1
#define CMD_NONE	2

void get_cmd(void);
void do_mi_authentication(uint8_t);
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

typedef struct acl_entry {
	uint8_t key[KEY_SIZE];
	uint8_t mac_addr[MAC_ADDR_SIZE];
} acl_entry_t;

acl_entry_t acl[1];
uint8_t acl_num_entries;
uint8_t acl_current;

//HardwareSerial dbSerial = Serial;
//uint8_t db_buf[BT_CMDBUF_LEN];
//uint8_t db_buf_pos = 0;

SoftwareSerial btSerial(BT_RX, BT_TX);
uint8_t bt_buf[BT_CMDBUF_LEN];
uint8_t bt_buf_pos = 0;
uint8_t bt_state = UNCONNECTED;

//SoftwareSerial wiflySerial(WIFLY_RX, WIFLY_TX);
HardwareSerial wiflySerial = Serial;
uint8_t wifly_buf[BT_CMDBUF_LEN];
uint8_t wifly_buf_pos = 0;

uint8_t lock_state = LOCK_INIT;

AES aes;

void setup()
{
	//dbSerial.begin(115200);
	wiflySerial.begin(9600);
	btSerial.begin(9600);   // SoftwareSerial was having trouble at 115200

//	ADCSRA &= ~ bit(ADEN); bitSet(PRR, PRADC);   // Disable the ADC to save power

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

	digitalWrite(UNLOCKED_PIN, HIGH);
	btSerial.write("$$$");
	delay(1000);
	btSerial.write("$$$\r");
	delay(1000);
	btSerial.write("$$$");
	delay(2000);
	digitalWrite(UNLOCKED_PIN, LOW);
}

void loop()
{
	/* Try connecting to phone if not currently connected or trying to connect */
	if (bt_state == UNCONNECTED) {
	//	bt_state = CONNECTING;
		btSerial.write("C,");
//		btSerial.write("78521A53544B");
		btSerial.write((char *) acl[acl_current].mac_addr);
		btSerial.write("\r");
		delay(100);
//		LowPower.powerDown(SLEEP_120MS, ADC_OFF, BOD_OFF);
	}

	/* Wait for the connection status message from the BT module.
	 * This will be unencrypted, because it's coming directly from the module */
	if (readBTSerial()) {

		/* Connected to phone, so now listen for the command the phone wants us to execute */
		if (checkData(bt_buf, (uint8_t *) "+CONNECT", 8)) {
			bt_state = CONNECTED;
			aes.set_key(acl[acl_current].key, KEY_SIZE);
			aes.set_key((uint8_t *) "12345678123456781234567812345678", 32);
			get_cmd();
		}
		/* Connection failed, set state to unconnected and we'll try to connect again at the top of loop() */
		else if (checkData(bt_buf, (uint8_t *) "CONNECT failed", 14)) {
			bt_state = UNCONNECTED;
			acl_current++;
			if (acl_current >= acl_num_entries)
				acl_current = 0;
		}

		/* For some reason the module will occasionally reboot, if that happens enter CMD mode again */
		else if (checkData(bt_buf, (uint8_t *) "+REBOOT", 7)) {
			btSerial.write("$$$");
			bt_state = UNCONNECTED;
		}
	}

	if (readWiflySerial()) {
		if (checkData(wifly_buf, (uint8_t *) "unlock", 6)) {
			unlock();
			wiflySerial.write("ACK");
		}
		else if (checkData(wifly_buf, (uint8_t *) "lock", 4)) {
			lock();
			wiflySerial.write("ACK");
		}
		if (checkData(wifly_buf, (uint8_t *) "status", 6)) {
			if (lock_state == LOCK_LOCKED)
				wiflySerial.write("locked");
			else if (lock_state == LOCK_UNLOCKED)
				wiflySerial.write("unlocked");
			else if (lock_state == LOCK_INIT)
				wiflySerial.write("init");
			else
				wiflySerial.write("unknown");
		}
	}

	delay(2000);
}

/* Connected to phone, so now listen for the command to be sent from the phone */
void get_cmd(void)
{
	while(1) {

		/* Read the command (lock or unlock) that the phone sent.
		 * This will also be encrypted. */
		if (readBTSerial()) {
			if (checkData(bt_buf, (uint8_t *) "dsadsa", 6)) {
				do_mi_authentication(CMD_UNLOCK);
				return;
			} else if (checkData(bt_buf, (uint8_t *) "asdasd", 6)) {
				do_mi_authentication(CMD_LOCK);
				return;
			}
		}
	}
}

/* We've received the command, so now just do the MI authenticate.
 * If that succeeds, execute the command sent previously. */
void do_mi_authentication(uint8_t on_auth_cmd)
{
	while(1) {
		sendMIChallenge((uint8_t *) "ABCDEFGH");

		/* Read BT to get response.  This WILL be encrypted,
		 * because this message comes from the phone */
		if (readBTSerialEnc()) {

			if (checkData(bt_buf, (uint8_t *) "ABCDEFGH", 8)) {	// Good response to MI
				btSerial.write("ACKACKACK");

				if (on_auth_cmd == CMD_UNLOCK)
					unlock();
				else if (on_auth_cmd == CMD_LOCK)
					lock();

				/* Done communication sequence, just return */
				return;
			} else {	// Bad response to MI, respond with a NAK and keep spamming
				btSerial.write("NAKNAKNAK");
			}
		}
	}
}

void initLock(void)
{
	/* Both LEDs on to indicate uninitialized state */
	digitalWrite(LOCKED_PIN, HIGH);
	digitalWrite(UNLOCKED_PIN, HIGH);

	/* Not initialized */
	if (EEPROM.read(0) == 255) {
		initACL();
	} else {
		readACL();
	}

	lock_state = LOCK_LOCKED;

	digitalWrite(UNLOCKED_PIN, LOW);
}

/* Inits ACL struct and waits for first ACL entry to be sent from phone */
void initACL()
{
	/* When the phone connects, we'll get output from the BT module that looks like:
		+CONNECT,<12 bytes of phone BT MAC addr>,0<init passcode><key>
	 * Where the phone's MAC address is 9 bytes from start of buf, passcode is 23, and key is
	 * 31. */
#if 0
	uint8_t *mac_addr = bt_buf + 9;
	uint8_t *key = bt_buf + 31;

	/* Wait for phone to connect and send init info */
	while (1) {
		if (readBTSerial()) {
			if (checkData(bt_buf+23, (uint8_t *) "ABCDEFGH", 8)) {
				bt_state = CONNECTED;
				break;	// got a valid init string, so break out of while loop
			}
		}
	}
#endif
	uint8_t *key = (uint8_t *) "12345678123456781234567812345678";
	uint8_t *mac_addr = (uint8_t *) "78521A53544B";

	uint8_t *acl_ptr = (uint8_t *) acl;
	uint16_t eeprom_addr = 1;
	for (; acl_ptr < (uint8_t *) acl + (uint8_t) KEY_SIZE; acl_ptr++, eeprom_addr++, key++) {
		EEPROM.write(eeprom_addr, *key);
		*acl_ptr = *key;
	}
	for (; acl_ptr < ((uint8_t *) acl + (uint8_t) KEY_SIZE + (uint8_t) MAC_ADDR_SIZE);
			acl_ptr++, eeprom_addr++, mac_addr++) {
		EEPROM.write(eeprom_addr, *mac_addr);
		*acl_ptr = *mac_addr;
	}

	acl_current = 0;
	acl_num_entries = 1;
	EEPROM.write(0, 1);
}

/* Reads in the ACL struct from EEPROM */
void readACL()
{
	uint8_t *acl_ptr = (uint8_t *) acl;
	uint16_t eeprom_addr = 1;
	acl_num_entries = EEPROM.read(0);
	for (; acl_ptr < (uint8_t *) acl + (uint8_t) (acl_num_entries *sizeof(acl_entry_t)); acl_ptr++, eeprom_addr++) {
		*acl_ptr = EEPROM.read(eeprom_addr);
	}
	acl_current = 0;
}

void unlock(void)
{
	int numHundreds = 0;	// keep track of how many current readings in a row are > 100

	digitalWrite(UNLOCKED_PIN, HIGH);	// set LEDs accordingly
	digitalWrite(LOCKED_PIN, LOW);

	lock_state = LOCK_UNLOCKED;

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

	lock_state = LOCK_LOCKED;

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
 * an end of line character. */
int readBTSerial()
{
	while(btSerial.available()) {
		bt_buf[bt_buf_pos] = (char)btSerial.read();
		//dbSerial.write(bt_buf[bt_buf_pos]);

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

/* Read output from the Wifly module.  Returns 1 if we reach
 * an end of line character. */
int readWiflySerial()
{
	while(wiflySerial.available()) {
		wifly_buf[wifly_buf_pos] = (char)wiflySerial.read();
		//dbSerial.write(wifly_buf[wifly_buf_pos]);

		if(wifly_buf[wifly_buf_pos] == '\r' || wifly_buf[wifly_buf_pos] == '\n') {
			wifly_buf_pos = 0;
			return 1;
		}
		else if(wifly_buf_pos >= BT_CMDBUF_LEN-2)
			wifly_buf_pos = 0;
		else
			wifly_buf_pos++;
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
		//dbSerial.write(bt_buf[bt_buf_pos]);

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
	for (uint8_t i=0; i<len; i++) {
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
	for(uint8_t i=0; i<8; i++) {
#ifdef UNO
		digitalWrite(MI_OUT_PIN, data & 0x01);
		delayMicroseconds(PERIOD);
#else
		if (data & 0x01)
			sbi(PORTB, 0);
		else
			cbi(PORTB, 0);
		delay(10);
		delayMicroseconds(230);
#endif
		data >>= 1;
	}
}

void preamble(void)
{
	for(uint8_t i=0; i<8; i++) {
#ifdef UNO
		digitalWrite(MI_OUT_PIN, HIGH);
		delayMicroseconds(PERIOD);
		digitalWrite(MI_OUT_PIN, LOW);
		delayMicroseconds(PERIOD);
#else
		sbi(PORTB, 0);
		delay(10);
		delayMicroseconds(230);
		cbi(PORTB, 0);
		delay(10);
		delayMicroseconds(230);
#endif
	}
}
