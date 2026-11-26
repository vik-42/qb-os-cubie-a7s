SUMMARY = "Linux kernel for the Radxa Cubie A7S (Allwinner A733)"
DESCRIPTION = "Radxa builds this kernel from three separate pieces glued together at \
build time: a generic kernel source shared by several Allwinner chips, a shared pack \
of extra Allwinner drivers ('bsp'), and a small folder of files specific to this one \
board ('device-a733', this is where the real device tree source for the Cubie A7S \
actually lives). We fetch all three and glue them the same way Radxa's own (private, \
not publicly available) build script does."

LICENSE = "GPL-2.0-only"
LIC_FILES_CHKSUM = "file://COPYING;md5=6bc538ed5bd9a7fc9398086aedcd7e46"

inherit kernel

# radxa's a733 tree, this board only
COMPATIBLE_MACHINE = "cubie-a7s"

# pinned, AUTOREV moved under us once and broke a patch
SRCREV_kernel = "61f3fe291c7a0ca97a038194bb242b133971b76e"
SRCREV_bsp = "8571b52b946f1fe16558e68170bb9148f0a99326"
SRCREV_device = "9ee07c72616ca5b3b8744246e61744dfe1ecdf02"
SRCREV_aic8800 = "e31c0316aca3252345615cf6a52401e192448a90"
SRCREV_FORMAT = "kernel_bsp_device_aic8800"

SRC_URI = " \
    git://github.com/radxa/kernel.git;protocol=https;branch=allwinner-aiot-linux-6.6;name=kernel;destsuffix=git \
    git://github.com/radxa/allwinner-bsp.git;protocol=https;branch=cubie-aiot-v1.4.8;name=bsp;destsuffix=git/bsp \
    git://github.com/radxa/allwinner-device.git;protocol=https;branch=device-a733-v1.4.8;name=device;destsuffix=git/device-a733 \
    file://known-good-kernel.config \
    file://galcore-report-6.4.15.patch \
    file://galcore-a733-sram.patch \
    file://img-bxm-make-4.4.patch \
    file://img-bxm-dvfs.patch \
    file://a733-thermal.patch \
    file://a733-pmic-quiet-boot.patch \
    file://img-bxm-dvfs-fixes.patch \
    file://img-bxm-yocto-triple.patch \
    file://pwm-sunxi-regulator-balance.patch \
    file://edp-relink-after-hotplug.patch \
    file://aic8800-no-trace-printk.patch;apply=no \
    git://github.com/radxa-pkg/aic8800.git;protocol=https;branch=main;name=aic8800;destsuffix=aic8800 \
"

S = "${WORKDIR}/git"
PV = "6.6.98"

# build in place like radxa does, with a separate B this tree reconfigures forever
B = "${S}"

KERNEL_DEVICETREE = "allwinner/sun60i-a733-cubie-a7s.dtb"

# radxa always sets this, our gcc errors out without it
TOOLCHAIN_OPTIONS:append = " -Wno-enum-int-mismatch"
# BSP_TOP and LICHEE_KERN_DIR are in cubie-a7s.conf
export CROSS_COMPILE = "${TARGET_PREFIX}"

# plain host compiler for the kernel's helper tools, yocto's extra flags made them rebuild forever
HOSTCC = "gcc"
HOSTCFLAGS = "-O2 -pipe"
HOSTLDFLAGS = ""
HOSTCPP = "gcc -E"
HOSTCXX = "g++"
HOSTCXXFLAGS = "-O2 -pipe"

do_configure:prepend() {
    # bsp already lands in ${S}/bsp from the fetch, only the board dts needs linking
    ln -sfn ${WORKDIR}/git/device-a733/configs/cubie_a7s/linux-6.6/board.dts \
        ${S}/arch/arm64/boot/dts/allwinner/sun60i-a733-cubie-a7s.dts
    cp -a ${WORKDIR}/git/bsp/configs/linux-6.6/*.dtsi ${S}/arch/arm64/boot/dts/allwinner/
    mkdir -p ${S}/include/dt-bindings
    cp -aR ${WORKDIR}/git/bsp/include/dt-bindings/. ${S}/include/dt-bindings/
    echo '#define AW_BSP_VERSION "yocto-build"' > ${S}/bsp/include/sunxi-autogen.h

    # config that already boots this board, radxa's own merge step needs private files
    cp ${WORKDIR}/known-good-kernel.config ${B}/.config

    # swap radxa's compiler string for ours or kconfig thinks the compiler changed and loops
    our_compiler_version=$(${CC} --version | head -n 1 | tr -d '#')
    sed -i "s|^CONFIG_CC_VERSION_TEXT=.*|CONFIG_CC_VERSION_TEXT=\"${our_compiler_version}\"|" ${B}/.config

    # bsp drivers include headers from their own dir, add -I$(src) everywhere (keep the leading newline)
    find ${S}/bsp -name Makefile | while read -r makefile; do
        if ! grep -q '\-I\$(src)' "${makefile}"; then
            printf '\nccflags-y += -I$(src)\n' >> "${makefile}"
        fi
    done
}

# onboard wifi (aic8800 usb), built here since out of tree modules loop forever in make-mod-scripts
AIC_DRV_DIR = "${WORKDIR}/aic8800/src/USB/driver_fw/drivers"
KERNEL_MODULE_AUTOLOAD += "aic_load_fw aic8800_fdrv"
# channels and tx power come from the kernel's signed regulatory.db (wireless-regdb) for CUBIE_WIFI_COUNTRY,
# not from the driver's own table, which it calls "permissive custom regulatory rules (for testing ONLY)"
KERNEL_MODULE_PROBECONF += "aic8800_fdrv cfg80211"
module_conf_aic8800_fdrv = "options aic8800_fdrv custregd=0"
module_conf_cfg80211 = "options cfg80211 ieee80211_regdom=${CUBIE_WIFI_COUNTRY}"
python () {
    import re
    c = d.getVar("CUBIE_WIFI_COUNTRY")
    if not re.fullmatch(r"00|[A-Z]{2}", c or ""):
        bb.fatal("CUBIE_WIFI_COUNTRY must be an ISO 3166 country code like DE or US, or 00 for the world rules, not '%s'" % c)
}

# npu, module so it can be reloaded, needs the sram patch to not hang
KERNEL_MODULE_AUTOLOAD += "galcore"

# video engines (cedar decode/encode)
KERNEL_MODULE_AUTOLOAD += "sunxi-ve"

# gpu (img bxm), imagination's own build system wrapped around kbuild, same ddk as img-bxm-umlibs
# no autoload needed, udev loads it from the img,gpu dt node
# its makefiles are patched for make 4.4, use ours so the host make doesn't matter
DEPENDS += "make-native"
BXM_DIR = "${S}/bsp/modules/gpu/img-bxm/linux/rogue_km"
# it only adds CROSS_COMPILE to a default CC (and picks the target arch from it), and word-splits
# KERNEL_CC, so drop yocto's CC/CXX and pass plain tool names. KCFLAGS keeps build paths out of the .ko
BXM_MAKE = "env -u CC -u CXX ${STAGING_BINDIR_NATIVE}/make -C ${BXM_DIR}/build/linux/sunxi_linux BUILD=release \
    KERNELDIR=${B} CROSS_COMPILE=${TARGET_PREFIX} HOST_CC=gcc HOST_CXX=g++ \
    KERNEL_CC=${TARGET_PREFIX}gcc KERNEL_LD=${TARGET_PREFIX}ld.bfd \
    KERNEL_NM=${TARGET_PREFIX}nm KERNEL_OBJCOPY=${TARGET_PREFIX}objcopy \
    KCFLAGS='-fmacro-prefix-map=${STAGING_KERNEL_DIR}/= -fdebug-prefix-map=${STAGING_KERNEL_DIR}/='"

do_patch[postfuncs] += "aic8800_apply_radxa_patches"
aic8800_apply_radxa_patches() {
    cd ${WORKDIR}/aic8800
    for p in $(grep -v '^#' debian/patches/series); do
        # some sources are crlf and the patches arent, patch refuses those so strip the CRs first
        for f in $(sed -n 's|^+++ b/||p' debian/patches/$p); do
            [ -f "$f" ] && sed -i 's/\r$//' "$f"
        done
        patch -p1 --forward < debian/patches/$p
    done
    # ours go on top of radxa's
    patch -p1 --forward < ${WORKDIR}/aic8800-no-trace-printk.patch
}

do_compile_kernelmodules:append() {
    for d in aic8800 aic_btusb; do
        oe_runmake -C ${B} ${PARALLEL_MAKE} M=${AIC_DRV_DIR}/$d modules ${KERNEL_EXTRA_ARGS}
    done
    ${BXM_MAKE} ${PARALLEL_MAKE}
}

do_install:append() {
    for d in aic8800 aic_btusb; do
        oe_runmake -C ${B} M=${AIC_DRV_DIR}/$d DEPMOD=echo INSTALL_MOD_DIR=extra \
            MODLIB=${D}${nonarch_base_libdir}/modules/${KERNEL_VERSION} modules_install
    done
    install -d ${D}${nonarch_base_libdir}/modules/${KERNEL_VERSION}/extra
    install -m 0644 ${BXM_DIR}/binary_sunxi_linux_nulldrmws_release/target_aarch64/kbuild/pvrsrvkm.ko \
        ${D}${nonarch_base_libdir}/modules/${KERNEL_VERSION}/extra/
}
