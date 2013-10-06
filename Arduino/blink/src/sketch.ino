#define TRANSMIT_FREQ	101			// set to the sampling freq, in hertz
#define PERIOD (1000000/TRANSMIT_FREQ)	// period, in us

void setup(void)
{
	pinMode(13, OUTPUT);
}

void loop(void)
{
	int8_t *data = (int8_t *) "ABCD";
	delay(1000);
	preamble();
	transmitData(data);
}

void preamble(void)
{
	for(int i=0; i<4; i++) {
		digitalWrite(13, HIGH);
		delayMicroseconds(PERIOD);
		digitalWrite(13, LOW);
		delayMicroseconds(PERIOD);
	}
}

void transmitData(int8_t *data)
{
	while (*data)
		transmitByte(*data++);
}

void transmitByte(int8_t data)
{
	for(int i=0; i<8; i++) {
		digitalWrite(13, data & 0x01);
		data >>= 1;
		delayMicroseconds(PERIOD);
	}
}
