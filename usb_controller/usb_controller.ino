#include "variant.h"
#include <stdio.h>
#include <adk.h>

/* !!!!!----- Debug zone -----!!!!! */
//#define DEBUG

#ifdef DEBUG
void log(String string)
{
  Serial.print(string);
}

void log(String string, bool newline)
{
  if (newline)
    Serial.println(string);
  else
    Serial.print(string);
}
#endif
/* -------- End Debug zone -------- */

/* Motor Module
 *
 * stop([motor]) - stop all motors or a selected motor
 * setDir(dir, [motor]) - set direction for all motors or one selected motor
 * setPow(pow, [motor]) - set power for all motors or one selected motor
 * setPowDelay(time, pow, [motor]) - set power for all motor or one selected motor
 *                                   for a certain period of time
 *
 */

// Velocites
#define VLOW 75
#define VMEDIUM 127
#define VHIGH 200
#define VMAX 250

#define CW  0 // Clockwise
#define CCW 1 // Counter-Clockwise

// Motor definitions
#define MOTOR_A 0
#define MOTOR_B 1

// Pin Assignments //
const byte PWMA = 3;  // PWM control (speed) for motor A
const byte PWMB = 11; // PWM control (speed) for motor B
const byte DIRA = 12; // Direction control for motor A
const byte DIRB = 13; // Direction control for motor B

void stop()
{
  analogWrite(PWMA, 0);
  analogWrite(PWMB, 0);
}

void stop(byte motor)
{
  if (motor == MOTOR_A)
    analogWrite(PWMA, 0);
  else if (motor == MOTOR_B)
    analogWrite(PWMB, 0);
}

void setDir(byte dir)
{
  digitalWrite(DIRA, dir);
  digitalWrite(DIRB, dir);
}

void setDir(byte dir, byte motor)
{
  if (motor == MOTOR_A)
    digitalWrite(DIRA, dir);
  else if (motor == MOTOR_B)
    digitalWrite(DIRB, dir);
}

void setPow(byte vel)
{
  analogWrite(PWMA, vel);
  analogWrite(PWMB, vel);
}

void setPow(byte vel, byte motor)
{
  if (motor == MOTOR_A)
    analogWrite(PWMA, vel);
  else if (motor == MOTOR_B)
    analogWrite(PWMB, vel);
}

void setPowDelay(unsigned long time, byte vel)
{
  setPow(vel);
  delay(time);
}

void setPowDelay(unsigned long time, byte vel, byte motor)
{
  setPow(vel, motor);
  delay(time);
}

// setupArdumoto initialize all pins
void setupArdumoto()
{
  // All pins should be setup as outputs:
  pinMode(PWMA, OUTPUT);
  pinMode(PWMB, OUTPUT);
  pinMode(DIRA, OUTPUT);
  pinMode(DIRB, OUTPUT);

  // Initialize all pins as low:
  digitalWrite(PWMA, LOW);
  digitalWrite(PWMB, LOW);
  digitalWrite(DIRA, LOW);
  digitalWrite(DIRB, LOW);
}

/* USB Controller Module
 *
 * 1) stop([motor])
 * 2) setDir(dir, [motor])
 * 3) setPow(pow, [motor])
 * 4) setPowDelay(time, pow, [motor])
 *
 *
 * OPCODE (string)
 *
 * ---------------------
 * fun |    char pos   |
 *     | 0 | 1 | 2 | 3 |
 * ---------------------
 *  1  | 0 | m |   |   |
 * ---------------------
 *  2  | 1 | m | c |   |
 * ---------------------
 *  3  | 2 | m | p |   |
 * ---------------------
 *  4  | 3 | m | p | t |
 * ---------------------
 *
 * V = void (empty)
 *
 * m = [V, A, B]
 * c = [0, 1]
 * p = [0, 1, 2, 3, 4]
 * t = int string
 *
 */

// Accessory descriptor. It's how Arduino identifies itself to Android.
char applicationName[] = "ArDroid_FollowUp"; // Model on Android
char accessoryName[] = "ArduinoDueController"; // your Arduino board
char companyName[] = "UniPG"; // Manufactured on Android

// Make up anything you want for these
char versionNumber[] = "1.0";
char serialNumber[] = "1";
char url[] = "https://unipg.it";

USBHost Usb;
ADK adk(&Usb, companyName, applicationName, accessoryName, versionNumber, url, serialNumber);

#define RCVSIZE 128

uint32_t responseLen = 2;
char * response = "42";

byte parseCommands(String inputString, String * commandList, byte &commandsNumber)
{
  String temp = "";
  byte tempNumCommand = 0;

  for (int i = 0; i != inputString.length(); ++i)
  {
    if (inputString[i] == ';')
    {
      commandList[tempNumCommand] = temp;
      tempNumCommand++;
      temp = "";
    }
    else
    {
      temp += inputString[i];
    }
    if (i == inputString.length() - 1)
    {
      commandList[tempNumCommand] = temp;
      tempNumCommand++;
      temp = "";
    }
  }

  commandsNumber = tempNumCommand;

  return tempNumCommand;
}

void execCommands(String * commandList, byte commandsNum)
{
  for (int i = 0; i != commandsNum; ++i) {
#ifdef DEBUG
    log("-> ");
    log(commandList[i], true);
#endif
    if (commandList[i][0] == '0') // stop([motor])
    {
      if (commandList[i][1] == 'V')
      {
#ifdef DEBUG
        log("STOP!", true);
#endif
        stop();
      }
      else if (commandList[i][1] == 'A')
      {
#ifdef DEBUG
        log("STOP A!", true);
#endif
        stop(MOTOR_A);
      }
      else if (commandList[i][1] == 'B')
      {
#ifdef DEBUG
        log("STOP B!", true);
#endif
        stop(MOTOR_B);
      }
    }
    else if (commandList[i][0] == '1') // setDir(dir, [motor])
    {
      byte dir;
      if (commandList[i][2] == '0') {
#ifdef DEBUG
        log("Set DIR CW on ");
#endif
        dir = CW;
      }
      else if (commandList[i][2] == '1') {
#ifdef DEBUG
        log("Set DIR CCW on ");
#endif
        dir = CCW;
      }

      if (commandList[i][1] == 'V')
      {
#ifdef DEBUG
        log("all motors!", true);
#endif
        setDir(dir);
      }
      else if (commandList[i][1] == 'A')
      {
#ifdef DEBUG
        log("motor A!", true);
#endif
        setDir(dir, MOTOR_A);
      }
      else if (commandList[i][1] == 'B')
      {
#ifdef DEBUG
        log("motor B!", true);
#endif
        setDir(dir, MOTOR_B);
      }
    }
    else if (commandList[i][0] == '2') // setPow(pow, [motor])
    {
      byte pwr;
      if (commandList[i][2] == '0') {
#ifdef DEBUG
        log("Set 0 power ");
#endif
        pwr = 0;
      }
      else if (commandList[i][2] == '1') {
#ifdef DEBUG
        log("Set LOW power ");
#endif
        pwr = VLOW;
      }
      else if (commandList[i][2] == '2') {
#ifdef DEBUG
        log("Set MEDIUM power ");
#endif
        pwr = VMEDIUM;
      }
      else if (commandList[i][2] == '3') {
#ifdef DEBUG
        log("Set HIGH power ");
#endif
        pwr = VHIGH;
      }
      else if (commandList[i][2] == '4') {
#ifdef DEBUG
        log("Set MAX power ");
#endif
        pwr = VMAX;
      }

      if (commandList[i][1] == 'V')
      {
#ifdef DEBUG
        log("on all motors!", true);
#endif
        setPow(pwr);
      }
      else if (commandList[i][1] == 'A')
      {
#ifdef DEBUG
        log("on motor A!", true);
#endif
        setPow(pwr, MOTOR_A);
      }
      else if (commandList[i][1] == 'B')
      {
#ifdef DEBUG
        log("on motor B!", true);
#endif
        setPow(pwr, MOTOR_B);
      }
    }
    else if (commandList[i][0] == '3') // setPowDelay(time, pow, [motor])
    {
      unsigned long time = commandList[i].substring(3).toInt();
#ifdef DEBUG
      log("For ");
      log(commandList[i].substring(3));
      log(" milliseconds ");
#endif
      byte pwr;
      if (commandList[i][2] == '0') {
#ifdef DEBUG
        log("Set 0 power ");
#endif
        pwr = 0;
      }
      else if (commandList[i][2] == '1') {
#ifdef DEBUG
        log("Set LOW power ");
#endif
        pwr = VLOW;
      }
      else if (commandList[i][2] == '2') {
#ifdef DEBUG
        log("Set MEDIUM power ");
#endif
        pwr = VMEDIUM;
      }
      else if (commandList[i][2] == '3') {
#ifdef DEBUG
        log("Set HIGH power ");
#endif
        pwr = VHIGH;
      }
      else if (commandList[i][2] == '4') {
#ifdef DEBUG
        log("Set MAX power ");
#endif
        pwr = VMAX;
      }

      if (commandList[i][1] == 'V')
      {
#ifdef DEBUG
        log("on all motors!", true);
#endif
        setPowDelay(time, pwr);
      }
      else if (commandList[i][1] == 'A')
      {
#ifdef DEBUG
        log("on motor A!", true);
#endif
        setPowDelay(time, pwr, MOTOR_A);
      }
      else if (commandList[i][1] == 'B')
      {
#ifdef DEBUG
        log("on motor B!", true);
#endif
        setPowDelay(time, pwr, MOTOR_B);
      }
    }
  }
#ifdef DEBUG
  log("----- !!!End task!!! -----", true);
#endif
}

#define LEDGREEN 42
#define LEDRED 28

/* ----- MAIN SECTION ----- */
void setup()
{
  Serial.begin(9600);
  setupArdumoto(); // Set all pins as outputs
  cpu_irq_enable();
  pinMode(LEDGREEN, OUTPUT);
  pinMode(LEDRED, OUTPUT);
  delay(100);
#ifdef DEBUG
  log("Arduino 2 started!", true);
#endif
#ifdef DEBUG
  log("Test leds");
  // Test led
  for (int i = 0; i < 21; ++i) {
    digitalWrite(LEDRED, HIGH);
    delay(21);
    digitalWrite(LEDRED, LOW);

    digitalWrite(LEDGREEN, HIGH);
    delay(21);
    digitalWrite(LEDGREEN, LOW);
    log(".");
  }
  log("", true);
#endif
}

void loop()
{
  /*
  // Start interpreter
  String commandList[10];
  byte commandsNumber = 0;

  parseCommands("0V;1V0;2V3", commandList, commandsNumber);
  execCommands(commandList, commandsNumber);

  delay(2000);

  parseCommands("0V;1V1;2V3", commandList, commandsNumber);
  execCommands(commandList, commandsNumber);

  delay(2000);

  parseCommands("0V;3A31000", commandList, commandsNumber);
  execCommands(commandList, commandsNumber);

  parseCommands("0V;3B31000", commandList, commandsNumber);
  execCommands(commandList, commandsNumber);*/

  uint8_t buf[RCVSIZE];
  uint32_t nbread = 0;

  Usb.Task();

  if (adk.isReady())
  {
#ifdef DEBUG
    log("!!!READY!!!", true);
#endif
    digitalWrite(LEDGREEN, HIGH);
    digitalWrite(LEDRED, LOW);

    adk.read(&nbread, RCVSIZE, buf);
    if (nbread > 0)
    {
#ifdef DEBUG
      log("READED :");
      log(String(nbread));
#endif

      // Convert nbread to String
      String inputString;
      for (uint32_t i = 0; i != nbread; ++i)
      {
        inputString += (char) buf[i];
      }

      if (inputString == "Read")
      {
#ifdef DEBUG
        log("String 'Read' received!", true);
#endif
        digitalWrite(LEDRED, HIGH);
      }
      else
      {
#ifdef DEBUG
        log(" -> ", false);
        log(inputString, true);
#endif

        // Start interpreter
        String commandList[10];
        byte commandsNumber = 0;

        parseCommands(inputString, commandList, commandsNumber);

#ifdef DEBUG
        log("Task :", true);
        for (int i = 0; i != commandsNumber; ++i)
          log(commandList[i], true);
        log("-----", true);
#endif

        execCommands(commandList, commandsNumber);

        /* --- DONE --- */
        adk.write(responseLen, (uint8_t *)response);
      }
    }
    else {
#ifdef DEBUG
      log("NOTHING TO READ!", true);
#endif
      digitalWrite(LEDGREEN, LOW);
    }
  }
  else
  {
#ifdef DEBUG
    log("NOT READY!", true);
#endif
    digitalWrite(LEDRED, HIGH);
    digitalWrite(LEDGREEN, LOW);
  }
}
