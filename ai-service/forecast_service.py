import math
import os
from datetime import date, timedelta
from statistics import mean

from flask import Flask, jsonify, request

app = Flask(__name__)
SUPPORTED_MEALS = {"BREAKFAST", "LUNCH"}


@app.get("/health")
def health():
    return jsonify({"status": "ok", "service": "smartmeal-ai"})


def _parse_date(value, field_name):
    if not isinstance(value, str):
        raise ValueError(f"{field_name} must be an ISO date string.")
    try:
        return date.fromisoformat(value)
    except ValueError as error:
        raise ValueError(f"{field_name} must be an ISO date string.") from error


def _validate_payload(payload):
    if not isinstance(payload, dict):
        raise ValueError("Request body must be a JSON object.")

    meal = str(payload.get("meal_category", "")).strip().upper()
    if meal not in SUPPORTED_MEALS:
        raise ValueError("meal_category must be BREAKFAST or LUNCH.")

    target_date = _parse_date(payload.get("target_date"), "target_date")
    history = payload.get("historical_data", [])
    if not isinstance(history, list):
        raise ValueError("historical_data must be a list.")

    points = []
    for index, point in enumerate(history):
        if not isinstance(point, dict):
            raise ValueError(f"historical_data[{index}] must be an object.")
        observed_date = _parse_date(point.get("date"), f"historical_data[{index}].date")
        quantity = point.get("quantity")
        if isinstance(quantity, bool) or not isinstance(quantity, (int, float)) or not math.isfinite(quantity) or quantity < 0:
            raise ValueError(f"historical_data[{index}].quantity must be a non-negative number.")
        points.append((observed_date, float(quantity)))

    if any(observed_date >= target_date for observed_date, _ in points):
        raise ValueError("Historical observations must be earlier than target_date.")
    return meal, target_date, points


def _make_forecast(meal, target_date, points):
    by_date = {}
    for observed_date, quantity in points:
        by_date[observed_date] = by_date.get(observed_date, 0.0) + quantity
    ordered = sorted(by_date.items())

    if len(ordered) >= 3:
        origin = ordered[0][0].toordinal()
        x_values = [observed_date.toordinal() - origin for observed_date, _ in ordered]
        y_values = [quantity for _, quantity in ordered]
        x_mean = mean(x_values)
        y_mean = mean(y_values)
        denominator = sum((x_value - x_mean) ** 2 for x_value in x_values)
        slope = sum((x_value - x_mean) * (y_value - y_mean) for x_value, y_value in zip(x_values, y_values)) / denominator
        prediction = max(0.0, y_mean + slope * (target_date.toordinal() - origin - x_mean))
        method = "LINEAR_REGRESSION"
        explanation = "Linear trend fitted to historical booking totals."
    elif ordered:
        prediction = mean(quantity for _, quantity in ordered)
        method = "BASELINE_MEAN"
        explanation = "Baseline mean used because fewer than three distinct historical dates were available."
    else:
        prediction = 0.0
        method = "BASELINE_NO_HISTORY"
        explanation = "No historical bookings were available; zero is a placeholder baseline, not a learned forecast."

    return {
        "meal_category": meal,
        "target_date": target_date.isoformat(),
        "predicted_quantity": round(prediction, 2),
        "method": method,
        "sample_count": len(ordered),
        "explanation": explanation,
    }


@app.route("/forecast", methods=["GET", "POST"])
def forecast():
    if request.method == "GET":
        target_date = request.args.get("target_date") or (date.today() + timedelta(days=1)).isoformat()
        payload = {
            "meal_category": request.args.get("meal", "BREAKFAST"),
            "target_date": target_date,
            "historical_data": [],
        }
    else:
        payload = request.get_json(silent=True)

    try:
        meal, target_date, points = _validate_payload(payload)
    except ValueError as error:
        return jsonify({"error": str(error)}), 400
    return jsonify(_make_forecast(meal, target_date, points))


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=int(os.environ.get("PORT", "5001")), debug=False)
