# ArDroid FollowUp
## [Artificial Intelligence] Object tracking with OpenCV on Android and follower robot with Arduino

### Overview
This is made for a university project. Below is the assigment text (in italian).

**Scenario**: Il robot si muove in una scena “guardando” verso l’alto: se individua un simbolo dato su una maglietta o su una persona (anche in forma di cartello), che può essere una forma di un colore particolare (che non si confonda con altri che possono essere sulla scena), un QR code, o una faccia stampata, si dirige verso di esso, in modo da inquadrarla frontalmente alla massima risoluzione e seguire la persona che indossa la maglietta/cartello per i corridoi del dipartimento.
  
**Obiettivo**: Percezione dell’immagine tramite sensore (camera di un telefono Android puntata nella direzione desiderata), sistema di riconoscimento dell’immagine (sviluppata in Android) e attuazione dei movimenti per il tracking della persona.

### How it works
The robot is made using the Arduino Due platform and ArduMotor kit on a base with two wheels and a locking arm to connect the Android smartphone. The motors are alimented with a battery pack, while the Arduino is alimented with a power supplier. An Arduino SDK that support the **adk.h** library is needed. We used Android Studio for the Java code.

<img src="/ArduRobot 2.png" width="35%" height="35%" alt="Robot Structure" />

The logo that is searched by the robot is our university's one (University of Perugia)

<img src="/ArDroidFollowUp/app/src/main/res/drawable/unipg.png" width="20%" height="20%" alt="UniPG Logo" />

The Android smartphone, after controlling the USB connection with the Arduino platform using the USB Accessory Mode, acquires a frame from the camera using OpenCV APIs. Then it checks for the logo in the image using the OpenCV CascadeClassifier trained with the OpenCV **traincascade** tools. If it matchs somewhere in the frame, the robot computes the movement it has to do and launchs a Thread to send it to the Arduino platform, that actuates it via the ArduMotor extension. Also a version using Feature Detection is provided, but it is less performant that the previous one (that is the default choice).

### Structure
The **./usb_controller/usb_controller.ino** file is the Arduino program, that receives the commands from the phone and send them to the ArduMotor. The **./ArDroidFollowUp** directory contains the Android program files. The **/OpenCV** folder contains the OpenCV dependencies (that had to be imported and configured in Android Studio). The **/app/src/main/java/castellini/jacopo/ardroidfollowup** contains the Java classes. The **ArduThread.java** file is the Thread launched to communicate with the Arduino platform. The **CascadeFinder.java** and the **FeatureScanner.java** files implement respectively the object tracking with the CascadeClassifier and the Feature Detection approaches, while the **MainActivity.java** checks the USB connection and is runned by the smartphone. The **/app/src/main/res/xml/myfilter.xml** file is used to communicate between Arduino and Android, while **/app/src/main/res/raw/unipg_cascade.xml** contains the classifier rules. Other files are the usual files required by Android and Android Studio to work correctly (like the Manifest.xml,layout files or gradle.build files).

### Author
*Castellini Jacopo*

*Thanks to Mirco Tracolli for the Arduino source*
