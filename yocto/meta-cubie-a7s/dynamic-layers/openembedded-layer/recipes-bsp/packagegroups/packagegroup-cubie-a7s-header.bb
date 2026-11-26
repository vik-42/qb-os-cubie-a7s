SUMMARY = "Userspace tools and Python libraries for the Cubie A7S 40-pin header"
DESCRIPTION = "gpiod command line tools for the gpio lines, plus Python access to gpio, pwm, \
i2c and spi when meta-python is in the build. The pins themselves come from CUBIE_HEADER."
LICENSE = "MIT"

inherit packagegroup

# python bits only if meta-python is around, gpiod tools just need meta-oe
RDEPENDS:${PN} = "libgpiod-tools \
    ${@bb.utils.contains('BBFILE_COLLECTIONS', 'meta-python', 'python3-gpiod python3-periphery python3-smbus2 python3-spidev', '', d)}"
