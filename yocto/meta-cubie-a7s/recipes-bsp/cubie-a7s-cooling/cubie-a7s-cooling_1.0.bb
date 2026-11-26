SUMMARY = "Cooling profile for the Cubie A7S"
DESCRIPTION = "Applies the CUBIE_COOLING profile from site.conf: with a fan the DRAM \
runs at its top speed, without one it follows the load to keep the idle die cool. \
The thermal trips themselves live in the kernel and cover both."
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

COMPATIBLE_MACHINE = "cubie-a7s"
PACKAGE_ARCH = "${MACHINE_ARCH}"
INHIBIT_DEFAULT_DEPS = "1"

python () {
    cooling = d.getVar("CUBIE_COOLING")
    if cooling not in ("passive", "fan"):
        bb.fatal("CUBIE_COOLING must be passive or fan, got '%s'" % cooling)
}

# with a fan the heat is handled, without one dram pinned at its top speed kept a bare board's idle die near 80C
DRAM_GOVERNOR = "${@'performance' if d.getVar('CUBIE_COOLING') == 'fan' else 'sunxi_actmon'}"

do_configure[noexec] = "1"
do_compile[noexec] = "1"

do_install() {
    install -d ${D}${sysconfdir}/udev/rules.d
    cat > ${D}${sysconfdir}/udev/rules.d/90-cubie-a7s-cooling.rules <<RULES
# CUBIE_COOLING=${CUBIE_COOLING}
SUBSYSTEM=="devfreq", KERNEL=="a020000.dmcfreq", ATTR{governor}="${DRAM_GOVERNOR}"
RULES
}
do_install[vardeps] += "CUBIE_COOLING DRAM_GOVERNOR"
