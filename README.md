<p align="center">
  <img align="center" src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp" alt="Frida Mgr Logo"/>
</p>

<p align="center">
  <img alt="Github Release" src="https://img.shields.io/github/v/release/Yimura/FridaMgr">
  <img alt="Android Version" src="https://img.shields.io/badge/Android_Version-10%2B-blue?logo=android">
  <img alt="Release Build" src="https://github.com/Yimura/FridaMgr/actions/workflows/release.yml/badge.svg">
</p>

Frida Manager is an Android Application made to make the installation and management of the frida-server binary.

- [Functionality](#functionality)
- [How To Use](#how-to-use)
  - [USB](#usb)
  - [Remote](#remote)
- [Images](#images)
- [Tested Devices](#tested-devices)
  - [Physical](#physical)
  - [Emulator](#emulator)
  - [Contributors](#contributors)
- [Common Errors](#common-errors)
  - [need Gadget to attach on jailed Android; ...](#need-gadget-to-attach-on-jailed-android-)
  - [unexpectedly timed out while waiting for signal from process with PID XXX](#unexpectedly-timed-out-while-waiting-for-signal-from-process-with-pid-xxx)

## Functionality

- Installation and Updating of Frida Server
- Start server automatically on boot
- Listen on network interface
- Easily start/stop server from Quick Settings

## How To Use

Open the application and press the start server button (if the start button is not visible you need to install the server first), this will launch the Frida-Server on the device.
If your device is connected via USB then proceed [here](#usb). If you currently do not have a USB cable on you then you can enable [Network Mode](#remote).

### USB

If you've used Frida before then you'll be the most familiar with this method, the following test command should work as expected:

```bash
frida -U -f sh.damon.fridamgr
```

`-U` : stands for USB connected device, optionally if multiple devices are connected you may need to specify this with the `--device=` flag

### Remote

If you've enabled **Listen on Network Interface** in the FridaMgr app then you'll be able to connect to the Frida Server on the device remotely as long as you're within the same LAN.

```bash
frida -H 192.168.88.7:27055 -f sh.damon.fridamgr
```

`-H` : tells Frida that you want to connect to a network connected device.

## Images

- Before installing Frida Server
  ![Frida Mgr Installation Screen](docs/imgs/install_screen.png)

- When Frida Server is installed and running
  ![Frida Mgr Post-Installation Screen](docs/imgs/post_install_screen.png)

## Tested Devices

The following list of physical and emulated devices have been tested and verified to be working.

### Physical

- Samsung Galaxy Z Fold3 (sdk33)
- One Plus 11 5G (sdk33)
- One Plus 6 (sdk30)

### Emulator

- Google Pixel 8 (sdk33)

### Contributors

- [Yimura](https://x.com/Yimura9)
- [drop](https://x.com/dropn0w)

## Common Errors

### need Gadget to attach on jailed Android; ...

Make sure the Frida Server is running in the FridaMgr application.

### unexpectedly timed out while waiting for signal from process with PID XXX

Try killing and starting the Frida-Server a few times, some devices make use of a process called USAP which allows applications to start up quicker.
However this interferes with Frida-Server being able to launch applications.

If the problem persist consider opening an issue with your device model and operating system version mentioned.
