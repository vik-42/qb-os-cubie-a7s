FILESEXTRAPATHS:prepend := "${THISDIR}/${PN}:"

# zenoh-c, zenoh-cpp, the zenoh crates zenoh-c pulls from git and its crates.io deps are all fetched by bitbake,
# the vendor package builds from these copies instead of downloading them during compile.
# commits are the ones upstream's CMakeLists (VCS_VERSION) and zenoh-c's Cargo.lock pin
SRC_URI += " \
    git://github.com/eclipse-zenoh/zenoh-c.git;protocol=https;nobranch=1;name=zenoh-c;destsuffix=zenoh-c \
    git://github.com/eclipse-zenoh/zenoh-cpp.git;protocol=https;nobranch=1;name=zenoh-cpp;destsuffix=zenoh-cpp \
    git://github.com/eclipse-zenoh/zenoh.git;protocol=https;branch=release/1.6.2;name=zenoh;destsuffix=zenoh \
    file://vendor-from-fetched-sources.patch \
    file://zenoh-c-panic-unwind.patch;patchdir=${WORKDIR}/zenoh-c \
"
SRCREV_zenoh-c = "f376456ccf75ed837a21a186bdf5191cba50eb3b"
SRCREV_zenoh-cpp = "0cd54f291039a65b96921a5951a66aeef088e67c"
SRCREV_zenoh = "b81e253b32f98fa35bae34e44e647630f50f0321"
SRCREV_FORMAT = "default_zenoh-c_zenoh-cpp_zenoh"

require zenoh-cpp-vendor-crates.inc

EXTRA_OECMAKE += "-DZENOH_C_SOURCE_DIR=${WORKDIR}/zenoh-c -DZENOH_CPP_SOURCE_DIR=${WORKDIR}/zenoh-cpp"

# cargo and clang run on the build host
DEPENDS:remove = "cargo clang"
DEPENDS += "cargo-native clang-native"

# zenoh-c runs cargo itself, tell it to cross compile and where the fetched crates are
inherit cargo-cross-vendor
EXTRA_OECMAKE += "-DZENOHC_CUSTOM_TARGET=${CARGO_CROSS_TARGET}"
CARGO_GIT_PATCHES = "https://github.com/eclipse-zenoh/zenoh.git=${WORKDIR}/zenoh"
CARGO_LOCK_FILES = "${WORKDIR}/zenoh-c/Cargo.lock"
