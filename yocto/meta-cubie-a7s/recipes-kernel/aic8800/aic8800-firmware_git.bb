SUMMARY = "AIC8800 USB wifi/bt firmware for the Cubie A7S onboard wireless"
DESCRIPTION = "Firmware for the aic8800 modules built in linux-radxa, installed where Radxa's aic8800-firmware deb puts it."
HOMEPAGE = "https://github.com/radxa-pkg/aic8800"
LICENSE = "GPL-2.0-only"
LIC_FILES_CHKSUM = "file://debian/copyright;md5=ccd22839cbff32b2bbe9a6ec05d11739"

# keep in sync with SRCREV_aic8800 in linux-radxa
SRC_URI = "git://github.com/radxa-pkg/aic8800.git;protocol=https;branch=main"
SRCREV = "e31c0316aca3252345615cf6a52401e192448a90"

PV = "1.0+git"
S = "${WORKDIR}/git"

inherit allarch

do_configure[noexec] = "1"
do_compile[noexec] = "1"

do_install() {
    install -d ${D}${nonarch_base_libdir}/firmware/aic8800_fw/USB
    cp -r --no-preserve=ownership ${S}/src/USB/driver_fw/fw/* ${D}${nonarch_base_libdir}/firmware/aic8800_fw/USB/
}

FILES:${PN} = "${nonarch_base_libdir}/firmware/aic8800_fw"
INSANE_SKIP:${PN} = "arch already-stripped"
