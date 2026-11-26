SUMMARY = "Make Zenoh the default ROS 2 middleware on this robot"
DESCRIPTION = "Sets RMW_IMPLEMENTATION=rmw_zenoh_cpp for every login shell, \
so ROS 2 nodes use Zenoh instead of ROS 2 Jazzy's built-in default (Fast DDS)."
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

SRC_URI = "file://rmw-zenoh.sh"
S = "${WORKDIR}"

do_install() {
    install -d ${D}${sysconfdir}/profile.d
    install -m 0644 ${WORKDIR}/rmw-zenoh.sh ${D}${sysconfdir}/profile.d/rmw-zenoh.sh
}

FILES:${PN} = "${sysconfdir}/profile.d/rmw-zenoh.sh"
RDEPENDS:${PN} = "rmw-zenoh-cpp"
