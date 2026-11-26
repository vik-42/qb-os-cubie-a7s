# Windows: build and flash

Yocto doesn't run on Windows, so the build happens in Linux inside WSL 2 (the Linux system built into
Windows 10 and 11), and Windows itself flashes the card and talks to the board. These steps haven't been
tested by the project yet, the Linux ones in [linux.md](linux.md) have. The Yocto Project says WSL 2 works
but doesn't officially support or test it.

## Build

You need a 64-bit Intel or AMD PC (the build doesn't work on ARM Windows laptops, the board's power
management firmware is built with an x86_64-only compiler), 16 GB of RAM or more, and about 120 GB free on
the drive Windows is on.

1. Install WSL 2 with Ubuntu 24.04. In PowerShell as Administrator:

   ```
   wsl --install -d Ubuntu-24.04
   ```

   Restart when it asks, then open Ubuntu from the Start menu and pick a user name and password.
   `wsl -l -v` in PowerShell should show it with VERSION 2.

2. WSL gives Linux half of the PC's RAM by default. With 32 GB or more that's fine; with less, give it
   more by making `C:\Users\<you>\.wslconfig`:

   ```
   [wsl2]
   memory=12GB
   ```

   and running `wsl --shutdown` in PowerShell, so it starts again with the new size.

3. In the Ubuntu window, follow [linux.md](linux.md) and the README's Build section. Clone the repository
   into your Linux home folder (`~`), not under `/mnt/c`: building on the Windows drive is very slow and
   breaks on Windows' case-insensitive file names.

The SSH key is the one on Windows, since that's where you'll log in from. Windows 10 and 11 come with
OpenSSH, so in PowerShell:

```
ssh-keygen -t ed25519
```

The Windows drive shows up in Ubuntu under `/mnt/c`, so in `site.conf` you can point straight at the key:

```
CUBIE_SSH_AUTHORIZED_KEYS = "/mnt/c/Users/<you>/.ssh/id_ed25519.pub"
```

Make the root password hash in Ubuntu (`openssl passwd -6`).

When the build is done, copy the image to Windows, renamed to `.img` so the flashing programs accept it
(it's the same raw disk image):

```
cp ~/qb-os-cubie-a7s/yocto/build/tmp/deploy/images/cubie-a7s/cubie-a7s-image-jazzy-cubie-a7s.rootfs.wic \
   /mnt/c/Users/<you>/Downloads/cubie-a7s.img
```

WSL's disk file only ever grows, so after a few builds Windows can run out of space even when Ubuntu
shows plenty free. The Yocto docs explain how to shrink it back:
[Optimize your WSL 2 storage](https://docs.yoctoproject.org/5.0/dev-manual/start.html#setting-up-to-use-windows-subsystem-for-linux-wsl-2).

## Flash

WSL can't see the SD card reader, so flash from Windows with [balenaEtcher](https://etcher.balena.io/):
pick `cubie-a7s.img`, pick the card (check the size, it overwrites whatever drive you choose), Flash.
[Rufus](https://rufus.ie/) works too.

When it's done Windows may say the card needs formatting: click Cancel, the card is fine, Windows just
doesn't know the Linux filesystem.

## Serial console

Plug in the USB-serial adapter and open Device Manager: it's under Ports (COM & LPT), for example `COM3`.
If it shows up with a warning sign instead, install the driver from the chip maker (FTDI, Silicon Labs
or WCH).

In [PuTTY](https://www.putty.org/) pick Serial, put in the COM port and speed `115200`, Open.

## Log in over the network

In PowerShell: `ssh root@<address>`, with the address from your router or from `ip addr show wlan0` on the
serial console.
