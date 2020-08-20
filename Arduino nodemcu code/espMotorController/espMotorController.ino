#include <ESP8266WiFi.h>
#include <WiFiUdp.h>

WiFiUDP udp;
unsigned int localUdpPort = 4210;
char incomingPacket[5];


/* packet layout
 *  byte 0 - F or R, forward or reverse for motor A
 *  byte 1 - 0-255 power for motor A
 *  byte 2 - F or R, forward or reverse for motor B
 *  byte 3 - 0-255 power for motor B
 */

 // Pins for all inputs, keep in mind the PWM defines must be on PWM pins
#define AIN1 D1
#define BIN1 D2
#define AIN2 D0
#define BIN2 D3
#define PWMA D5
#define PWMB D6
#define STBY D4

#define FORWARD 1
#define REVERSE 2

float SCALE_FACTOR = 4.011;

void driveMotors (int motorADirection, int motorASpeed, int motorBDirection, int motorBSpeed) {
  if (motorADirection == FORWARD) {
    digitalWrite(AIN1, HIGH);
    digitalWrite(AIN2, LOW);
  }
  else {
    digitalWrite(AIN1, LOW);
    digitalWrite(AIN2, HIGH);
  }

  if (motorBDirection == FORWARD) {
    digitalWrite(BIN1, HIGH);
    digitalWrite(BIN2, LOW);
  }
  else {
    digitalWrite(BIN1, LOW);
    digitalWrite(BIN2, HIGH);
  }

  int motorASpeedScaled = motorASpeed * SCALE_FACTOR;
  int motorBSpeedScaled = motorBSpeed * SCALE_FACTOR;

  analogWrite(PWMA, motorASpeedScaled);
  analogWrite(PWMB, motorBSpeedScaled);
}

void setup() {
  Serial.begin(115200);
  
  // put your setup code here, to run once:
  pinMode(AIN1, OUTPUT);
  pinMode(AIN2, OUTPUT);
  pinMode(PWMA, OUTPUT);

  pinMode(BIN1, OUTPUT);
  pinMode(BIN2, OUTPUT);
  pinMode(PWMB, OUTPUT);
  
  pinMode(STBY, OUTPUT);

  digitalWrite(STBY, HIGH);

  WiFi.softAP("WifiCarAP", "12345678");

  udp.begin(localUdpPort);
}

void loop() {
  // put your main code here, to run repeatedly:

  int packetSize = udp.parsePacket();

  if (packetSize > 0 && packetSize == 4) {
    udp.read(incomingPacket,4);
    int motor1DirectionCommand = FORWARD;
    int motor2DirectionCommand = FORWARD;
    
    if (incomingPacket[0] == 'R') {
      motor1DirectionCommand = REVERSE;
    }

    if (incomingPacket[2] == 'R') {
      motor2DirectionCommand = REVERSE;
    }

    Serial.print("m1dc = ");
    Serial.print(motor1DirectionCommand);
    Serial.print(" m1power = ");
    Serial.println(incomingPacket[1]);

    driveMotors(motor1DirectionCommand, incomingPacket[1], motor2DirectionCommand, incomingPacket[3]);

    udp.flush();
  }

  delay(10);

}
