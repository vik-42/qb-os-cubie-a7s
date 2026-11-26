# u-boot's signing tool runs on the build host and needs p11-kit
PACKAGECONFIG:append:class-native = " p11-kit"
