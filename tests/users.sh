url="http://localhost:8080"

curl -sX POST "$url/users" -H  "accept: */*" -H  "Content-Type: application/json" -d "{\"name\":\"$1\",\"password\":\"$2\"}" | jq '.'