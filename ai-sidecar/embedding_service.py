import os

from fastapi import FastAPI, UploadFile, File
from huggingface_hub import login
from pydantic import BaseModel
from sentence_transformers import SentenceTransformer
# from transformers import CLIPProcessor, CLIPModel
from PIL import Image
import torch
import io
import numpy as np
from transformers import AutoImageProcessor, AutoModel

app = FastAPI(title="SQB AI Sidecar", version="1.0.0")

from btprop import btprop_router
app.include_router(btprop_router, prefix="/api/v1/ai/btprop", tags=["BTProp Hallucination Checker"])

from dotenv import load_dotenv
load_dotenv()  # Load biến từ .env

# ===== Load models (1 lần khi khởi động) =====

TEXT_MODEL = os.getenv("TEXT_MODEL",
    "intfloat/multilingual-e5-small")  # 384 dims, chạy nhanh
    # "BAAI/bge-m3")  # 1024 dims, chất lượng cao nhưng chậm

IMAGE_MODEL = os.getenv("IMAGE_MODEL",
    "facebook/dinov2-small")  # 384 dims, chạy nhanh
    # "facebook/dinov2-base")  # 768 dims, chất lượng cao hơn

HF_TOKEN = os.getenv("HF_TOKEN")
if HF_TOKEN:
    login(token=HF_TOKEN)
print(f"HF TOKEN: {HF_TOKEN}")
print(f"Loading text model: {TEXT_MODEL}")
print(f"Loading image model: {IMAGE_MODEL}")

text_model = SentenceTransformer(TEXT_MODEL, token=HF_TOKEN)
image_processor = AutoImageProcessor.from_pretrained(IMAGE_MODEL, token=HF_TOKEN)
image_model = AutoModel.from_pretrained(IMAGE_MODEL, token=HF_TOKEN)

# # Multimodal model - CLIP
# clip_model = CLIPModel.from_pretrained("openai/clip-vit-base-patch32")
# clip_processor = CLIPProcessor.from_pretrained("openai/clip-vit-base-patch32")


# ===== Request/Response Models =====
class TextEmbeddingRequest(BaseModel):
    text: str


class BatchTextEmbeddingRequest(BaseModel):
    texts: list[str]


class EmbeddingResponse(BaseModel):
    embedding: list[float]
    model: str
    dimensions: int


class BatchEmbeddingResponse(BaseModel):
    embeddings: list[list[float]]
    model: str
    dimensions: int


# ===== Text Embedding Endpoints =====
@app.post("/api/v1/embed/text", response_model=EmbeddingResponse)
async def embed_text(request: TextEmbeddingRequest):
    """Generate embedding cho text"""
    embedding = text_model.encode(
        request.text,
        normalize_embeddings=True,  # Normalize cho cosine similarity
        convert_to_numpy=True
    )
    return EmbeddingResponse(
        embedding=embedding.tolist(),
        model=TEXT_MODEL,
        dimensions=len(embedding)
    )


@app.post("/api/v1/embed/text/batch", response_model=BatchEmbeddingResponse)
async def embed_text_batch(request: BatchTextEmbeddingRequest):
    """Generate embeddings cho nhiều texts (batch)"""
    embeddings = text_model.encode(
        request.texts,
        normalize_embeddings=True,
        convert_to_numpy=True,
        batch_size=32,
        show_progress_bar=False
    )
    return BatchEmbeddingResponse(
        embeddings=[e.tolist() for e in embeddings],
        model=TEXT_MODEL,
        dimensions=len(embeddings[0])
    )


# ===== Image Embedding Endpoints =====
@app.post("/api/v1/embed/image", response_model=EmbeddingResponse)
async def embed_image(file: UploadFile = File(...)):
    """Generate embedding cho image"""
    image_bytes = await file.read()
    image = Image.open(io.BytesIO(image_bytes)).convert("RGB")

    inputs = image_processor(images=image, return_tensors="pt")

    with torch.no_grad():
        outputs = image_model(**inputs)
        # Lấy [CLS] token embedding
        embedding = outputs.last_hidden_state[:, 0, :].squeeze().numpy()
        # Normalize
        embedding = embedding / np.linalg.norm(embedding)

    return EmbeddingResponse(
        embedding=embedding.tolist(),
        model=IMAGE_MODEL,
        dimensions=len(embedding)
    )


# # ===== Multimodal Embedding (CLIP) =====
# @app.post("/api/v1/embed/clip/text", response_model=EmbeddingResponse)
# async def embed_clip_text(request: TextEmbeddingRequest):
#     """CLIP text embedding (so sánh được với image)"""
#     inputs = clip_processor(text=[request.text], return_tensors="pt", padding=True)
#
#     with torch.no_grad():
#         text_features = clip_model.get_text_features(**inputs)
#         # Normalize
#         text_features = text_features / text_features.norm(dim=-1, keepdim=True)
#
#     embedding = text_features.squeeze().numpy()
#     return EmbeddingResponse(
#         embedding=embedding.tolist(),
#         model="clip-vit-base-patch32",
#         dimensions=len(embedding)
#     )
#
#
# @app.post("/api/v1/embed/clip/image", response_model=EmbeddingResponse)
# async def embed_clip_image(file: UploadFile = File(...)):
#     """CLIP image embedding (so sánh được với text)"""
#     image_bytes = await file.read()
#     image = Image.open(io.BytesIO(image_bytes)).convert("RGB")
#
#     inputs = clip_processor(images=image, return_tensors="pt")
#
#     with torch.no_grad():
#         image_features = clip_model.get_image_features(**inputs)
#         # Normalize
#         image_features = image_features / image_features.norm(dim=-1, keepdim=True)
#
#     embedding = image_features.squeeze().numpy()
#     return EmbeddingResponse(
#         embedding=embedding.tolist(),
#         model="clip-vit-base-patch32",
#         dimensions=len(embedding)
#     )


# # ===== Similarity Endpoint =====
# class SimilarityRequest(BaseModel):
#     embedding1: list[float]
#     embedding2: list[float]
#
#
# class SimilarityResponse(BaseModel):
#     cosine_similarity: float
#     cosine_distance: float
#
#
# @app.post("/similarity", response_model=SimilarityResponse)
# async def calculate_similarity(request: SimilarityRequest):
#     """Tính cosine similarity giữa 2 vectors"""
#     v1 = np.array(request.embedding1)
#     v2 = np.array(request.embedding2)
#
#     # Normalize
#     v1 = v1 / np.linalg.norm(v1)
#     v2 = v2 / np.linalg.norm(v2)
#
#     similarity = float(np.dot(v1, v2))
#     distance = 1.0 - similarity
#
#     return SimilarityResponse(
#         cosine_similarity=similarity,
#         cosine_distance=distance
#     )


# ===== Health Check =====
@app.get("/health")
async def health():
    return {
        "status": "ok",
        "models": {
            "text_model": TEXT_MODEL,
            "image_model": IMAGE_MODEL,
            "text_dims": text_model.get_embedding_dimension(),
            "image_dims": image_model.config.hidden_size,
            # "multimodal": "clip-vit-base-patch32"
        }
    }

# # Development (model nhẹ)
# uvicorn embedding_service:app --port 8000
#
# # Production (nếu server mạnh, swap sang model nặng)
# TEXT_MODEL=BAAI/bge-m3 IMAGE_MODEL=facebook/dinov2-base \
#   uvicorn embedding_service:app --port 8000