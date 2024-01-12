#!/bin/bash
set -e -o pipefail

CURL="curl --no-progress-meter --fail-with-body"
if [[ "$RUNNER_DEBUG" == "1" ]]; then
  set -x
  CURL="${CURL} --verbose"
fi

function requiredEnv() {
  for ENV in $@; do
      if [ -z "${!ENV}" ]; then
        echo "${ENV} variable must be set"
        exit 1
      fi
  done
}

requiredEnv TOKEN PROJECT_KEY PULL_REQUEST SUMMARY TYPE

echo "'PROJECT_KEY'"
echo "'PULL_REQUEST'"
echo "'SUMMARY'"
echo "'TYPE'"

BASE_URL=https://issues.redhat.com
API_URL=${BASE_URL}/rest/api/2

cat << EOF | tee headers
Authorization: Bearer ${TOKEN}
Content-Type: application/json
EOF

PROJECT_ID=12317323
ISSUE_TYPE_ID=12

cat << EOF | tee create-jira.json
  {
    "fields": {
      "project": {
        "id": "${PROJECT_ID}"
      },
      "summary": "${SUMMARY}",
      "description": "${DESCRIPTION}",
      "customfield_12310220": "${PULL_REQUEST}",
      "issuetype": {
        "id": "${ISSUE_TYPE_ID}"
      }
    }
  }
EOF
curl -v -H @headers --data @create-jira.json $API_URL/issue
export JIRA_TICKET_URL=test
