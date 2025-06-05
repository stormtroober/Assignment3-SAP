curl -X PUT 'http://localhost:8080/api/2/policies/org.eclipse.ditto:simple-policy' \
-u 'ditto:ditto' \
-H 'Content-Type: application/json' \
--data-binary @simple_policy.json