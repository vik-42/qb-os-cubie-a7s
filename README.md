# QB OS for the Radxa Cubie A7S

QB OS is a Yocto (scarthgap) Linux distribution for the Radxa Cubie A7S. It ships with:

- onboard wifi
- the A733 NPU, usable from standard runtimes (TIM-VX, ONNX Runtime)
- hardware video through GStreamer
- the GPU through Mesa (GLES, EGL, GBM), Vulkan and OpenCL

QB OS comes as four images you can build. All of them have the features above, and each one adds
something on top:

| Image | What it adds | Build type |
|---|---|---|
| `cubie-a7s-image` | ROS 2 Jazzy over Zenoh, Docker | release |
| `cubie-a7s-image-dev` | same as above | dev: passwordless root, dnf |
| `cubie-a7s-image-base` | nothing, no ROS | release |
| `cubie-a7s-image-playground` | NPU, video and GPU samples, no ROS | dev |

Release images need an SSH key or a root password set at build time (see [Logging in](#logging-in)).
Dev images have no root password, so keep them on your desk and never ship them.

> **Unofficial community project.** Not made, endorsed or supported by Radxa, Allwinner,
> Imagination Technologies, VeriSilicon, the Yocto Project or any other company or project whose
> software it builds. Product and project names belong to their owners and are used only to say
> what hardware and software this works with.

## Build

These instructions are for Linux. What to install first: [docs/linux.md](docs/linux.md). On a Mac or a
Windows PC: [docs/macos.md](docs/macos.md), [docs/windows.md](docs/windows.md).

```
git clone --recurse-submodules https://github.com/vik-42/qb-os-cubie-a7s.git
cd qb-os-cubie-a7s/yocto
source poky/oe-init-build-env build
bitbake cubie-a7s-image              # ROS image, release
bitbake cubie-a7s-image-dev          # ROS image, dev (passwordless root, dnf)
bitbake cubie-a7s-image-base         # no ROS, release
bitbake cubie-a7s-image-playground   # no ROS, dev, plus NPU samples for experimenting
```

`build/conf` sets `DISTRO = "qb"` (QB OS) and has both layers: `meta-cubie-a7s` is the board
support (kernel, bootloader, drivers, GPU/NPU/video stacks, header and cooling options) and works with
plain poky, `meta-cubie-robot` is the product on top (the QB OS distro, the images, ROS 2 over Zenoh,
ONNX Runtime, network setup).

x86_64 build host only. Output: `build/tmp/deploy/images/cubie-a7s/*.wic` (the ROS one is named `cubie-a7s-image-jazzy-*.wic`, meta-ros adds the distro)

## Flash

The board boots from a microSD card. Put the card in your computer and find its device with `lsblk`:
a built-in reader usually shows it as `/dev/mmcblk0`, a USB reader as `/dev/sdX`. Double-check it,
`dd` overwrites whatever disk you point it at.

```
sudo umount /dev/mmcblk0p*
sudo dd if=<image>.wic of=/dev/mmcblk0 bs=4M status=progress conv=fsync
```

The first boot grows the root partition and filesystem to the end of the card, once (`rootfs-grow` in the
boot log).

### Serial console

Optional, but it's the only way to watch the boot log and to get a shell before the board is on the
network, so it's the first thing to try when something doesn't come up. You need a 3.3V USB-serial
adapter (FTDI, CP2102, CH340, ...) and three wires on the board's 30-pin header. Pin 1 is the one with
the triangle mark:

![Cubie A7S 30-pin header with the UART0 pins and two USB-serial adapters](https://docs.radxa.com/en/img/cubie/a7s/a7s-uart-login.webp)

*Picture from Radxa's [serial login guide](https://docs.radxa.com/en/cubie/a7s/system-config/uart-login).*

```
              +----+----+
        3.3V  |  1 |  2 |  5V
              |  3 |  4 |  5V
              |  5 |  6 |  GND  -> adapter GND
              |  7 |  8 |  TX   -> adapter RX
         GND  |  9 | 10 |  RX   -> adapter TX
              |  . |  . |
              | 29 | 30 |
              +----+----+
```

TX and RX cross over. Leave the adapter's power pin unconnected, the board powers itself from its USB-C
port. Then open the adapter's port at 115200 baud, 8N1, no flow control:

```
picocom -b 115200 /dev/ttyUSB0       # or: screen /dev/ttyUSB0 115200
```

On the board this port is `ttyAS0`.

### Logging in

The user is `root`. Dev images (`-dev`, playground) have no password, on the serial console and over SSH.

Release images refuse to build until you give them a login in `yocto/build/conf/site.conf` (see
Per-machine settings below): an SSH key for root, a root password for the serial console, or both.
SSH takes the key only, password login over SSH is off.

```
# on your computer, once: an SSH key pair, skip if ~/.ssh/id_ed25519.pub already exists
ssh-keygen -t ed25519

# the root password for the serial console, prints a hash starting with $6$
openssl passwd -6
```

Then in `site.conf`:

```
CUBIE_SSH_AUTHORIZED_KEYS = "/home/you/.ssh/id_ed25519.pub"
CUBIE_ROOT_PASSWORD_HASH = "$6$..."
```

The image only holds the public key and the hash, never the private key or the password. Changing
either needs a rebuild and a reflash.

The board joins the wifi from `WIFI_SSID`/`WIFI_PSK` and gets its address by DHCP: look it up in your
router, or run `ip addr show wlan0` on the serial console. Then `ssh root@<address>`. Every flash makes
new SSH host keys, so after reflashing remove the old one with `ssh-keygen -R <address>`.

## Custom options

### Board layer (`yocto/meta-cubie-a7s`)

| What | Where | Why |
|---|---|---|
| Kernel builds in place (`B = S`) | `linux-radxa_6.6.98.bb` | separate build dir loops forever reconfiguring |
| Kernel config from the reference board | `linux-radxa/known-good-kernel.config` | Radxa's merge step needs private files |
| Wifi driver (aic8800) built inside the kernel recipe | `linux-radxa_6.6.98.bb` | out of tree modules hit the same loop in `make-mod-scripts` |
| Radxa aic8800 patches applied with CRs stripped | `linux-radxa_6.6.98.bb` | sources are CRLF, patches aren't |
| dtb also installed as `allwinner/<dtb>` | `cubie-a7s.conf` `IMAGE_BOOT_FILES` | U-Boot looks there, else uses its own dtb and nothing works |
| `scp.bin` built and passed to U-Boot | `scp-firmware_git.bb`, `u-boot-radxa_2026.04.bb` | without it boot hangs at `wait arisc ready` |
| SPL loads U-Boot proper from 0x4a000 | `u-boot-radxa/spl-a733-uboot-sector.patch` | that's where binman puts it, the SPL used its own size and missed it when the SPL is small |
| Boot args `console=ttyAS0 irqchip.gicv3_nolpi=1` | `wic/cubie-a7s.wks` | vendor uart name; GIC ITS hangs boot |
| `lttng-modules` dropped | `lttng-tools_%.bbappend` | lttng-modules can't build against this kernel |
| Header tools only load with meta-oe | `conf/layer.conf` `BBFILES_DYNAMIC` | layer works with plain poky |
| NPU driver is `galcore` (module, autoloaded), not the stock `vipcore` | `known-good-kernel.config`, `linux-radxa_6.6.98.bb` | the OpenVX stack only talks to galcore |
| galcore reports version 6.4.15 | `linux-radxa/galcore-report-6.4.15.patch` | the only public userspace is 6.4.15 and refuses anything else, ioctl layout is identical |
| galcore routes the shared SRAM to the NPU | `linux-radxa/galcore-a733-sram.patch` | vipcore does this at probe, galcore didn't, NPU hung on every job |
| Prebuilt OpenVX libs from Radxa's ai-sdk | `recipes-bsp/viv-unified-driver` | only userspace for this NPU, gcc 13 build |
| TIM-VX (lenet sample in `tim-vx-samples`, playground image only) | `recipes-support/tim-vx` | graph API that ONNX Runtime's NPU provider sits on |
| GPU driver `pvrsrvkm` built inside the kernel recipe with Imagination's own makefiles | `linux-radxa_6.6.98.bb`, `linux-radxa/img-bxm-*.patch` | same out of tree problem; patches are Radxa's (make 4.4, DVFS on) |
| Closed GPU userspace + firmware from Radxa's xorg package | `recipes-graphics/img-bxm-umlibs` | has to be the exact DDK build of the kernel driver (24.2.6603887) |
| Mesa from Imagination's powervr branch (as TI ships it) | `recipes-graphics/mesa/mesa-pvr_24.0.1.bb` | upstream mesa can't drive the closed libs; recipe from meta-ti |
| Mesa `sunxi-drm` DRI alias | `mesa-pvr` `-Dgallium-pvr-alias`, `0001-pvr_alias-allow-a-dash-...patch` | mesa loads the DRI driver named after the KMS driver |

### Distro and product layer (`yocto/meta-cubie-robot`)

| What | Where | Why |
|---|---|---|
| QB OS distro, started from oe-core's `defaultsetup.conf` | `conf/distro/qb.conf` | Poky is a reference distro for testing, not for shipping |
| Poky's hardening, uninative, hash equivalence and sysvinit kept | `qb.conf` | same build behaviour, minus Poky's dev defaults |
| `ptest` off | `qb.conf` | lttng-tools-ptest needs lttng-modules |
| No `create-spdx` | `qb.conf` `INHERIT_DISTRO` | meta-ros `BSD` license tags break it |
| gst-omx `commercial` flag accepted | `qb.conf` | it drives the video engines, see Video |
| ROS images and Zenoh bits only load with meta-ros jazzy | `conf/layer.conf` `BBFILES_DYNAMIC` | layer works with or without ROS |
| Docker in the ROS images, off at boot | `dynamic-layers/meta-virtualization`, `cubie-a7s-image.bb` | extra nodes in containers, see Docker; only loads with meta-virtualization |
| First boot grows `/` to fill the SD card | `recipes-core/rootfs-grow` | the image only holds what it needs, containers need the space |
| `/etc/os-release` says QB OS | `cubie-a7s-image-common.inc` | rosdep, containers and other tools read it |
| Clock set from the network (chrony) | `cubie-a7s-image-common.inc` | the board has no time source and would start at the build's fixed 2018 timestamp; only with meta-networking |
| `rmw-zenoh-cpp` license is Apache-2.0 & BSD-2-Clause | `rmw-zenoh-cpp_%.bbappend` | the generated recipe only says "BSD" |

### Build config (`yocto/build/conf/local.conf`)

| Setting | Why |
|---|---|
| `MACHINE = "cubie-a7s"`, `DISTRO = "qb"`, `ROS_DISTRO = "jazzy"` | |
| `BB_NUMBER_THREADS = "2"`, `PARALLEL_MAKE = "-j 4"` | avoids OOM on LLVM/clang builds |
| `INHERIT += "rm_work"`, except `linux-radxa` | disk fills up otherwise |

### Per-machine settings (`yocto/build/conf/site.conf`, not tracked)

```
WIFI_SSID = "network"
WIFI_PSK = "password"

# login for release images, the build refuses to make one with neither
CUBIE_SSH_AUTHORIZED_KEYS = "/home/you/.ssh/id_ed25519.pub"
CUBIE_ROOT_PASSWORD_HASH = "<output of: openssl passwd -6>"

# wifi country (ISO code), where the board runs. default "00", the world rules
CUBIE_WIFI_COUNTRY = "IT"

# cooling: "passive" (default, bare board or heatsink) or "fan" (pwm fan on the 3-pin header)
CUBIE_COOLING = "passive"

# header functions, Radxa's A733 overlays merged into the board dt at build time (default: none)
CUBIE_HEADER = "pwm1-1 twi2 uart2"

# optional, dev package feed from your build machine
PACKAGE_FEED_URIS = "http://<build-machine-ip>:8000"
PACKAGE_FEED_BASE_PATHS = "rpm"
PACKAGE_FEED_ARCHS = "cubie_a7s cortexa76_cortexa55 noarch"
```

The wifi key is baked into the image hashed, `wlan0` connects at boot. Without it, connect by hand (see layer README).

`CUBIE_WIFI_COUNTRY` picks the wifi channels and transmit power from the kernel's regulatory database
(`wireless-regdb`). Set it to the country the board runs in: with the default `00` (rules valid everywhere)
2.4GHz channels 12-14 and most of 5GHz are passive scan only and the board can't run an access point on them.
`iw reg get` on the board shows what's in effect.

`CUBIE_COOLING` has to match the hardware, the fan header has no tach line so the board can't tell. Both
profiles share the kernel's thermal trips: a fan starts at 60C, CPUs and GPU throttle at 90C, power is cut
at 110C. `fan` adds the fan driver and keeps the DRAM at full speed. `passive` lets the DRAM clock follow
the load, which keeps a bare board's idle die about 13C cooler (memory-bound work runs ~12% slower). A fan
on a `passive` image never spins, the board is still protected by throttling.

`CUBIE_HEADER` picks what the header pins do. Without it every pin is a plain GPIO (`/dev/gpiochip0`,
`/dev/gpiochip1`). The names are Radxa's overlays for the A733 ([radxa-overlays](https://github.com/radxa-pkg/radxa-overlays),
pinned in `recipes-bsp/cubie-a7s-header`); only the ones Radxa marks for the A7S are accepted, and the build
stops if two of them need the same SoC pin:

| Name | Function | SoC pins |
|---|---|---|
| `pwm1-1`, `pwm1-2`, `pwm1-3` | PWM | PD11, PD12, PD13 |
| `pwm1-6`, `pwm1-7` | PWM | PJ24, PJ25 |
| `twi2`, `twi7` | I2C | PD16/PD17, PJ22/PJ23 |
| `uart2`, `uart3`, `uart4` | UART | PB0/PB1, PJ22/PJ23, PJ24/PJ25 |
| `spi1-spidev` | SPI1 as `/dev/spidev*` | PD10-PD13 |
| `i2s0-2ch` | I2S audio | PB4-PB8 |

Check the header pin numbers against Radxa's [pinout](https://docs.radxa.com/en/cubie/a7s/hardware-use/hardware-info)
before wiring. PWM channels show up under `/sys/class/pwm`.

Every image carries `i2c-tools`, and with meta-oe in the build (the documented setup) also the gpiod tools
(`gpioinfo`, `gpioset`, ...) and the Python libraries `gpiod`, `periphery` (gpio, pwm, i2c, spi, serial),
`smbus2` and `spidev`.

## Dev package feed

```
# laptop
bitbake <recipe> && bitbake package-index
python3 -m http.server 8000 --directory build/tmp/deploy

# board
dnf makecache && dnf install <package>
```

Set `PACKAGE_FEED_URIS` in `site.conf` (above) and rebuild the image, or edit `/etc/yum.repos.d/` on the board, if the IP changes.

## NPU

The NPU (VIP9000, 8 NN cores, int8/int16) runs through galcore, Radxa's prebuilt OpenVX libs and TIM-VX. Quick check on the board:

```
lsmod | grep galcore                  # loaded at boot
ls -l /dev/galcore                    # crw-rw-rw-
```

ONNX Runtime with the NPU provider is in every image (`python3 -c "import onnxruntime"`). Samples
and test programs are not: to run an actual network, flash the playground image and run
`tim-vx-lenet`, Top5 should start with `0: 0.999628`.

The board is passively cooled, keep an eye on `/sys/class/thermal/thermal_zone*/temp` during long NPU/CPU loads.

## Video

The video engines run through Allwinner's CedarC libraries (prebuilt, from Radxa) and GStreamer's
`gst-omx`, built from source with six patches (`recipes-multimedia/gstreamer/gstreamer1.0-omx/`); the per-element
workarounds in `gstomx-a733.conf` are each commented with why CedarC needs them.
Every image has these hardware elements:

| Element | | Checked against ffmpeg (720p) |
|---|---|---|
| `omxh264dec` | H.264 decode | bit-exact, ~170 fps at 1080p |
| `omxvp8dec` | VP8 decode | bit-exact |
| `omxmpeg2videodec`, `omxmpeg4videodec`, `omxh263dec`, `omxmjpegdec` | decode | 70+ dB (IDCT rounding) |
| `omxh264enc`, `omxh265enc` | encode from NV12 | 37-38 dB at 4 Mbit/s |

Decoders output YV12 without copying: each buffer is the decoder's own frame as a dmabuf, so
dmabuf-aware elements import it directly, and CPU elements map it (it stays mapped). Frames are
padded to 16 lines (720p is 736), elements that don't take `GstVideoMeta` get a copy. Setting
`NONE_ZERO_COPY_MODE=1` in the environment switches back to CedarC copying each frame out. Example:

```
gst-launch-1.0 filesrc location=in.h264 ! h264parse ! omxh264dec ! videoconvert ! ...
gst-launch-1.0 v4l2src ! video/x-raw,format=NV12,width=1280,height=720 ! omxh264enc target-bitrate=4000000 ! h264parse ! ...
```

`/dev/cedar_dev*` and `/dev/dma_heap/system` are `root:video` 0660, add users to `video`.

gst-omx is flagged `commercial` in oe-core (H.264/H.265 patents), the QB OS distro accepts that flag for it.
Shipping a product with these codecs is on you to license.

## GPU

The GPU (Imagination BXM-4-64, up to 1 GHz) runs Imagination's DDK 24.2: the `pvrsrvkm` kernel
driver from the Allwinner BSP, its closed userspace and firmware, and Mesa's PowerVR frontend.
Every image has:

| API | Through | |
|---|---|---|
| OpenGL ES 3.2, EGL 1.5, GBM | Mesa (`libEGL`, `libGLESv2`, `libgbm`) | the usual DRM/KMS + GBM path, no X11/Wayland in the image |
| Vulkan 1.3 | `vulkan-loader`, ICD `libVK_IMG.so` | |
| OpenCL 3.0 | `opencl-icd-loader`, ICD `libPVROCL.so` | |

Quick check on the board:

```
dmesg | grep 'Initialized pvr'        # driver up, firmware loads on first use
vulkaninfo --summary; clinfo          # playground image
```

Headless rendering works on `/dev/dri/renderD128` (EGL on GBM, surfaceless context). The playground
image also has `kmscube` and `glmark2-es2-drm` for a display on the USB-C DisplayPort output (it can be plugged
in and out while running). Both can pick the GPU's node by themselves, point them at the display:
`kmscube -D /dev/dri/card0` and `glmark2-es2-drm --winsys-options drm-device=/dev/dri/card0`. A bare board can't hold a 4K GPU load: with
everything throttled it still went from 80C to 98C in about two minutes, use a heatsink or fan for that.

The GPU runs at its top clock (1008 MHz) and powers down when idle. It's the cooling device of its
own thermal zone (the GPU's sensor), so it steps down to 400 MHz when the GPU reaches 90C (see `CUBIE_COOLING`).

## Docker

The ROS images have Docker, off at boot so it costs nothing until it's used:

```
/etc/init.d/docker.init start          # now
update-rc.d docker.init defaults       # at every boot
```

Containers talk to the board's ROS 2 over Zenoh, so they need ROS 2 Jazzy with `rmw_zenoh_cpp`
(`RMW_IMPLEMENTATION=rmw_zenoh_cpp`) and a router running on the board (`ros2 run rmw_zenoh_cpp rmw_zenohd`).
Stock `ros:jazzy` images default to DDS, install `ros-jazzy-rmw-zenoh-cpp` in them. Ours is rmw_zenoh 0.2.9 on
Zenoh 1.6.2, keep the container's close to it.

- `--network host`: the container reaches the router on `localhost:7447` like a native node, no extra setup
  and no extra copies, use it for anything that moves images or point clouds
- default bridge network: point the container at the board's router with
  `-e ZENOH_CONFIG_OVERRIDE='connect/endpoints=["tcp/172.17.0.1:7447"]'`

Keep hardware access (NPU, GPU, video, PWM/GPIO) in native nodes, a stock container doesn't have the
board's userspace drivers.

## Before production

- Ship release images only, never `-dev` or playground (passwordless root, dnf)
- Drop `PACKAGE_FEED_*` from `site.conf` (only used by dev images anyway)

## Known issues

- Boot log shows `BL31: v2.5(debug)`: the ARM Trusted Firmware is Radxa's prebuilt debug build, there's no source to rebuild it
- No out of tree kernel modules (`make-mod-scripts` loops), build them in `linux-radxa` instead
- NPU: matmul / fully connected operands must be uint8, batched `MatMul` doesn't run on it (reported by other A733 users, not yet checked here)
- NPU: converting from float (standalone QuantizeLinear, i.e. a float model input) fails to initialize on the NPU, the driver's shader tensorcopy is broken here, cause unknown, so ONNX Runtime does it on the CPU (`vsinpu-a733-quantize-on-cpu.patch`). Quantized layers and uint8->float still run on the NPU
- Video: no H.265 decode, CedarC's `libawh265.so` crashes on every HEVC stream (its own `vdecoderdemo` too, same lib in every CedarC release Radxa ships)
- Video: the encoders drop the last 2 frames at end of stream
- Video: CedarC can't free a decoder that never decoded a frame. One that got no data is kept and reused by the next pipeline, one that got data it couldn't decode (unsupported or corrupt stream) stays allocated until the process exits, with a warning
- Video: the encoders log `no empty AllocateInputBuffer` for every wait when the source is faster than real time (file transcoding), harmless

## Closed binaries and credits

The board needs some closed binaries to boot and to use its hardware. None of them are in this
repository: the build downloads each one from its publisher, pinned by commit or checksum, the same
way it downloads the open source code. Thanks to Radxa for publishing them, and to the companies
that made them:

| Binary | What it does | Made by | Downloaded from |
|---|---|---|---|
| BL31 (`bl31.bin`) | ARM Trusted Firmware, runs before U-Boot | Allwinner | [radxa-pkg/u-boot-dlan17](https://github.com/radxa-pkg/u-boot-dlan17) |
| DRAM init (`ddr.bin`) | trains the RAM at power on | Allwinner | [radxa-pkg/u-boot-dlan17](https://github.com/radxa-pkg/u-boot-dlan17) |
| SCP firmware (`scp.bin`) | power management core; built from Radxa's sources with a prebuilt DRAM library and RISC-V toolchain | Allwinner | [radxa/allwinner-arisc](https://github.com/radxa/allwinner-arisc), [allwinner-dramlib](https://github.com/radxa/allwinner-dramlib), [allwinner-toolchain](https://github.com/radxa/allwinner-toolchain) |
| Wifi/Bluetooth firmware | AIC8800D80 radio | AICSemi | [radxa-pkg/aic8800](https://github.com/radxa-pkg/aic8800) |
| GPU userspace and firmware (DDK 24.2) | GLES, Vulkan and OpenCL drivers | Imagination Technologies | [radxa/allwinner-debian](https://github.com/radxa/allwinner-debian) (`xserver-xorg-img-bxm`) |
| NPU OpenVX libraries (6.4.15) | NPU runtime under TIM-VX | VeriSilicon | Radxa's ai-sdk, [ZIFENG278/ai-sdk](https://github.com/ZIFENG278/ai-sdk) |
| CedarC | video encode and decode libraries | Allwinner | [radxa/allwinner-debian](https://github.com/radxa/allwinner-debian) (`libcedarc`) |

The recipes are `u-boot-radxa`, `scp-firmware`, `aic8800-firmware`, `img-bxm-umlibs`, `viv-unified-driver`
and `libcedarc` in `meta-cubie-a7s`. A built image contains these binaries, so whoever hands out a built
image passes them on under their owners' terms.

## License

The project's own files (layers, recipes, configuration, scripts, docs) are under the MIT license, see
`LICENSE`. Patches are under the license of the code they change (e.g. the kernel patches are GPL-2.0).
Everything the build downloads keeps its own license: open source code like the kernel, U-Boot, Mesa or
ROS 2, and the closed binaries listed above.
