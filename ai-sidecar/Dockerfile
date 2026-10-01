FROM python:3.11-slim

WORKDIR /app

ENV PIP_ROOT_USER_ACTION=ignore
ENV PYTHONUNBUFFERED=1

# Cài đặt công cụ hệ thống tối thiểu
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    curl \
    && rm -rf /var/lib/apt/lists/*

COPY requirements.txt .

# Cài PyTorch bản CPU siêu nhẹ (~200MB thay vì bản CUDA 2.5GB-4GB)
RUN pip install --no-cache-dir torch torchvision --index-url https://download.pytorch.org/whl/cpu && \
    pip install --no-cache-dir -r requirements.txt

# Copy source code & model (đã loại trừ .venv qua .dockerignore)
COPY . .

EXPOSE 8000

CMD ["uvicorn", "embedding_service:app", "--host", "0.0.0.0", "--port", "8000", "--workers", "2"]