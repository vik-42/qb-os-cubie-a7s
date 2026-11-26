SUMMARY = "Playground image for the Radxa Cubie A7S: base image plus NPU, video and GPU samples for experimenting"
LICENSE = "MIT"

require recipes-core/images/core-image-minimal.bb
require cubie-a7s-image-common.inc
require cubie-a7s-image-dev.inc

# lenet runs a real network on the npu, check with tim-vx-lenet
IMAGE_INSTALL += "tim-vx-samples"

# cedarc decode/encode demos for the video engines
IMAGE_INSTALL += "libcedarc-demos"

# test pattern and raw file sources for trying gstreamer pipelines
IMAGE_INSTALL += "gstreamer1.0-plugins-base-videotestsrc gstreamer1.0-plugins-base-rawparse"

# gpu checks: kms/gles, a gles benchmark, vulkan and opencl info
IMAGE_INSTALL += "kmscube glmark2 vulkan-tools clinfo"
