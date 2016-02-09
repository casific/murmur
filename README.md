Murmur (formerly project Rangzen) is an open-source, anonymous messaging app that does not require an Internet connection. Messages are not sent in real-time like with other messaging apps that rely on the Internet and have a central server, but instead spread directly from one device to another (forming a peer-to-peer network) without user intervention using Bluetooth and WiFi Direct. The more devices the faster the message spreads! If no device is around, the message is queued in the feed to be sent later. You control your anonymity and decide what information to share about yourself. Lastly, Connection Scores help you filter spam messages and Restricted Messages limit your audience to your friends.

## Background and Design Overview
Please refer to the technical paper in docs for a detailed description of the motivation for the project and an overview of the initial app design. Much more functionality was added since then but it provides a good thorough review of the scaffolds.

## Network layer exchange protocol
When the app is in Online mode it advertises the fact that it is running Murmur via the Wifi Direct device name following this format MURMUR:Bluetooth MAC. At the same time, it constantly seeks Murmur peers using the WiFi Direct discovery mechanism. Upon finding a peer, the app parses the Bluetooth MAC out of the WiFi Direct device name and attempts to create a Bluetooth socket with the peer device. If the connection is successful, a message exchange is attempted between the two devices.

Each exchange starts with a handshake in which each device sends its contacts list and then computes the amount of shared contacts using a cryptographic protocol called Private Set Intersection. Afterward, a short message is sent stating how many messages are to expected in this message exchange. Each device in turn continues to send messages until reaching the agreed number of messages in the handshake or until the connection times out. Each message might include aside for the message body additional information such as location tagging, timestamp, etc based on the settings on the sending device. 
When the exchange is completed, the received messages are saved to the local database and a backoff algorithm is initiated in order to conserve battery life and avoid redundant exchanges with same peer if no actual messages were added to the feed. 

## Major classes and packages
The Network layer consist of 3 main classes:  
* MurmurService - service running in the background, listening to WiFi Direct and Bluetooth changes and attempting to perform exchange when peer(s) identified. This is the main process of the network layer and must be running to provide network functionality.  
* BluetoothSpeaker - An event handler for the Bluetooth hardware, this class is in charge of initiating calls to the Bluetooth hardware, starting a connection and create a listening socket for incoming transmissions  
* WifiDirectSpeaker - An event handler for the WifiDirect hardware, this class is in charge of running scans on WiFi Direct network.

UI Structure:
Murmur uses one primary Activity called MainActivity as the main source for all its UI screens. This class provides the basic handling of the DrawerMenu widget and sets the stage for other screens supplied as Fragments. Running this Activity will launch the MurmurService which remains alive even after the app is closed.

## Security Audit
Murmur passed a full 3rd party security audit by iSEC, see docs for the report outputs. All major findings have been resolved.

## Installation notes
Murmur is built using Android Studio using java 1.8 plugin. To setup your own build:  
1. Clone the repository  
2. Open android studio  
3. From the splash menu select Open existing android studio project  
4. Navigate to where the clone is located and select the build.gradle file  
5. wait for android studio to complete building the project (will require internet connection)  

# Troubleshooting
If you have any issues setting up your build environment consider the following:  
1. Update your java plugin to 1.8 or higher  
2. Ensure you have the latest build tools version from the sdk manager  

## Dependencies / 3rd party libraries
* Android support library  
* Android AppCompat extension  
* ZXing : used in QR-code read/creation  
* Okio : used in cryptographic engine of the storage and network layer  
* Spongycastle : used in cryptographic engine of the storage layer  
* SystemBarTint : provides translucent theme for older API levels  
* Log4J : used to create and send logs as part of the Send Feedback feature  
