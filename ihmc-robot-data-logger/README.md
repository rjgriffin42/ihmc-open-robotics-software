IHMC Robot Data Logger
======================

## Logger computer system requirements

- Tons of hard drive space
	- The logger can take 100GB/hour easily, depending on the number of variables and video streams.
- 4 GB RAM (8 GB when more than one video stream is captured)
- 2.5GHz or faster Intel i7 or Xeon E3, E5 or E7
	- 1 + n cores, where n is the number of video streams
- (optional) BlackMagic Decklink Mini Recorder capture cards
	- [https://www.blackmagicdesign.com/products/decklink](https://www.blackmagicdesign.com/products/decklink)
	- The other Decklink capture cards might work, but are not tested yet.

## Setting up a logging computer's dependencies

- Disable secure boot (Or follow instructions during the Ubuntu installation to enable third party drivers)
- Install Ubuntu 16.04 64 bit (Server is recommended, no need for a GUI)
	- Make sure to install OpenSSH server
- Install IHMC Java Decklink dependencies and Java 8.
    - `sudo apt-get install libavformat-ffmpeg56 libavcodec-ffmpeg56 libswscale-ffmpeg3 libboost-thread1.58.0 openjdk-8-jre`
- (If logging video streams with capture card) Install BlackMagic software
    - Get "Desktop Video 10.8.5" for Linux from [https://www.blackmagicdesign.com/support/family/capture-and-playback](https://www.blackmagicdesign.com/support/family/capture-and-playback).
        - You do not need the SDK, just the plain Desktop Video product. The registration has a "Download only" link in the bottom left to bypass.
    - Untar Desktop video: `tar xzvf Blackmagic_Desktop_Video_Linux_10.8.5.tar.gz`
    - Install debian packages: `sudo dpkg -i Blackmagic_Desktop_Video_Linux_10.8.5a4/deb/amd64/desktopvideo_10.8.5a4_amd64.deb`
- (If logging video streams with capture card) Update Blackmagic firmware for each Decklink card (first card is 0, second 1, etc).
    - `BlackmagicFirmwareUpdater update [Decklink card]`
- Reboot the computer

## Publishing the logger from source

- clone ihmc-open-robotics-software
- `cd ihmc-open-robotics-software/ihmc-robot-data-logger`
- `../gradlew deployLogger -PdeployLoggerHost=[Hostname or IP of logger] -PdeployLoggerUser=[SSH username] -PdeployLoggerPassword=[SSH password]`

## Finishing the logger configuration

Create the directory to log to (either logging locally to the computer or logging to a network volume)

        mkdir ~/robotLogs

### Logging to a Network volume

If you would like to log to a network volume, ~/robotLogs can be a symbolic link to a mount point. The IHMC convention is to create a RobotLogs/incoming directory on the network storage volume, auto-mount this volume using the OS's fstab, and then symlinking the "incoming" directory to be the ~/robotLogs directory

### Customizing the logging location
If you would like to log somewhere other than ~/robotLogs, you can change the directory using the "-d" command line flag when you launch the logger

## Setting up robot controller network configuration

To enable logging and selecting which Decklink cards are captured by the logger, the controller needs to be configured using `~/.ihmc/IHMCLoggerConfiguration.ini`.

Create `~/.ihmc/IHMCLoggerConfiguration.ini` and add the following

```
publicBroadcast=true
camerasToCapture=[comma separated list of Decklink card ID's to capture]
```

- `publicBroadcast=true` enables the controller to announce its existance over the network.
- `camerasToCapture=...` informs the logger which cameras to use for this contorller. 

The Decklink cards are numbered sequentially starting from 0. Video resolution is auto-detected. If you want to log Decklink cards 0 and 2, set "loggedCameras=0,2". If you only want to log Decklink card 1, set "loggedCameras=1". If you do not want to log any cameras, remove the loggedCameras line or set it to an empty string.

Alternatively, you can add `-Dihmc.publicBroadcast=true` and `-Dihmc.camerasToCapture` to the java command line arguments.
 
## Starting the logger

From the logger computer:

    ~/IHMCLogger/bin/IHMCLogger

You can also add the IHMCLogger/bin directory to your PATH. The logger will listen for controller sessions coming online. It will dispatching logging sessions accordingly. There is no need to restart the logger unless it crashes.

If logged in over SSH, it is recommended to use screen to start the logger.

## Set To Run On Boot

Create a new file `/etc/systemd/system/logger.service` with the following contents:
```
[Unit]
Description=IHMC Logger
After=network.target

[Service]
User=<user>
ExecStart=/home/[user]/IHMCLogger/bin/IHMCLogger

[Install]
WantedBy=multi-user.target
```

Then reload systemd and enable the logger

- `sudo systemctl daemon-reload`
- `sudo systemctl enable logger`
