SUMMARY = "ONNX Runtime with the VeriSilicon NPU (VSINPU) execution provider"
DESCRIPTION = "Runs .onnx models, ops the A733 NPU supports go through TIM-VX, \
the rest falls back to the cpu. C/C++ lib plus the python module."
HOMEPAGE = "https://onnxruntime.ai"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://LICENSE;md5=0f7e3b1308cb5c00b372a6e78835732d"

# ort pulls its deps with cmake FetchContent, no network in do_compile so we fetch them here
# and lay them out as onnxruntime_CMAKE_DEPS_MIRROR_DIR expects. list + sha1s come from cmake/deps.txt
SRC_URI = " \
    https://github.com/microsoft/onnxruntime/archive/refs/tags/v${PV}.tar.gz;name=ort;downloadfilename=onnxruntime-${PV}.tar.gz \
    file://vsinpu-in-memory-initializers.patch \
    file://vsinpu-report-graph-failures.patch \
    file://vsinpu-a733-quantize-on-cpu.patch \
    https://github.com/abseil/abseil-cpp/archive/refs/tags/20250814.0.zip;name=abseil-cpp;unpack=0;downloadfilename=ortdep-abseil_cpp.zip \
    https://github.com/jarro2783/cxxopts/archive/3c73d91c0b04e2b59462f0a741be8c07024c1bc0.zip;name=cxxopts;unpack=0;downloadfilename=ortdep-cxxopts.zip \
    https://github.com/HowardHinnant/date/archive/refs/tags/v3.0.1.zip;name=date;unpack=0;downloadfilename=ortdep-date.zip \
    https://github.com/dmlc/dlpack/archive/5c210da409e7f1e51ddf445134a4376fdbd70d7d.zip;name=dlpack;unpack=0;downloadfilename=ortdep-dlpack.zip \
    https://github.com/eigen-mirror/eigen/archive/1d8b82b0740839c0de7f1242a3585e3390ff5f33/eigen-1d8b82b0740839c0de7f1242a3585e3390ff5f33.zip;name=eigen;unpack=0;downloadfilename=ortdep-eigen.zip \
    https://github.com/google/flatbuffers/archive/refs/tags/v23.5.26.zip;name=flatbuffers;unpack=0;downloadfilename=ortdep-flatbuffers.zip \
    https://github.com/Maratyszcza/FP16/archive/0a92994d729ff76a58f692d3028ca1b64b145d91.zip;name=fp16;unpack=0;downloadfilename=ortdep-fp16.zip \
    https://github.com/Maratyszcza/FXdiv/archive/63058eff77e11aa15bf531df5dd34395ec3017c8.zip;name=fxdiv;unpack=0;downloadfilename=ortdep-fxdiv.zip \
    https://github.com/nlohmann/json/archive/refs/tags/v3.11.3.zip;name=json;unpack=0;downloadfilename=ortdep-json.zip \
    https://github.com/microsoft/GSL/archive/refs/tags/v4.2.1.zip;name=microsoft-gsl;unpack=0;downloadfilename=ortdep-microsoft_gsl.zip \
    https://github.com/boostorg/mp11/archive/refs/tags/boost-1.82.0.zip;name=mp11;unpack=0;downloadfilename=ortdep-mp11.zip \
    https://github.com/onnx/onnx/archive/refs/tags/v1.22.0.zip;name=onnx;unpack=0;downloadfilename=ortdep-onnx.zip \
    https://github.com/protocolbuffers/protobuf/releases/download/v33.6/protobuf-33.6.tar.gz;name=protobuf;unpack=0;downloadfilename=ortdep-protobuf.tar.gz \
    https://github.com/protocolbuffers/protobuf/releases/download/v33.6/protoc-33.6-linux-x86_64.zip;name=protoc-linux-x64;unpack=0;downloadfilename=ortdep-protoc_linux_x64.zip \
    https://github.com/Maratyszcza/psimd/archive/072586a71b55b7f8c584153d223e95687148a900.zip;name=psimd;unpack=0;downloadfilename=ortdep-psimd.zip \
    https://github.com/google/pthreadpool/archive/dcc9f28589066af0dbd4555579281230abbf74dd.zip;name=pthreadpool;unpack=0;downloadfilename=ortdep-pthreadpool.zip \
    https://github.com/pybind/pybind11/archive/refs/tags/v3.0.2.zip;name=pybind11;unpack=0;downloadfilename=ortdep-pybind11.zip \
    https://github.com/pytorch/cpuinfo/archive/66ee79c038d70dad9f08705b2c9b3e58f6d8f512.zip;name=pytorch-cpuinfo;unpack=0;downloadfilename=ortdep-pytorch_cpuinfo.zip \
    https://github.com/google/re2/archive/refs/tags/2024-07-02.zip;name=re2;unpack=0;downloadfilename=ortdep-re2.zip \
    https://github.com/dcleblanc/SafeInt/archive/refs/tags/3.0.28.zip;name=safeint;unpack=0;downloadfilename=ortdep-safeint.zip \
    https://github.com/ARM-software/kleidiai/archive/refs/tags/v1.30.0.tar.gz;name=kleidiai;unpack=0;downloadfilename=ortdep-kleidiai.tar.gz \
"
SRC_URI[ort.sha256sum] = "92ffbf8b97872fd1e73c9eebdf3319d9a3b24919ae438d27db903b72ee08939e"
SRC_URI[abseil-cpp.sha1sum] = "a9eb1d648cbca4d4d788737e971a6a7a63726b07"
SRC_URI[cxxopts.sha1sum] = "6c6ca7f8480b26c8d00476e0e24b7184717fe4f0"
SRC_URI[date.sha1sum] = "2dac0c81dc54ebdd8f8d073a75c053b04b56e159"
SRC_URI[dlpack.sha1sum] = "e499c86e4e5c5268a87661d7ea39c27fae10907c"
SRC_URI[eigen.sha1sum] = "05b19b49e6fbb91246be711d801160528c135e34"
SRC_URI[flatbuffers.sha1sum] = "59422c3b5e573dd192fead2834d25951f1c1670c"
SRC_URI[fp16.sha1sum] = "b985f6985a05a1c03ff1bb71190f66d8f98a1494"
SRC_URI[fxdiv.sha1sum] = "a5658f4036402dbca7cebee32be57fb8149811e1"
SRC_URI[json.sha1sum] = "5e88795165cc8590138d1f47ce94ee567b85b4d6"
SRC_URI[microsoft-gsl.sha1sum] = "1094e3bb7a8af763dcb136ccd676e6e75e614eec"
SRC_URI[mp11.sha1sum] = "9bc9e01dffb64d9e0773b2e44d2f22c51aace063"
SRC_URI[onnx.sha1sum] = "2b2cd58ac7a26df5371266149e0c76776330cdf1"
SRC_URI[protobuf.sha1sum] = "051fee744fcca04dab37eb009d49aa3e9546acda"
SRC_URI[protoc-linux-x64.sha1sum] = "44c6aeb3f494e4f8e46d283b05e087c47cf6369d"
SRC_URI[psimd.sha1sum] = "1f5454b01f06f9656b77e4a5e2e31d7422487013"
SRC_URI[pthreadpool.sha1sum] = "533a77943203ef15ca608bcd9dbe2c94da7451d2"
SRC_URI[pybind11.sha1sum] = "a064e663b4d7a337ac291d1bef7337ef4e60a1ae"
SRC_URI[pytorch-cpuinfo.sha1sum] = "6e2be17c110e4f4184b059303c722b3c5bd080a6"
SRC_URI[re2.sha1sum] = "646e1728269cde7fcef990bf4a8e87b047882e88"
SRC_URI[safeint.sha1sum] = "23f252040ff6cb9f1fd18575b32fa8fb5928daac"
SRC_URI[kleidiai.sha1sum] = "36aebdd5bd28c40b1943eb93498ae42e703510e5"

S = "${WORKDIR}/onnxruntime-${PV}"
OECMAKE_SOURCEPATH = "${S}/cmake"

DEPENDS = "tim-vx viv-unified-driver python3 python3-numpy python3-numpy-native"
RDEPENDS:${PN}-python = "python3-core python3-numpy python3-ctypes python3-json"

# everything is pinned by checksum, the github archive warning doesnt apply
WARN_QA:remove = "src-uri-bad"

COMPATIBLE_MACHINE = "cubie-a7s"
PACKAGE_ARCH = "${MACHINE_ARCH}"

inherit cmake python3native python3targetconfig

python () {
    # deps include a prebuilt x86_64 protoc
    if d.getVar('BUILD_ARCH') != 'x86_64':
        raise bb.parse.SkipRecipe("onnxruntime needs an x86_64 build host")
}

ORT_MIRROR = "${WORKDIR}/deps-mirror"

# the vsinpu cmake reads these from the env
export TIM_VX_INSTALL = "${STAGING_DIR_HOST}${prefix}"
export VIVANTE_SDK_DIR = "${STAGING_DIR_HOST}${prefix}"

# cmake.bbclass sets FETCHCONTENT_FULLY_DISCONNECTED=ON which skips unpacking, every dep is a local file here so turn it back off
EXTRA_OECMAKE = " \
    -Donnxruntime_CMAKE_DEPS_MIRROR_DIR=${ORT_MIRROR} \
    -DFETCHCONTENT_TRY_FIND_PACKAGE_MODE=NEVER \
    -DFETCHCONTENT_FULLY_DISCONNECTED=OFF \
    -Donnxruntime_USE_VSINPU=ON \
    -Donnxruntime_BUILD_SHARED_LIB=ON \
    -Donnxruntime_ENABLE_PYTHON=ON \
    -Donnxruntime_BUILD_UNIT_TESTS=OFF \
    -Donnxruntime_BUILD_BENCHMARKS=OFF \
    -Donnxruntime_USE_XNNPACK=OFF \
    -DPython_EXECUTABLE=${PYTHON} \
    -DPython_INCLUDE_DIR=${STAGING_INCDIR}/${PYTHON_DIR} \
    -DPython_NumPy_INCLUDE_DIR=${STAGING_DIR_HOST}${PYTHON_SITEPACKAGES_DIR}/numpy/core/include \
    -DCMAKE_BUILD_TYPE=Release \
"

do_configure:prepend() {
    # mirror path is the deps.txt url minus https://
    rm -rf ${ORT_MIRROR}
    grep -v '^#' ${S}/cmake/deps.txt | while IFS=';' read -r name url sha; do
        for f in ${DL_DIR}/ortdep-$name.zip ${DL_DIR}/ortdep-$name.tar.gz; do
            if [ -f "$f" ]; then
                dst=${ORT_MIRROR}/$(echo "$url" | sed 's|^https://||')
                mkdir -p $(dirname "$dst")
                ln -sf "$f" "$dst"
            fi
        done
    done
}

do_install:append() {
    # python package is assembled in the build tree, not by cmake install
    install -d ${D}${PYTHON_SITEPACKAGES_DIR}
    cp -r --no-preserve=ownership ${B}/onnxruntime ${D}${PYTHON_SITEPACKAGES_DIR}/
    find ${D}${PYTHON_SITEPACKAGES_DIR}/onnxruntime -name '__pycache__' -prune -exec rm -rf {} +
    # the pybind module has ort linked in, the copy of libonnxruntime and the static provider are just build tree leftovers
    rm -f ${D}${PYTHON_SITEPACKAGES_DIR}/onnxruntime/capi/libonnxruntime.so* ${D}${PYTHON_SITEPACKAGES_DIR}/onnxruntime/capi/*.a
    # same two lines setup.py's save_build_and_package_info() writes for a non-cuda build
    printf "package_name = 'onnxruntime'\n__version__ = '%s'\n" "$(head -n 1 ${S}/VERSION_NUMBER)" \
        > ${D}${PYTHON_SITEPACKAGES_DIR}/onnxruntime/capi/build_and_package_info.py
}

PACKAGES =+ "${PN}-python"
FILES:${PN}-python = "${PYTHON_SITEPACKAGES_DIR}/onnxruntime"
INSANE_SKIP:${PN}-python += "dev-so"

# providers_shared is a dlopened plugin not a dev symlink, keep it in the runtime package
FILES_SOLIBSDEV = "${libdir}/libonnxruntime.so"
FILES:${PN} += "${libdir}/libonnxruntime_providers_shared.so"
INSANE_SKIP:${PN} += "dev-so"
