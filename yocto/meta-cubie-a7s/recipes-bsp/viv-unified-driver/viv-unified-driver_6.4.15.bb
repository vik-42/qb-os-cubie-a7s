SUMMARY = "Vivante unified driver userspace (OpenVX 1.3) for the A733 NPU"
DESCRIPTION = "Prebuilt OpenVX libs from Radxa's ai-sdk (unified-tina, gcc 13.2 build), \
talks to galcore in linux-radxa. TIM-VX and onnxruntime sit on top of this."
HOMEPAGE = "https://github.com/ZIFENG278/ai-sdk"
# prebuilt vendor blobs, no license file in the repo
LICENSE = "CLOSED"

SRC_URI = "git://github.com/ZIFENG278/ai-sdk.git;protocol=https;branch=main \
    file://99-galcore.rules \
"
SRCREV = "fc90006d0f6569da2f6726c2d8395877686f5aca"
S = "${WORKDIR}/git/unified-tina"

COMPATIBLE_MACHINE = "cubie-a7s"
PACKAGE_ARCH = "${MACHINE_ARCH}"

# ovxlib left out, tim-vx builds its own copy
VIV_LIBS = "libArchModelSw libCLC libGAL libGLSLC libNNArchPerf libNNGPUBinary libNNVXCBinary \
    libOpenVXU libOvx12VXCBinary libOvxGPUVXCBinary libVSC"

do_configure[noexec] = "1"
do_compile[noexec] = "1"

do_install() {
    install -d ${D}${libdir} ${D}${includedir}/VX ${D}${sysconfdir}/udev/rules.d
    for l in ${VIV_LIBS}; do
        install -m 0755 ${S}/lib/glibc-gcc13_2_0/$l.so ${D}${libdir}/
    done
    # repo has three copies of the same file, keep one plus symlinks
    install -m 0755 ${S}/lib/glibc-gcc13_2_0/libOpenVX.so.1.3.0 ${D}${libdir}/
    ln -sf libOpenVX.so.1.3.0 ${D}${libdir}/libOpenVX.so.1
    ln -sf libOpenVX.so.1 ${D}${libdir}/libOpenVX.so
    install -m 0644 ${S}/inc/VX/*.h ${D}${includedir}/VX/
    install -m 0644 ${WORKDIR}/99-galcore.rules ${D}${sysconfdir}/udev/rules.d/
}

# unversioned sonames, so the plain .so files are the runtime libs
FILES_SOLIBSDEV = "${libdir}/libOpenVX.so"
FILES:${PN} += "${libdir}/*.so"
INSANE_SKIP:${PN} += "already-stripped ldflags dev-so"
