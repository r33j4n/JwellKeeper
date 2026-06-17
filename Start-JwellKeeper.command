#!/bin/bash

clear

echo "======================================="
echo "         Starting JwellKeeper"
echo "======================================="
echo

# Start Docker Desktop if not running

if ! docker info >/dev/null 2>&1; then
echo "Docker Desktop is not running."
echo "Starting Docker Desktop..."
open -a Docker

```
echo
echo "Waiting for Docker to start..."

until docker info >/dev/null 2>&1
do
    sleep 5
    echo "Still waiting..."
done
```

fi

echo
echo "Docker is ready."
echo

# Go to the script directory

cd "$(dirname "$0")"

echo "Starting containers..."
docker compose up -d

echo
echo "Containers started."
echo

# Get local IP address

IP=$(ipconfig getifaddr en0)

if [ -z "$IP" ]; then
IP=$(ipconfig getifaddr en1)
fi

echo "======================================="
echo "          JwellKeeper Ready"
echo "======================================="
echo
echo "This Mac:"
echo "https://localhost"
echo
echo "Other phones and computers:"
echo "https://$IP"
echo
echo "If a browser warning appears,"
echo "choose Advanced -> Continue."
echo
echo "======================================="
echo

read -p "Press Enter to close..."
