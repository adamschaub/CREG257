#include "AES.h"

#define TRANSMIT_FREQ   101         // set to the sampling freq, in hertz
#define PERIOD (1000000/TRANSMIT_FREQ)  // period, in us

#define KEY_SIZE (256/8)	// int is (I believe) 8 bits, so use byte size instead of bit size

AES aes;

// 256 bit (32 byte) key = 0x0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF
byte key[] =
{
	0x01, 0x23, 0x45, 0x67, 0x89, 0xab, 0xcd, 0xef,
	0x01, 0x23, 0x45, 0x67, 0x89, 0xab, 0xcd, 0xef,
	0x01, 0x23, 0x45, 0x67, 0x89, 0xab, 0xcd, 0xef,
	0x01, 0x23, 0x45, 0x67, 0x89, 0xab, 0xcd, 0xef,
};

/* 16 bytes.  This should be random, I think.  Do we even need this? */
byte my_iv[] = 
{
	0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
	0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
};

/* 16 bytes of plain text data */
byte plain_data[] =
{
	'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p',
};

byte cipher[N_BLOCK];

void setup()
{
	pinMode(13, OUTPUT);
}

void loop()
{
	delay(100);
	encrypt_data();
	preamble();
//	transmitData(plain_data);
	transmitData(cipher);
}

void encrypt_data()
{
	byte success;

	Serial.println("Starting encryption run");

	success = aes.set_key(key, KEY_SIZE);
	if (success == SUCCESS)
		Serial.println("Setting key: Success!");
	else {
		Serial.println("Setting key: Fail!");
		while(1);	// don't both to continue if setting key failed
	}

	success = aes.encrypt(plain_data, cipher);
	if (success == SUCCESS) {
		Serial.println("Encrypting data: Success!");
		for(int i=0; i<sizeof(cipher); i++)
			Serial.print(cipher[i], HEX);
		Serial.println("");
	} else {
		Serial.println("Encrpyting data: Fail!");
		while(1);
	}
}

void transmitData(uint8_t *data)
{
    while (*data)
        transmitByte(*data++);
}

void transmitByte(uint8_t data)
{
    for(int i=0; i<8; i++) {
        //for (int j=0; j<3; j++) {
            digitalWrite(13, data & 0x01);
            delayMicroseconds(PERIOD);
        //}
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
