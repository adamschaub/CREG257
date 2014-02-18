#include<SoftwareSerial.h>

#define W_RX 10
#define W_TX 11
#define BUFF_SIZE 256
//const String inputMarker = "**PHNKY**";
uint8_t match_pos;

String inputBuff;
uint8_t input_pos;
uint8_t buff[BUFF_SIZE];
uint8_t buff_pos;

SoftwareSerial wiflySerial(W_RX, W_TX);
HardwareSerial arduinoSerial = Serial;

void setup()
{
	wiflySerial.begin(9600);
	arduinoSerial.begin(9600);
	buff_pos = 0;
	input_pos = 0;
	match_pos = 0;
}	

void loop()
{
	while(wiflySerial.available()) {
		parseWifly();

		//arduinoSerial.print(inputBuff);
	}


	
	while(arduinoSerial.available()) {
		
		uint8_t next = arduinoSerial.read();
		arduinoSerial.print((char)next);
		if(next == '\r' || next == '\n') {
			arduinoSerial.println();
			sendWiflyCmd();
			buff_pos = 0;
		}
		else if(next == '\b' && buff_pos > 0) {
			buff_pos--;
		}
		else {
			buff[buff_pos++] = next;
		}
	}
	
}

void sendWiflyCmd() {

	//$$$ enters command mode, do not send <cr>
	if(buff[0] == '$' && buff[1] == '$' && buff[2] == '$') {
		wiflySerial.write(buff, buff_pos); 
	}
	else {
		buff[buff_pos] = '\r';
		wiflySerial.write(buff, buff_pos+1); //send <cr> if not entering command mode
	}
}

/* TODO:
	Having errors with buffer size. When receiving large messages, only the 
	first ~150 characters or so are received. May not be an issue with smaller
	communications (aka TCP serial only).
*/
void parseWifly() {
	inputBuff = "";
	input_pos = 0;
	while(wiflySerial.available()) {
			inputBuff += (char)wiflySerial.read();//dump first 256 chars
			
	}

	uint8_t i = 0;
	arduinoSerial.print(inputBuff);
	/*
	arduinoSerial.print(inputBuff.substring(0,9));
	while(inputBuff.substring(0, 9) != "**PHNKY**") {
		inputBuff = inputBuff.substring(10);
		arduinoSerial.print(inputBuff);
	}*/
}

////////////////////////////
//WIFLY CONFIGURATION - TCP
////////////////////////////
/**
	set dns name <host name> //currently testserver-c9-ams314.c9.io
	set ip host 0 //forces a dns lookup for the given host
	set ip remote 80 //standard web port
	set com remote GET$/update?
	set option format 9 //sends http header (0x01) and appends &id=<deviceid> (0x08)
	set uart mode 0x10 //buffers incoming TCP information
*/
