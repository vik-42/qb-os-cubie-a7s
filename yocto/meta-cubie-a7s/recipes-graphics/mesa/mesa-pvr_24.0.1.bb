# mesa with imagination's powervr frontend, it loads the closed gles/vulkan libs from img-bxm-umlibs
# recipe and patches from meta-ti (MIT), the revision ti shipped with ddk 24.2
require recipes-graphics/mesa/mesa.inc

SUMMARY += " (with PowerVR for the A733 BXM)"

LIC_FILES_CHKSUM = "file://docs/license.rst;md5=63779ec98d78d823a9dc533a0735ef10"

BRANCH = "powervr/${PV}"

SRC_URI = " \
    git://gitlab.freedesktop.org/StaticRocket/mesa.git;protocol=https;branch=${BRANCH} \
    file://0001-meson.build-check-for-all-linux-host_os-combinations.patch \
    file://0001-meson-misdetects-64bit-atomics-on-mips-clang.patch \
    file://0001-gallium-Fix-build-with-llvm-17.patch \
    file://0001-drisw-fix-build-without-dri3.patch \
    file://0002-glxext-don-t-try-zink-if-not-enabled-in-mesa.patch \
    file://0001-gallivm-Call-StringMapIterator-from-llvm-scope.patch \
    file://0001-Update-lp_bld_misc.cpp-to-support-llvm-19.patch \
    file://0001-pvr_alias-allow-a-dash-in-the-display-driver-name.patch \
"

S = "${WORKDIR}/git"

SRCREV = "68af6a102c2298569e77d1aa8bccc1ff61438b3e"
PV = "24.0.1"

COMPATIBLE_MACHINE = "cubie-a7s"
PACKAGE_ARCH = "${MACHINE_ARCH}"

# pvr gallium frontend for gles, pvr vulkan builds the wsi shim libVK_IMG uses
GALLIUMDRIVERS:append = ",pvr"
VULKAN_DRIVERS:append = ",pvr"
# mesa picks the dri driver by the kms device's name, the a733 display driver is sunxi-drm
EXTRA_OEMESON += "-Dgallium-pvr-alias=sunxi-drm"

do_install:append () {
    # remove pvr custom pkgconfig
    rm -rf ${D}${datadir}/pkgconfig
}

FILES:${PN}-dev += "${datadir}/mesa/wayland-drm.xml"
FILES:mesa-vulkan-drivers += "${libdir}/libpvr_mesa_wsi.so"

RRECOMMENDS:mesa-megadriver:append:class-target = " img-bxm-umlibs"
