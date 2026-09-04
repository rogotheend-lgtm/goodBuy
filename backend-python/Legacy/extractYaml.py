import yaml
from app import app  # app.py에서 FastAPI app 객체 가져오기

# OpenAPI JSON 스키마 가져오기
openapi_schema = app.openapi()

# YAML 파일로 저장
with open("openapi.yaml", "w", encoding="utf-8") as f:
    yaml.dump(openapi_schema, f, allow_unicode=True, sort_keys=False)

print("openapi.yaml 추출 완료!")