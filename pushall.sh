#!/bin/bash
if [ -z "$1" ]; then
    echo "Usage: ./pushall.sh -m \"commit message\""
    exit 1
fi
if [ "$1" = "-m" ]; then shift; fi
git add .
git commit -m "$1"
git push -u origin main
