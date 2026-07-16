curl -X 'POST' \                                                                                                                                 
  'http://127.0.0.1:8000/translation/invoke' \
  -H 'accept: application/json' \
  -H 'Content-Type: application/json' \
  -d '{
  "input": {
    "input": "你好",
    "language": "日语"
  },
  "config": {},
  "kwargs": {
    "additionalProp1": {}
  }
}'
{"output":"こんにちは","metadata":{"run_id":"fb6c033b-e597-4d11-82f8-a1721559cb60","feedback_tokens":[]}}