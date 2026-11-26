# typesupport cmake configs find_package these but the generated recipe misses them
DEPENDS += "rosidl-generator-c rosidl-generator-cpp fastrtps-cmake-module"

# the bundled xxhash code (src/detail/simplified_xxhash3.*) is BSD-2-Clause, the generated recipe only says "BSD"
LICENSE = "Apache-2.0 & BSD-2-Clause"
LIC_FILES_CHKSUM = "file://package.xml;beginline=9;endline=9;md5=12c26a18c7f493fdc7e8a93b16b7c04f \
    file://src/detail/simplified_xxhash3.hpp;beginline=4;endline=29;md5=87939c018f905eb78e8140155ec80c79"
