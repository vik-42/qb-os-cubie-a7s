# dev images: passwordless root (debug-tweaks) and dnf for the package feed, never ship these
CUBIE_DEV_IMAGE = "1"
IMAGE_FEATURES += "debug-tweaks package-management"
