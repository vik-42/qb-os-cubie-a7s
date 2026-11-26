SUMMARY = "SCP firmware (scp.bin) for the A733 ARISC power management core"
DESCRIPTION = "Builds the firmware for the small RISC-V core that BL31 starts and waits on during boot. \
Same sources and make command as Radxa's own u-boot-dlan17 build."
HOMEPAGE = "https://github.com/radxa/allwinner-arisc"
# no license file anywhere in radxa's repos, dramlib part is a prebuilt .a
LICENSE = "CLOSED"

SRC_URI = "git://github.com/radxa/allwinner-arisc.git;protocol=https;branch=main;name=arisc \
    git://github.com/radxa/allwinner-dramlib.git;protocol=https;branch=main;name=dramlib;destsuffix=dramlib \
    https://github.com/radxa/allwinner-toolchain/releases/download/aiot-linux-v1.4.6/riscv64-elf-x86_64-20201104.tar.gz;name=toolchain;subdir=git/ar100s/tools/riscv64-elf-x86_64-20201104 \
"
# same commits radxa-pkg/u-boot-dlan17 pins as submodules
SRCREV_arisc = "79086caef1f6d1fb1cf164afb018f6e873fd9910"
SRCREV_dramlib = "b1c31a940733da15a127be74dc9cb90bdd01a516"
SRCREV_FORMAT = "arisc_dramlib"
SRC_URI[toolchain.sha256sum] = "c0b9197b25e9778afffbcda649f3e1d4cc3555fe0bed70e9aaf5730d90a70530"

PV = "1.0+git"
S = "${WORKDIR}/git"

inherit deploy nopackages

COMPATIBLE_MACHINE = "cubie-a7s"
PACKAGE_ARCH = "${MACHINE_ARCH}"
SCP_PLATFORM = "sun60iw2p1"

python () {
    # toolchain and kconfig tools in the repo are prebuilt x86_64 binaries
    if d.getVar('BUILD_ARCH') != 'x86_64':
        raise bb.parse.SkipRecipe("scp-firmware needs an x86_64 build host")
}

do_configure[noexec] = "1"

do_compile() {
    # makefile sets its own riscv CC but appends to CFLAGS, so drop the aarch64 ones
    unset CC CXX CPP LD AR AS NM OBJCOPY OBJDUMP STRIP RANLIB CFLAGS CXXFLAGS CPPFLAGS LDFLAGS
    touch ${S}/ar100s/tools/.toolchain.flag
    oe_runmake -C ${S} \
        LICHEE_ARISC_DEFDIR=ar100s \
        LICHEE_ARISC_DEFCONFIG=${SCP_PLATFORM}_defconfig \
        LICHEE_DRAMLIB_PATH=${WORKDIR}/dramlib \
        CFG_CHIP_PLATFORM=${SCP_PLATFORM}
}

do_deploy() {
    install -m 0644 ${S}/ar100s/scp.bin ${DEPLOYDIR}/scp.bin
}
addtask deploy after do_compile before do_build
