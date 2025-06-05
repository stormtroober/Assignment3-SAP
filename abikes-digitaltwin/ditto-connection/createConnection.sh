curl -X POST \
  -u devops:foobar \
  -H "Content-Type: application/json" \
  -d @abike-connection.json \
  "http://localhost:8080/api/2/connections?timeout=5s"
