# macOS: build and flash

Yocto doesn't run on macOS, so the build happens on Linux and the Mac only flashes the card and talks to
the board. These steps haven't been tested by the project yet, the Linux ones in [linux.md](linux.md) have.

## Build

You need an x86_64 Linux system to build on, the board's power management firmware is built with an
x86_64-only compiler from Radxa.

- **Intel Mac:** make a Linux virtual machine (UTM, VirtualBox, Parallels or VMware Fusion) with Ubuntu
  24.04, give it at least 16 GB of RAM, as many CPU cores as you can spare and a 120 GB disk. Inside it,
  follow [linux.md](linux.md) and the README's Build section. Keep the build on the VM's own disk, not
  in a folder shared with macOS: the Mac's filesystem doesn't tell upper and lower case apart, and the
  build breaks on that.
- **Apple Silicon Mac (M1 and later):** the VMs run ARM Linux, which can't run that compiler. Emulating a
  whole x86_64 machine works in principle but is far too slow for a build this size. Build on another
  x86_64 Linux machine or a rented cloud server (Ubuntu 24.04, 16 GB RAM, 120 GB disk), then copy the
  image over.

`site.conf` goes in the Linux system, and the root password hash should be made there too
(`openssl passwd -6`): the `openssl` that comes with macOS is LibreSSL, which may not support `-6`.

The SSH key is the one on your Mac, since that's where you'll log in from. Make it on the Mac if you
don't have one, then copy the public half into the Linux system and point `CUBIE_SSH_AUTHORIZED_KEYS` at
it:

```
ssh-keygen -t ed25519                              # on the Mac, skip if ~/.ssh/id_ed25519.pub exists
scp ~/.ssh/id_ed25519.pub you@<linux-machine>:     # then in site.conf: "/home/you/id_ed25519.pub"
```

When the build is done, copy the image to the Mac:

```
scp you@<linux-machine>:qb-os-cubie-a7s/yocto/build/tmp/deploy/images/cubie-a7s/cubie-a7s-image-jazzy-cubie-a7s.rootfs.wic .
```

## Flash

Put the microSD card in the Mac and find it:

```
diskutil list
```

It's the one marked `(external, physical)` with the card's size, for example `/dev/disk4`. Double-check
the number, `dd` overwrites whatever disk you give it. Then, with your number in place of 4:

```
diskutil unmountDisk /dev/disk4
sudo dd if=cubie-a7s-image-jazzy-cubie-a7s.rootfs.wic of=/dev/rdisk4 bs=4m
diskutil eject /dev/disk4
```

`rdisk` is the same card without macOS's caching, much faster. `dd` prints nothing while it works, press
Ctrl+T to see how far it got. If macOS says afterwards that it can't read the disk, click Ignore or Eject,
the card is fine, macOS just doesn't know the Linux filesystem.

If you'd rather use a program, [balenaEtcher](https://etcher.balena.io/) writes the same image: rename
the file from `.wic` to `.img` first so it accepts it, the content is the same raw disk image.

## Serial console

Plug in the USB-serial adapter and look for it:

```
ls /dev/tty.usb*
```

FTDI and CP2102 adapters show up as `/dev/tty.usbserial-...`, CH340 ones as `/dev/tty.wchusbserial...` or
`/dev/tty.usbserial-...`. If nothing appears, install the driver from the chip maker (FTDI, Silicon Labs
or WCH). Then:

```
screen /dev/tty.usbserial-XXXX 115200
```

Leave `screen` with Ctrl+A then K, then Y.

## Log in over the network

Same as on Linux: `ssh root@<address>`, with the address from your router or from `ip addr show wlan0`
on the serial console.
