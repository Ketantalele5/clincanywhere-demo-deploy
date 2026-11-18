#!/bin/sh
set -eu
export LOG_FILE="/var/log/clincanywhere/${HOSTNAME:-clincanywhere}.log"
exec java -jar /app/app.jar
