SUMMARY = "Cubie A7S device tree with the header functions picked in CUBIE_HEADER"
DESCRIPTION = "Merges Radxa's A733 overlays (pwm, i2c, uart, spi, i2s on the header) into the \
board's device tree at build time. CUBIE_HEADER in site.conf lists them by name, e.g. \
\"pwm1-1 twi2 uart2\"; the build fails on unknown names, overlays not made for the A7S, or two \
overlays claiming the same pin."
HOMEPAGE = "https://github.com/radxa-pkg/radxa-overlays"
LICENSE = "GPL-2.0-only"
LIC_FILES_CHKSUM = "file://LICENSE;md5=b234ee4d69f5fce4486a80fdaf4a4263"

SRC_URI = "git://github.com/radxa-pkg/radxa-overlays.git;protocol=https;branch=main"
SRCREV = "8631c61dc312de768239db73a686383895ebb173"
S = "${WORKDIR}/git"

COMPATIBLE_MACHINE = "cubie-a7s"
PACKAGE_ARCH = "${MACHINE_ARCH}"

inherit deploy nopackages

DEPENDS = "dtc-native"
# the base dtb comes from the kernel's deploy, overlay #includes from its source tree
do_compile[depends] += "virtual/kernel:do_deploy virtual/kernel:do_shared_workdir"
do_compile[vardeps] += "CUBIE_HEADER"

CUBIE_HEADER ??= ""
HEADER_BASE_DTB = "sun60i-a733-cubie-a7s.dtb"
HEADER_OVERLAY_DIR = "${S}/arch/arm64/boot/dts/allwinner/overlays"

do_configure[noexec] = "1"

python do_compile () {
    import os, re, subprocess

    odir = d.getVar("HEADER_OVERLAY_DIR")
    out = d.getVar("B")
    names = (d.getVar("CUBIE_HEADER") or "").split()
    available = sorted(f[len("sun60iw2p1-"):-len(".dtso")] for f in os.listdir(odir)
                       if f.startswith("sun60iw2p1-") and f.endswith(".dtso"))

    def prop(text, name):
        m = re.search(r'\b%s\s*=\s*([^;]*);' % name, text)
        return re.findall(r'"([^"]*)"', m.group(1)) if m else []

    claimed = {}
    dtbos = []
    for name in names:
        src = os.path.join(odir, "sun60iw2p1-%s.dtso" % name)
        if not os.path.exists(src):
            bb.fatal("CUBIE_HEADER: no overlay '%s', available: %s" % (name, " ".join(available)))
        text = open(src).read()
        if "radxa,cubie-a7s" not in prop(text, "compatible"):
            bb.fatal("CUBIE_HEADER: '%s' isn't made for the Cubie A7S" % name)
        for pin in prop(text, "exclusive"):
            if pin in claimed:
                bb.fatal("CUBIE_HEADER: '%s' and '%s' both need %s" % (claimed[pin], name, pin))
            claimed[pin] = name
        # radxa's metadata node is for their rsetup tool, keep it out of the board's dt
        text = re.sub(r'/\s*\{\s*metadata\s*\{.*?\};\s*\};', '', text, count=1, flags=re.S)
        pre = os.path.join(out, name + ".pre.dts")
        open(pre, "w").write(text)
        ks = d.getVar("STAGING_KERNEL_DIR")
        cpp = subprocess.run(["cpp", "-nostdinc", "-undef", "-D__DTS__", "-x", "assembler-with-cpp",
                              "-I", os.path.join(ks, "include"),
                              "-I", os.path.join(ks, "scripts/dtc/include-prefixes"), pre],
                             capture_output=True, text=True)
        if cpp.returncode:
            bb.fatal("preprocessing %s failed:\n%s" % (name, cpp.stderr))
        dtbo = os.path.join(out, name + ".dtbo")
        subprocess.run(["dtc", "-@", "-q", "-I", "dts", "-O", "dtb", "-o", dtbo, "-"],
                       input=cpp.stdout, text=True, check=True)
        dtbos.append(dtbo)

    base = os.path.join(d.getVar("DEPLOY_DIR_IMAGE"), d.getVar("HEADER_BASE_DTB"))
    merged = os.path.join(out, "cubie-a7s-header.dtb")
    if dtbos:
        subprocess.run(["fdtoverlay", "-i", base, "-o", merged] + dtbos, check=True)
    else:
        subprocess.run(["cp", base, merged], check=True)
    bb.note("CUBIE_HEADER: %s" % (" ".join(names) or "nothing, plain board dt"))
}

do_deploy () {
    install -m 0644 ${B}/cubie-a7s-header.dtb ${DEPLOYDIR}/cubie-a7s-header.dtb
}
addtask deploy after do_compile before do_build
