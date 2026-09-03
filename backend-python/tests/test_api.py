from fastapi.testclient import TestClient

from app import MockOcrEngine, create_app


def test_extracts_transactions_from_uploaded_image() -> None:
    application = create_app(MockOcrEngine)

    with TestClient(application) as client:
        with open("userEX.png", "rb") as image:
            response = client.post(
                "/ocr/extraction",
                files={"file": ("transactions.png", image, "image/png")},
            )

    assert response.status_code == 200
    assert response.json() == {
        "transactions": [
            {"counterparty": "벌크커피", "amount": 3000},
            {"counterparty": "토스페이_TOSS", "amount": 630},
        ],
        "summary": {"total_count": 2, "total_amount": 3630},
    }


def test_rejects_non_image_upload() -> None:
    application = create_app(MockOcrEngine)

    with TestClient(application) as client:
        response = client.post(
            "/ocr/extraction",
            files={"file": ("input.txt", b"not-an-image", "text/plain")},
        )

    assert response.status_code == 400


def test_health_reports_loaded_engine() -> None:
    application = create_app(MockOcrEngine)

    with TestClient(application) as client:
        response = client.get("/health")

    assert response.status_code == 200
    assert response.json()["engineLoaded"] is True
