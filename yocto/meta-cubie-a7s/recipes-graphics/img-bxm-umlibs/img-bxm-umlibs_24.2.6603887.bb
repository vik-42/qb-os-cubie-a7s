SUMMARY = "Userspace driver and firmware for the A733's Imagination BXM GPU"
DESCRIPTION = "Closed PowerVR libraries (GLES, Vulkan, OpenCL, shader compilers) and GPU firmware, \
DDK ${PV}, the same build as the img-bxm kernel driver in the Allwinner BSP. GLES goes through \
mesa-pvr, Vulkan and OpenCL through their standard loaders."
HOMEPAGE = "https://github.com/radxa/allwinner-debian"
# prebuilt imagination blobs, no license file in the package
LICENSE = "CLOSED"

# radxa's xorg package for the a733 carries the ddk userspace, only the closed libs and firmware are taken
SRC_URI = "https://raw.githubusercontent.com/radxa/allwinner-debian/43fe44a304e8c06c6275769c4731ff82265325a7/packages/arm64/xserver/xserver-xorg-img-bxm_1.21.1-2_arm64.deb;subdir=${BP} \
    file://img_icd.json \
    file://IMG.icd \
"
SRC_URI[sha256sum] = "c8e606db1abdea40a5b97f7905ea86901b50c5fe1b76a55356ab70dde3304c3a"

# the blobs link these, here so their runtime packages get picked up
DEPENDS = "libdrm libx11 libxcb"

COMPATIBLE_MACHINE = "cubie-a7s"
PACKAGE_ARCH = "${MACHINE_ARCH}"

do_configure[noexec] = "1"
do_compile[noexec] = "1"

# BXM-4-64 MC1, the bvnc the kernel driver is built for
BVNC = "36.56.104.183"
UM_LIBS = "srv_um usc glslcompiler ufwriter pvr_dri_support GLESv1_CM_PVR_MESA GLESv2_PVR_MESA VK_IMG PVROCL"

do_install() {
    install -d ${D}${libdir} ${D}${nonarch_base_libdir}/firmware
    install -m 0644 ${S}/lib/firmware/rgx.fw.${BVNC} ${S}/lib/firmware/rgx.sh.${BVNC} ${D}${nonarch_base_libdir}/firmware/
    for l in ${UM_LIBS}; do
        install -m 0755 ${S}/usr/lib/lib$l.so.${PV} ${D}${libdir}/
        # their sonames are mostly the bare .so, keep every link the package has
        for link in ${S}/usr/lib/lib$l.so ${S}/usr/lib/lib$l.so.1; do
            [ -L $link ] && ln -sf lib$l.so.${PV} ${D}${libdir}/$(basename $link)
        done
    done

    install -d ${D}${datadir}/vulkan/icd.d ${D}${sysconfdir}/OpenCL/vendors
    install -m 0644 ${WORKDIR}/img_icd.json ${D}${datadir}/vulkan/icd.d/
    install -m 0644 ${WORKDIR}/IMG.icd ${D}${sysconfdir}/OpenCL/vendors/
}

PACKAGES =+ "${PN}-firmware ${PN}-vulkan ${PN}-opencl"

# keep these names, the debian renamer would turn them into libvk-img1 / libpvrocl1
DEBIAN_NOAUTONAME:${PN}-vulkan = "1"
DEBIAN_NOAUTONAME:${PN}-opencl = "1"

FILES:${PN}-firmware = "${nonarch_base_libdir}/firmware"
FILES:${PN}-vulkan = "${libdir}/libVK_IMG.so* ${datadir}/vulkan"
FILES:${PN}-opencl = "${libdir}/libPVROCL.so* ${sysconfdir}/OpenCL"
# everything else, the bare .so links are what the sonames point at
FILES:${PN} = "${libdir}/*.so*"
FILES_SOLIBSDEV = ""

RDEPENDS:${PN} = "${PN}-firmware"
RDEPENDS:${PN}-vulkan = "${PN} vulkan-loader"
RDEPENDS:${PN}-opencl = "${PN} opencl-icd-loader"

INSANE_SKIP:${PN} += "already-stripped ldflags dev-so"
INSANE_SKIP:${PN}-vulkan += "already-stripped ldflags dev-so"
INSANE_SKIP:${PN}-opencl += "already-stripped ldflags dev-so"
INSANE_SKIP:${PN}-firmware += "arch"
