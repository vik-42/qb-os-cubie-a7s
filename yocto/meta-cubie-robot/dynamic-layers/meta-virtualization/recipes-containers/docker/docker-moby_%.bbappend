FILESEXTRAPATHS:prepend := "${THISDIR}/${PN}:"

# installed but off at boot, only stopped cleanly at shutdown if someone started it.
# start it by hand with /etc/init.d/docker.init start, or at every boot with update-rc.d docker.init defaults
INITSCRIPT_PARAMS:${PN} = "stop 20 0 1 6 ."

# plain daemon options, the init script's own defaults point at a registry mirror on localhost:5000 that doesn't exist
SRC_URI += "file://dockerd.sysconfig"
do_install:append () {
    install -d ${D}${sysconfdir}/sysconfig
    install -m 0644 ${WORKDIR}/dockerd.sysconfig ${D}${sysconfdir}/sysconfig/dockerd
}
FILES:${PN} += "${sysconfdir}/sysconfig/dockerd"
CONFFILES:${PN} += "${sysconfdir}/sysconfig/dockerd"

# kernel modules docker loads when it starts (overlay storage, bridge network, NAT), they are modules on this kernel
RRECOMMENDS:${PN} += "kernel-module-overlay kernel-module-bridge kernel-module-br-netfilter kernel-module-veth \
    kernel-module-iptable-filter kernel-module-iptable-nat kernel-module-xt-conntrack kernel-module-xt-nat"
