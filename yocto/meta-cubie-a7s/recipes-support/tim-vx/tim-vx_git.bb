SUMMARY = "VeriSilicon TIM-VX, tensor interface over OpenVX for the A733 NPU"
DESCRIPTION = "Graph/op API on top of the Vivante OpenVX driver. onnxruntime's VSINPU \
execution provider and the tflite vx-delegate both build against it."
HOMEPAGE = "https://github.com/VeriSilicon/TIM-VX"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://LICENSE;md5=a223f028c6a73fd2bd9223b79a38dcf6"

SRC_URI = "git://github.com/VeriSilicon/TIM-VX.git;protocol=https;branch=main"
# tested on the board with lenet, 2026-11-09
SRCREV = "702715d714277376d7ff466b7b72833ff57564c1"
PV = "1.2.22+git"
S = "${WORKDIR}/git"

DEPENDS = "viv-unified-driver"

COMPATIBLE_MACHINE = "cubie-a7s"
PACKAGE_ARCH = "${MACHINE_ARCH}"

inherit cmake

# local_sdk.cmake wants <sdk>/include and <sdk>/lib, the sysroot /usr fits that
EXTRA_OECMAKE = " \
    -DEXTERNAL_VIV_SDK=${STAGING_DIR_HOST}${prefix} \
    -DTIM_VX_ENABLE_TEST=OFF \
    -DTIM_VX_BUILD_EXAMPLES=ON \
"

do_install:append() {
    # lenet is a handy smoke test on the board
    install -d ${D}${bindir}
    install -m 0755 ${B}/samples/lenet/lenet ${D}${bindir}/tim-vx-lenet
}

PACKAGES =+ "${PN}-samples"
FILES:${PN}-samples = "${bindir}/tim-vx-lenet"

# libtim-vx.so has no soname version, ship it in the main package
FILES_SOLIBSDEV = ""
FILES:${PN} += "${libdir}/libtim-vx.so"
INSANE_SKIP:${PN} += "dev-so"
