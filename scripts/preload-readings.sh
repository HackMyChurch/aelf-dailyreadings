#!/bin/bash
set -e

#
# Pre-Load readings so that around 1 month worth of reading is available on the first install.
# This is not perfect and implies at least 1 update / month BUT it should considerably reduce
# the frustration of user who had an Internet access when the installed the application but do
# have it anymore when opening it for the first time.
#

cd "$(dirname "$0")/.."

BASE_URL="http://api.app.epitre.co"
BASE_DEST="./app/src/main/assets/preloaded-reading"
DAYS_AHEAD=45
OFFICES="informations lectures tierce sexte none laudes vepres complies messes"
VERSION=$(grep android:versionCode ./app/src/main/AndroidManifest.xml | cut -d= -f2 | tr -d \")

# Ensure target folder exists
mkdir -p "$BASE_DEST"

# Pre-load readings
for i in $(seq 0 $DAYS_AHEAD)
do
    DATE=$(date --iso-8601 --date="+ $i day")
    for OFFICE in $OFFICES
	do
        READING="${OFFICE}_${DATE}.rss"
        if [ ! -f "${BASE_DEST}/${READING}" ]
        then
            echo "Pre-loading ${OFFICE} for ${DATE}"
            wget -q "${BASE_URL}/${VERSION}/office/${OFFICE}/${DATE}" -O "${BASE_DEST}/${READING}"
        fi
    done
done

# Clean old readings / errors
TODAY=$(date --iso-8601)
for READING in $(ls $BASE_DEST)
do
    READING_DATE=${READING: -14:-4}
    if [[ "${TODAY}" > "$READING_DATE" ]]
    then
        echo "Prunning outdated $BASE_DEST/$READING"
    elif grep -q '<source>error</source>' "$BASE_DEST/$READING"
    then
        echo "Prunning errored $BASE_DEST/$READING"
        rm -f "$BASE_DEST/$READING"
    fi
done

echo "All done !"

