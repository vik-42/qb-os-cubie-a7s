FILESEXTRAPATHS:prepend := "${THISDIR}/${PN}:"
# a733: cedarc's omx core on the bundled khronos headers plus cedarc's extensions, yv12, decoded frames exported as dmabuf
SRC_URI += "file://omx-headers-cedarc-extensions.patch file://omx-cedarc-yv12-nv21-output.patch \
    file://omxvideodec-codecconfig-only-for-header-subframes.patch file://omx-cedarc-dmabuf-output.patch \
    file://omxvideoenc-propose-frame-size.patch file://omx-reuse-unstarted-component.patch \
    file://gstomx-a733.conf"
GSTREAMER_1_0_OMX_TARGET = "generic"
GSTREAMER_1_0_OMX_CORE_NAME = "${libdir}/libOmxCore.so"
PACKAGE_ARCH = "${MACHINE_ARCH}"
do_install:append() {
    install -d ${D}${sysconfdir}/xdg
    install -m 0644 ${WORKDIR}/gstomx-a733.conf ${D}${sysconfdir}/xdg/gstomx.conf
}
