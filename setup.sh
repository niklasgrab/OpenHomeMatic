#!/bin/bash
#Description: Setup script to install OpenHomeMatic
NAME="OpenHomeMatic"
DRIVER="homematic-cc1101"
VERSION="1.0.0"
SERVER="https://github.com/isc-konstanz"

# Set the targeted location of the OpenMUC framework if not already set.
if [ -z ${ROOT_DIR+x} ]; then
    ROOT_DIR="/opt/emonmuc"
fi
TEMP_DIR="/var/tmp/emonmuc"

# Verify, if the specific version does exists already
if [ ! -f "$ROOT_DIR/bundles/openmuc-driver-$DRIVER-$VERSION.jar" ]; then
    # Create temporary directory and remove old versions
    mkdir -p "$TEMP_DIR/$DRIVER"
    rm -f "$ROOT_DIR/bundles/openmuc-driver-$DRIVER*"
    rm -rf "$ROOT_DIR/lib/device/$DRIVER*"

    wget --quiet --show-progress --directory-prefix="$ROOT_DIR/bundles" "$SERVER/$NAME/releases/download/v$VERSION/openmuc-driver-$DRIVER-$VERSION.jar"
    wget --quiet --show-progress --directory-prefix="$TEMP_DIR/$DRIVER" "$SERVER/$NAME/releases/download/v$VERSION/$DRIVER-$VERSION.zip"
    unzip -q "$TEMP_DIR/$DRIVER/$DRIVER-$VERSION.zip" -d "$TEMP_DIR/$DRIVER"
    mv -f "$TEMP_DIR/$DRIVER/lib/device/$DRIVER" "$ROOT_DIR/lib/device/$DRIVER"
    rm -rf "$TEMP_DIR/$DRIVER"
fi
exit 0
