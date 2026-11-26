SUMMARY = "Grow the root partition and filesystem to fill the SD card on first boot"
DESCRIPTION = "The image only holds what it needs. On the first boot this grows the root \
partition (if it is the last one on the disk) and its ext4 filesystem to the end of the \
card, then removes its own start link."
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

SRC_URI = "file://rootfs-grow"
S = "${WORKDIR}"

inherit update-rc.d allarch

# right after checkroot.sh (S05) has made / writable
INITSCRIPT_NAME = "rootfs-grow"
INITSCRIPT_PARAMS = "start 07 S ."

do_install () {
    install -d ${D}${sysconfdir}/init.d
    install -m 0755 ${WORKDIR}/rootfs-grow ${D}${sysconfdir}/init.d/rootfs-grow
}

# sfdisk and partx from util-linux, resize2fs for ext4
RDEPENDS:${PN} = "util-linux-sfdisk util-linux-partx e2fsprogs-resize2fs"
