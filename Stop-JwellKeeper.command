#!/bin/bash

clear

echo "======================================="
echo "         Stopping JwellKeeper"
echo "======================================="
echo

cd "$(dirname "$0")"

docker compose down

echo
echo "JwellKeeper has been stopped."
echo

read -p "Press Enter to close..."
