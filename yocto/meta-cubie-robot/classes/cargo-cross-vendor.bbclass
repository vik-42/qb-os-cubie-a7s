# cargo cross setup for recipes that run cargo themselves (eg from cmake), pass ${CARGO_CROSS_TARGET} as the triple
# crates come from crate:// entries in SRC_URI (bitbake unpacks them to ${CARGO_HOME}/bitbake) and cargo runs offline
#
# CARGO_GIT_PATCHES: "<git url>=<checkout dir> ...", crates the lock file takes from that git repo are
#   built from the checkout bitbake fetched instead (one [patch] entry per crate)
# CARGO_LOCK_FILES: the Cargo.lock files that list those crates

inherit rust-common

DEPENDS:append = " libstd-rs"

# yocto style triple (aarch64-<vendor>-linux-gnu), its what libstd-rs is built for
CARGO_CROSS_TARGET = "${RUST_HOST_SYS}"

export CARGO_HOME = "${WORKDIR}/cargo_home"
CARGO_CROSS_CONFIG = "${CARGO_HOME}/config.toml"
CARGO_GIT_PATCHES ??= ""
CARGO_LOCK_FILES ??= ""

# linker wrappers come from do_rust_create_wrappers in rust-common
python do_cargo_cross_configure () {
    import os, shutil, tomllib
    home = d.getVar("CARGO_HOME")
    os.makedirs(os.path.join(home, "bitbake"), exist_ok=True)

    build_sys = d.getVar("RUST_BUILD_SYS")
    host_sys = d.getVar("RUST_HOST_SYS")

    lines = [
        '[source.bitbake]',
        'directory = "%s"' % os.path.join(home, "bitbake"),
        '',
        '[source.crates-io]',
        'replace-with = "bitbake"',
        'local-registry = "/nonexistent"',
        '',
        '[net]',
        'offline = true',
        '',
        '[target.%s]' % host_sys,
        'linker = "%s"' % d.getVar("RUST_TARGET_CCLD"),
    ]
    if build_sys != host_sys:
        lines += [
            '',
            '[target.%s]' % build_sys,
            'linker = "%s"' % d.getVar("RUST_BUILD_CCLD"),
        ]

    # keep the original lock files: they say which crates come from which git repo.
    # the copies cargo reads lose their git "source" lines (offline cargo can't check out git),
    # same trick as oe-core's cargo_common
    from_git = {}
    for lock in (d.getVar("CARGO_LOCK_FILES") or "").split():
        orig = lock + ".orig"
        if not os.path.exists(orig):
            shutil.copy(lock, orig)
        for pkg in tomllib.load(open(orig, "rb")).get("package", []):
            src = pkg.get("source", "")
            if src.startswith("git+"):
                from_git.setdefault(src[4:].split("?")[0].split("#")[0], set()).add(pkg["name"])
        text = open(orig).read()
        open(lock, "w").write("".join(l for l in text.splitlines(True) if not l.startswith('source = "git+')))

    # point each of those crates at its folder in the checkout bitbake fetched
    for entry in (d.getVar("CARGO_GIT_PATCHES") or "").split():
        url, checkout = entry.split("=", 1)
        wanted = from_git.get(url, set())
        found = {}
        for root, dirs, files in os.walk(checkout):
            dirs[:] = [x for x in dirs if x not in (".git", "target")]
            if "Cargo.toml" in files:
                name = tomllib.load(open(os.path.join(root, "Cargo.toml"), "rb")).get("package", {}).get("name")
                if name in wanted:
                    found[name] = root
        missing = wanted - set(found)
        if missing:
            bb.fatal("%s has no folder for %s" % (checkout, " ".join(sorted(missing))))
        lines += ['', '[patch."%s"]' % url]
        lines += ['%s = { path = "%s" }' % (n, found[n]) for n in sorted(found)]

    with open(d.getVar("CARGO_CROSS_CONFIG"), "w") as f:
        f.write("\n".join(lines) + "\n")
}
addtask cargo_cross_configure before do_compile after do_rust_create_wrappers do_patch

# rust-common sets RUSTFLAGS but doesnt export it
RUSTFLAGS[export] = "1"

# C code inside crates (eg ring) is compiled from cargo_home, keep that build path out of the binaries
# (rust code is already remapped by rust-common, same as oe-core's python3-maturin)
CFLAGS:append = " -fdebug-prefix-map=${CARGO_HOME}=${TARGET_DBGSRC_DIR}/cargo_home -fmacro-prefix-map=${CARGO_HOME}=${TARGET_DBGSRC_DIR}/cargo_home"

# split host/target CC for cc-rs, otherwise build scripts like ring cross compile their host tools
do_compile:prepend () {
	export CC="${RUST_TARGET_CC}"
	export CXX="${RUST_TARGET_CXX}"
	export AR="${AR}"
	export TARGET_CC="${RUST_TARGET_CC}"
	export TARGET_CXX="${RUST_TARGET_CXX}"
	export TARGET_CFLAGS="${CFLAGS}"
	export TARGET_CXXFLAGS="${CXXFLAGS}"
	export TARGET_AR="${AR}"
	export HOST_CC="${RUST_BUILD_CC}"
	export HOST_CXX="${RUST_BUILD_CXX}"
	export HOST_CFLAGS="${BUILD_CFLAGS}"
	export HOST_CXXFLAGS="${BUILD_CXXFLAGS}"
	export HOST_AR="${BUILD_AR}"
}
