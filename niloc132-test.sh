#!/usr/bin/env bash

time bash -c 'set -e; for run in {1..50}; do for child in {1..50}; do curl https://localhost:8080/large.js -k > /dev/null 2>&1 & done; wait; echo $run done; done'