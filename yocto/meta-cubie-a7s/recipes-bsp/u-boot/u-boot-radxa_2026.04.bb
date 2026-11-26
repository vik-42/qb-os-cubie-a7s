SUMMARY = "U-Boot bootloader for the Radxa Cubie A7S (Allwinner A733)"
DESCRIPTION = "Built from Radxa's own actively-maintained U-Boot fork, not mainline \
U-Boot, the same source that already booted this exact board during our reference \
flash test. Reuses Yocto's own generic U-Boot build logic (u-boot.inc), only \
supplying our own source location."
HOMEPAGE = "https://github.com/radxa/u-boot"
SECTION = "bootloaders"

LICENSE = "GPL-2.0-or-later"
LIC_FILES_CHKSUM = "file://Licenses/README;md5=2ca5f2c35c8cc335f0a19756634782f1"

DEPENDS += "flex-native bison-native python3-setuptools-native bc-native dtc-native python3-pyelftools-native gnutls-native"

# pinned past the a733 spl sd fix (d45e03c7), AUTOREV made it impossible to tell if we had it
SRCREV = "de9bbde8c21cb1000f6d05630b7160664d8d5478"
SRC_URI = "git://github.com/radxa/u-boot.git;protocol=https;branch=u-boot-dlan17 \
    https://raw.githubusercontent.com/radxa-pkg/u-boot-dlan17/main/awbin/a733/bl31.bin;name=bl31;downloadfilename=bl31.bin \
    https://raw.githubusercontent.com/radxa-pkg/u-boot-dlan17/main/awbin/a733/ddr.bin;name=ddr;downloadfilename=ddr.bin \
    file://spl-a733-uboot-sector.patch \
"
SRC_URI[bl31.sha256sum] = "b009aad73508d53ee3313ba61e4a2a3a7c510506ad39968d635c05a81910e002"
SRC_URI[ddr.sha256sum] = "d89804f404f2eef6ef5b03d82a310ef0906c4db8ded760be221b131f5c813a05"

PV = "2026.04"

# radxa's a733 fork, this board only
COMPATIBLE_MACHINE = "cubie-a7s"
S = "${WORKDIR}/git"
B = "${WORKDIR}/build"

inherit pkgconfig

do_configure[cleandirs] = "${B}"

# boot rom wants spl + u-boot glued together
UBOOT_BINARY = "u-boot-sunxi-with-spl.bin"

require recipes-bsp/u-boot/u-boot.inc

# radxa's closed ram training + bl31 blobs, must stay after the require since u-boot.inc resets EXTRA_OEMAKE
EXTRA_OEMAKE += "SUNXI_LIBDRAM=${WORKDIR}/ddr.bin BL31=${WORKDIR}/bl31.bin"

# without scp.bin bl31 hangs forever at "wait arisc ready"
EXTRA_OEMAKE += "SCP=${DEPLOY_DIR_IMAGE}/scp.bin"
do_compile[depends] += "scp-firmware:do_deploy"
