# we use zenoh, drop cyclone dds (and iceoryx with it), fast dds stays since its the default
ROS_BUILD_DEPENDS:remove = "rmw-cyclonedds-cpp"
ROS_EXEC_DEPENDS:remove = "rmw-cyclonedds-cpp"

# cyclone used to leak this into the sysroot and libstatistics-collector needs it
ROS_BUILD_DEPENDS:append = " rosidl-default-runtime"
