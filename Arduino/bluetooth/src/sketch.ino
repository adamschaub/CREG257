#include <SoftwareSerial.h>

#define BT_RX			2
#define BT_TX			3
#define BT_CMDBUF_LEN	50

SoftwareSerial btSerial(BT_RX, BT_TX);
HardwareSerial dbSerial = Serial;

uint8_t buf[50];
uint8_t buf_pos = 0;

void setup()
{
	Serial.begin(115200);
	btSerial.begin(9600);	// SoftwareSerial was having trouble at 115200
}

void loop()
{
	/* If input available from debug serial interface, add char to buffer */
	while(dbSerial.available()) {
		buf[buf_pos] = (char)dbSerial.read();
		/* If user pressed enter, do the BT command */
		if(buf[buf_pos] == '\r' || buf[buf_pos] == '\n') {
			do_bt_cmd();
			buf_pos = 0;
		}
		else if(buf_pos >= BT_CMDBUF_LEN-2)
			buf_pos = 0;
		else
			buf_pos++;
	}

	/* If input available from BT, print it out */
	while(btSerial.available()) {
		dbSerial.print((char)btSerial.read());
	}
}

/* Commands for RN-42:
 *
 * $$$			Enter command mode
 * SM,0\r		Slave mode
 * SM,3\r		Auto-connect
 * SR,<addr>\r	Store MAC address of target
 * SA,<1,0>\r	Authentication - 1=on, 0=off
 * SE,<1,0>\r	Encryption
 * C\r			Connect to target
 * Use G instead of S for any set commands to get the current setting.
 * See the datasheet for many more.
*/

void do_bt_cmd()
{
	if(buf[0] != '$') {	// Don't add the carriage return when entering command mode
		buf[buf_pos] = '\r';
		btSerial.write(buf, buf_pos+1);			// Send command to BT module
	} else {
		btSerial.write(buf, buf_pos);			// Send command to BT module
	}
}
