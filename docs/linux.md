# Linux: what to install

These are the instructions for building and flashing on Linux, the only system the build runs on
directly. On a Mac or a Windows PC see [macos.md](macos.md) or [windows.md](windows.md).

## The computer

- **An x86_64 Linux machine.** Yocto only builds on Linux, and the firmware for the board's power
  management core is built with Radxa's prebuilt RISC-V compiler, which only runs on x86_64. So an ARM
  machine (a Raspberry Pi, an Apple Silicon Mac's Linux VM) won't do.
- **A distro Yocto supports**: Ubuntu 22.04 or 24.04, Debian 12, Fedora 40/41 and a few others, the full
  list is in the [Yocto docs](https://docs.yoctoproject.org/5.0/ref-manual/system-requirements.html#supported-linux-distributions).
- **About 100 GB of free disk.** A full build takes about 66 GB (7 GB of downloaded sources, 26 GB of
  build cache, 33 GB of work files) and grows a bit with every image you build. An SSD helps a lot.
- **16 GB of RAM or more.** Clang and LLVM are part of the build and take a lot of memory per compile job.
  `yocto/build/conf/local.conf` keeps the parallel jobs low on purpose (2 recipes at once, 4 compile jobs
  each); with more RAM you can raise them.
- **Internet.** The first build downloads all the source code and the closed binaries the board needs.
  It takes several hours; later builds only redo what changed.

## Packages

On Ubuntu or Debian:

```
sudo apt install build-essential chrpath cpio debianutils diffstat file gawk gcc git iputils-ping \
    libacl1 liblz4-tool locales python3 python3-git python3-jinja2 python3-pexpect python3-pip \
    python3-subunit socat texinfo unzip wget xz-utils zstd
```

This is the list from the Yocto docs for this release. Yocto also needs the `en_US.UTF-8` locale, check
with `locale -a | grep en_US.utf8`; if nothing shows up, run `sudo dpkg-reconfigure locales` and tick it.

On Fedora and others, the package lists are in the
[Yocto docs](https://docs.yoctoproject.org/5.0/ref-manual/system-requirements.html#required-packages-for-the-build-host).

## Ubuntu 24.04 only: let bitbake use user namespaces

Ubuntu 24.04 blocks unprivileged user namespaces through AppArmor, and bitbake uses them to cut each
build step off from the network. Without this bitbake stops right away with "User namespaces are not
usable by BitBake, possibly due to AppArmor":

```
echo 'kernel.apparmor_restrict_unprivileged_userns = 0' | sudo tee /etc/sysctl.d/60-yocto-userns.conf
sudo sysctl --system
```

This is a system-wide setting, it brings back the behaviour of older Ubuntu releases. Delete the file and
reboot to undo it.

## For flashing and the first login

- **A microSD card**, 8 GB or more. The images are 1-1.5 GB and the first boot grows the system to fill
  the card, so a bigger card leaves room for containers and logs.
- **A card reader.** `dd` is already on every Linux system.
- **A USB-C power supply** for the board, 5V.
- **Optional, a 3.3V USB-serial adapter** for the serial console (wiring in the README). To open it you
  need a terminal program and access to the port:

  ```
  sudo apt install picocom
  sudo usermod -aG dialout $USER       # log out and back in once, then no sudo needed for /dev/ttyUSB0
  ```

- **An SSH key** for logging in to release images, made with `ssh-keygen` (see Logging in in the README).

Then go back to the README: Build, then Flash.
