# /etc/os-release says which OS this is (QB OS and its version), tools like rosdep and containers read it
IMAGE_INSTALL += "os-release"

# the image only holds what it needs, the first boot grows / to fill the sd card
IMAGE_INSTALL += "rootfs-grow"

# sets the clock from the network (the board would start in 2018, the build's fixed timestamp), chrony is in meta-networking
IMAGE_INSTALL += "${@bb.utils.contains('BBFILE_COLLECTIONS', 'networking-layer', 'chrony chronyc', '', d)}"

# onboard wifi plus tools to connect, core-image-minimal doesnt pull MACHINE_EXTRA_RRECOMMENDS on its own
IMAGE_INSTALL += "${MACHINE_EXTRA_RRECOMMENDS} wpa-supplicant iw"

# onnx runtime with the npu provider, python module pulls in the lib
IMAGE_INSTALL += "onnxruntime-python"

# gstreamer on the video engines: omx hw codecs, v4l2 cameras, parsers, rtp/udp streaming, mp4/mkv
IMAGE_INSTALL += "gstreamer1.0 gstreamer1.0-omx gstreamer1.0-plugins-good-video4linux2 \
    gstreamer1.0-plugins-bad-videoparsersbad gstreamer1.0-plugins-good-rtp gstreamer1.0-plugins-good-rtpmanager \
    gstreamer1.0-plugins-good-udp gstreamer1.0-plugins-good-isomp4 gstreamer1.0-plugins-good-matroska \
    gstreamer1.0-plugins-base-videoconvertscale gstreamer1.0-plugins-base-app"

# gpu: gles/egl/gbm through mesa-pvr, vulkan and opencl through their loaders, all on the closed img libs
IMAGE_INSTALL += "libegl-mesa libgles2-mesa libgbm mesa-megadriver \
    img-bxm-umlibs-vulkan mesa-vulkan-drivers img-bxm-umlibs-opencl"

# ssh server, root may log in but only with a key on release images (see cubie_login), dev images add debug-tweaks
IMAGE_FEATURES += "ssh-server-dropbear allow-root-login"

# wifi creds from WIFI_SSID/WIFI_PSK in build/conf/site.conf (untracked), written into the rootfs only so no package carries them
ROOTFS_POSTPROCESS_COMMAND += "cubie_wifi_creds;"
python cubie_wifi_creds() {
    import hashlib, os
    ssid = d.getVar('WIFI_SSID')
    psk = d.getVar('WIFI_PSK')
    if not ssid or not psk:
        return
    key = hashlib.pbkdf2_hmac('sha1', psk.encode(), ssid.encode(), 4096, 32).hex()
    conf = os.path.join(d.getVar('IMAGE_ROOTFS'), 'etc/wpa_supplicant.conf')
    with open(conf, 'w') as f:
        f.write('ctrl_interface=/var/run/wpa_supplicant\nctrl_interface_group=0\nupdate_config=1\n\n')
        f.write('network={\n\tssid="%s"\n\tpsk=%s\n}\n' % (ssid, key))
    os.chmod(conf, 0o600)
}

# serial console: the whole kernel boot log still shows, then bootmisc.sh (which applies /etc/sysctl.conf)
# lowers the console level so only errors get printed over the login prompt. dmesg and /var/log/messages keep everything.
# written here so it works whether or not procps already installed a sysctl.conf
ROOTFS_POSTPROCESS_COMMAND += "cubie_quiet_console;"
cubie_quiet_console() {
	printf '\n# console shows kernel errors only, everything is in dmesg\nkernel.printk = 4 7 1 7\n' >> ${IMAGE_ROOTFS}${sysconfdir}/sysctl.conf
}

# login from site.conf: CUBIE_SSH_AUTHORIZED_KEYS = path to a public key file, CUBIE_ROOT_PASSWORD_HASH = output of `openssl passwd -6`
# CUBIE_ROOT_PASSWORD_EXPIRED = "1" makes root pick a new password at the first login
ROOTFS_POSTPROCESS_COMMAND += "cubie_login;"
do_rootfs[file-checksums] += "${@'%s:True' % d.getVar('CUBIE_SSH_AUTHORIZED_KEYS') if d.getVar('CUBIE_SSH_AUTHORIZED_KEYS') else ''}"
python cubie_login() {
    import os, re
    rootfs = d.getVar('IMAGE_ROOTFS')
    dev = d.getVar('CUBIE_DEV_IMAGE') == '1'
    keyfile = d.getVar('CUBIE_SSH_AUTHORIZED_KEYS')
    pwhash = d.getVar('CUBIE_ROOT_PASSWORD_HASH')

    if keyfile:
        with open(keyfile) as f:
            keys = f.read().strip() + '\n'
        sshdir = os.path.join(rootfs, 'home/root/.ssh')
        os.makedirs(sshdir, exist_ok=True)
        os.chmod(sshdir, 0o700)
        with open(os.path.join(sshdir, 'authorized_keys'), 'w') as f:
            f.write(keys)
        os.chmod(os.path.join(sshdir, 'authorized_keys'), 0o600)

    # dev images keep debug-tweaks' empty root password
    if dev:
        return

    insecure = [f for f in ('debug-tweaks', 'allow-empty-password', 'empty-root-password') if bb.utils.contains('IMAGE_FEATURES', f, True, False, d)]
    if insecure:
        bb.fatal('release image has %s, check EXTRA_IMAGE_FEATURES in local.conf or build the -dev image' % ' '.join(insecure))

    if not keyfile and not pwhash:
        bb.fatal('release image with no way to log in: set CUBIE_SSH_AUTHORIZED_KEYS and/or CUBIE_ROOT_PASSWORD_HASH in site.conf')

    expired = d.getVar('CUBIE_ROOT_PASSWORD_EXPIRED') == '1'
    if expired and not pwhash:
        bb.fatal('CUBIE_ROOT_PASSWORD_EXPIRED needs CUBIE_ROOT_PASSWORD_HASH')

    if pwhash:
        if not re.match(r'^\$[0-9a-z]+\$', pwhash):
            bb.fatal('CUBIE_ROOT_PASSWORD_HASH must be a crypt hash, e.g. from `openssl passwd -6`')
        shadow = os.path.join(rootfs, 'etc/shadow')
        with open(shadow) as f:
            lines = f.read().splitlines()
        for i, l in enumerate(lines):
            if l.startswith('root:'):
                fields = l.split(':')
                fields[1] = pwhash
                # last change on day 0 makes login ask for a new password first
                if expired:
                    fields[2] = '0'
                lines[i] = ':'.join(fields)
        with open(shadow, 'w') as f:
            f.write('\n'.join(lines) + '\n')

    # keys only over ssh, the password is for the serial console
    dropbear = os.path.join(rootfs, 'etc/default/dropbear')
    with open(dropbear, 'w') as f:
        f.write('DROPBEAR_EXTRA_ARGS="-s"\n')
}
