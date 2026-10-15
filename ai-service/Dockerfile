FROM python:3.11-slim
WORKDIR /app
ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PORT=5001
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt \
    && useradd --create-home --uid 10001 appuser
COPY forecast_service.py .
USER appuser
EXPOSE 5001
CMD ["sh", "-c", "exec gunicorn --bind 0.0.0.0:${PORT} --workers 2 forecast_service:app"]
