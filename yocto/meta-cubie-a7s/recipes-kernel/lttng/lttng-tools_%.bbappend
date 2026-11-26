# lttng-modules never finishes building against this kernel, ros2 userspace tracing is fine without it
RRECOMMENDS:${PN}:remove = "lttng-modules"
