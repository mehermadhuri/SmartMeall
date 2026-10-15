import pytest

from forecast_service import app


@pytest.fixture()
def client():
    app.config.update(TESTING=True)
    with app.test_client() as test_client:
        yield test_client


def test_health_endpoint(client):
    response = client.get("/health")

    assert response.status_code == 200
    assert response.get_json() == {"status": "ok", "service": "smartmeal-ai"}


def test_forecast_fits_linear_trend_to_historical_bookings(client):
    response = client.post(
        "/forecast",
        json={
            "meal_category": "BREAKFAST",
            "target_date": "2026-10-04",
            "historical_data": [
                {"date": "2026-10-01", "quantity": 10},
                {"date": "2026-10-02", "quantity": 12},
                {"date": "2026-10-03", "quantity": 14},
            ],
        },
    )

    assert response.status_code == 200
    assert response.get_json()["predicted_quantity"] == 16
    assert response.get_json()["method"] == "LINEAR_REGRESSION"
    assert response.get_json()["sample_count"] == 3


def test_insufficient_history_is_labeled_as_baseline(client):
    response = client.post(
        "/forecast",
        json={
            "meal_category": "LUNCH",
            "target_date": "2026-10-04",
            "historical_data": [
                {"date": "2026-10-01", "quantity": 10},
                {"date": "2026-10-03", "quantity": 20},
            ],
        },
    )

    assert response.status_code == 200
    assert response.get_json()["predicted_quantity"] == 15
    assert response.get_json()["method"] == "BASELINE_MEAN"
    assert "fewer than three" in response.get_json()["explanation"]


def test_no_history_returns_explicit_non_model_baseline(client):
    response = client.post(
        "/forecast",
        json={"meal_category": "BREAKFAST", "target_date": "2026-10-04", "historical_data": []},
    )

    assert response.status_code == 200
    assert response.get_json()["predicted_quantity"] == 0
    assert response.get_json()["method"] == "BASELINE_NO_HISTORY"


@pytest.mark.parametrize(
    "body",
    [
        {"meal_category": "DINNER", "target_date": "2026-10-04", "historical_data": []},
        {"meal_category": "LUNCH", "target_date": "not-a-date", "historical_data": []},
        {
            "meal_category": "LUNCH",
            "target_date": "2026-10-04",
            "historical_data": [{"date": "2026-10-05", "quantity": 5}],
        },
        {
            "meal_category": "LUNCH",
            "target_date": "2026-10-04",
            "historical_data": [{"date": "2026-10-03", "quantity": -1}],
        },
    ],
)
def test_invalid_forecast_inputs_return_400(client, body):
    response = client.post("/forecast", json=body)

    assert response.status_code == 400
    assert response.get_json()["error"]


def test_legacy_get_route_is_explicitly_a_baseline(client):
    response = client.get("/forecast?meal=LUNCH&target_date=2026-10-04")

    assert response.status_code == 200
    assert response.get_json()["method"] == "BASELINE_NO_HISTORY"