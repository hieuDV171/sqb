FROM python:3.11-slim

WORKDIR /app

# Cài đặt công cụ hệ thống tối thiểu
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    curl \
    && rm -rf /var/lib/apt/lists/*

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy source code & model
COPY . .

EXPOSE 8000

CMD ["uvicorn", "embedding_service:app", "--host", "0.0.0.0", "--port", "8000", "--workers", "2"]