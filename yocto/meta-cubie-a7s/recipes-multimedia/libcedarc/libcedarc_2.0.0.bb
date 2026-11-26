SUMMARY = "Allwinner CedarC video codec libraries for the A733 video engines"
DESCRIPTION = "Prebuilt hardware decode/encode libraries (H.264, HEVC, VP8/9, MPEG, MJPEG...) \
plus the OpenMAX IL core and components, from Radxa's Tina SDK debian packages. \
Talks to the sunxi-ve kernel driver (/dev/cedar_dev, /dev/cedar_dev_ve2)."
HOMEPAGE = "https://github.com/radxa/allwinner-debian"
# prebuilt vendor blobs, no license file in the package
LICENSE = "CLOSED"

# radxa's rootfs script: libcedarc-dev_2.0.0 is the one for a733/t736
SRC_URI = "https://raw.githubusercontent.com/radxa/allwinner-debian/43fe44a304e8c06c6275769c4731ff82265325a7/packages/arm64/libcedarc/libcedarc-dev_2.0.0_arm64.deb;subdir=${BP} \
    file://99-cedar.rules \
"
SRC_URI[sha256sum] = "ccd5da2403837f1c5a566239124eaa66c56b6bbcb485308dd7ad7ec6d8d0d3a4"

PROVIDES += "virtual/libomxil"

COMPATIBLE_MACHINE = "cubie-a7s"
PACKAGE_ARCH = "${MACHINE_ARCH}"

do_configure[noexec] = "1"
do_compile[noexec] = "1"

do_install() {
    install -d ${D}${libdir} ${D}${includedir} ${D}${sysconfdir}/udev/rules.d ${D}${bindir}
    install -m 0755 ${S}/usr/lib/aarch64-linux-gnu/*.so ${D}${libdir}/
    install -m 0644 ${S}/usr/include/*.h ${D}${includedir}/
    install -m 0644 ${S}/etc/cedarc.conf ${D}${sysconfdir}/
    # errors only, the shipped debug level prints hundreds of lines per stream
    sed -i 's/^cdc_log_level *=.*/cdc_log_level = 6/' ${D}${sysconfdir}/cedarc.conf
    install -m 0644 ${WORKDIR}/99-cedar.rules ${D}${sysconfdir}/udev/rules.d/
    install -m 0755 ${S}/usr/bin/vdecoderdemo ${S}/usr/bin/vencoderdemo ${D}${bindir}/
}

PACKAGES =+ "${PN}-demos"
FILES:${PN}-demos = "${bindir}/vdecoderdemo ${bindir}/vencoderdemo"

# unversioned sonames, the plain .so files are the runtime libs
FILES_SOLIBSDEV = ""
FILES:${PN} += "${libdir}/*.so"
INSANE_SKIP:${PN} += "already-stripped ldflags dev-so"
INSANE_SKIP:${PN}-demos += "already-stripped ldflags"
