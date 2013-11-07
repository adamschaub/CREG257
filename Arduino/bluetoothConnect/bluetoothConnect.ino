/*
  Bluetooth Connect
  
  Waits for reception of string "heyheyhey" and responds over bluetooth "hey you"
*/

int numBytes = 0;
char* received;
String phrase;

void setup() {
  Serial.begin(112500);
}

void loop() {
  
  //check for reception of data
  numBytes = Serial.available();
  if(numBytes > 0) {
    Serial.readBytes(received, numBytes);
    
    phrase = String(received);
    if(phrase.compareTo("heyheyhey") == 0) {
      Serial.println("hey you");
    }
  }
}
