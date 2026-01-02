#!/bin/bash
set -e
SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

curl --digest -u "admin:password" 'http://localhost:11222/rest/v2/caches/repl-c/k1' -X PUT -d 'value' -H 'Content-Type: text/plain'
curl --digest -u "admin:password" 'http://localhost:11222/rest/v2/caches/repl-c?action=size'
curl --digest -u "admin:password" 'http://localhost:11223/rest/v2/caches/repl-c?action=size'
