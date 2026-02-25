#!/bin/bash

for i in $(seq 1 20); do
  echo "Request $i: $(date)"
  timeout 10 curl -s -k --http2 https://localhost:8443/external

  if [ $? -ne 0 ]; then
    echo "!!! REQUEST FAILED OR TIMEOUT - BUG REPRODUCED !!!"
    exit 1
  fi
done

echo "All requests completed successfully"
