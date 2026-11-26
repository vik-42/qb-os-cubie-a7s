SUMMARY = "Robot brain image for the Radxa Cubie A7S: boots, has ROS2 and Zenoh"

# start from meta-ros's minimal ros image
require recipes-core/images/ros-image-core.bb
require recipes-core/images/cubie-a7s-image-common.inc

# ros2 over zenoh, rmw-zenoh-env makes it the default in login shells
IMAGE_INSTALL += "rmw-zenoh-cpp rmw-zenoh-env"

# docker for running extra nodes in containers, when meta-virtualization is in bblayers.
# it is off at boot, start it with /etc/init.d/docker.init start
IMAGE_INSTALL += "${@bb.utils.contains('BBFILE_COLLECTIONS', 'virtualization-layer', 'docker-moby', '', d)}"
