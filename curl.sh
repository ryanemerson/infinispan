#!/bin/bash
reset

set -x

BASE_URL=https://issues.redhat.com
API_URL=${BASE_URL}/rest/api/2

cat << EOF > headers
Authorization: Bearer MTMxNjY1NTQ4NTA2OuMgyff5LJSzp/Xov4uGq4Y2OWZa
Content-Type: application/json
EOF

PROJECT="ISPN"
SUMMARY="javax.inject:javax.inject 1.0-PFD-1"
JQL="project = ${PROJECT} AND summary ~ '${SUMMARY}'"

# Search existing issues
ISSUES=$(curl --silent ${API_URL}/search \
  -G --data-urlencode "jql=${JQL}"\
  -H @headers
)
TOTAL_ISSUES=$(echo ${ISSUES} | jq -r .total)
if [ ${TOTAL_ISSUES} == 0 ]; then
  echo "Existing Jira not found, creating a new one"
  exit
elif [ ${TOTAL_ISSUES} -gt 1 ]; then
  echo "Multiple Jiras found in '${PROJECT}' with summary ~ '${SUMMARY}'"
  exit 1
else
  NEW_PR="https://github.com/infinispan/infinispan/pull/1"
  ISSUE=$(echo ${ISSUES} | jq .issues[0])
  ISSUE_KEY=$(echo ${ISSUE} | jq -r .key)
  EXISTING_PRS=$(echo ${ISSUE} | jq .fields.customfield_12310220)
  ALL_PRS="$(echo ${EXISTING_PRS} | jq '. + ["'${NEW_PR}'"]' | jq -r '. |= join("\\n")')"
  echo $ALL_PRS

  cat << EOF > update-jira.json
  {
      "update": {
          "customfield_12310220": [
             {
                 "set": "${ALL_PRS}"
             }
          ]
      }
  }
EOF
  cat update-jira.json

  # Add PR to existing issue
  curl -X PUT ${API_URL}/issue/${ISSUE_KEY} \
    -H @headers \
    --data @update-jira.json
fi
