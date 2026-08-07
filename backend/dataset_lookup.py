"""
dataset_lookup.py
-----------------
Dataset Match & Lookup Engine for OralAI Backend.

Supports exact matching against the uploaded expert-annotated dataset:
  - dataset/images/  (500 images)
  - dataset/masks/   (500 expert masks)
  - dataset/mapping.csv

Mode 1: "Expert Annotation (Dataset Match)"
  - Uploaded image matches a stored image in dataset/images (via SHA256 byte or pixel array hash).
  - Loads corresponding expert mask from dataset/masks.
  - Generates overlay image and calculates lesion percentage from stored mask.
  - Sets prediction_mode = "Expert Annotation (Dataset Match)".

Mode 2: "AI Prediction"
  - Unseen image with no dataset match.
  - Passes image to best_unet_model.keras for TensorFlow/Keras U-Net prediction.
  - Sets prediction_mode = "AI Prediction".
"""

import os
import glob
import hashlib
import io
import csv
import base64
import numpy as np
import cv2
from PIL import Image

BASE_DIR = os.path.dirname(os.path.abspath(__file__))

# ── Dual Hash Index ────────────────────────────────────────────────────────
# Maps: sha256 -> entry_dict
_FILE_HASH_INDEX: dict = {}
_PIXEL_HASH_INDEX: dict = {}
_INDEX_BUILT: bool = False


def _compute_pixel_hash(img_rgb: np.ndarray) -> str:
    """SHA256 of standard 256x256 RGB pixel array (resilient to browser JPEG re-compression)."""
    resized = cv2.resize(img_rgb, (256, 256), interpolation=cv2.INTER_AREA)
    return hashlib.sha256(resized.tobytes()).hexdigest()


def _build_gt_mask_from_yolo(lbl_path: str, img_w: int, img_h: int) -> np.ndarray:
    """Generates binary mask from YOLO polygon label (.txt)."""
    mask = np.zeros((img_h, img_w), dtype=np.uint8)
    if not lbl_path or not os.path.exists(lbl_path):
        return mask

    with open(lbl_path, "r") as f:
        for line in f:
            parts = line.strip().split()
            if len(parts) < 7:
                continue
            coords = [float(x) for x in parts[1:]]
            pts = [
                [int(coords[i] * img_w), int(coords[i + 1] * img_h)]
                for i in range(0, len(coords) - 1, 2)
            ]
            if len(pts) >= 3:
                cv2.fillPoly(mask, [np.array(pts, dtype=np.int32)], 255)
    return mask


def build_hash_index() -> int:
    """Indexes all images in dataset/images and yolo_seg_dataset at startup."""
    global _FILE_HASH_INDEX, _PIXEL_HASH_INDEX, _INDEX_BUILT
    if _INDEX_BUILT:
        return len(_FILE_HASH_INDEX)

    print("=" * 60)
    print(" Building Dataset Match Lookup Index...")
    print("=" * 60)

    count = 0

    # 1. Primary Dataset: dataset/images, dataset/masks, mapping.csv
    dataset_dir = os.path.join(BASE_DIR, "dataset")
    images_dir = os.path.join(dataset_dir, "images")
    masks_dir = os.path.join(dataset_dir, "masks")
    mapping_csv = os.path.join(dataset_dir, "mapping.csv")

    # Load mapping.csv if present
    mapping_dict = {}
    if os.path.exists(mapping_csv):
        try:
            with open(mapping_csv, "r", encoding="utf-8") as f:
                reader = csv.DictReader(f)
                for row in reader:
                    img_name = row.get("image") or row.get("image_name") or row.get("img")
                    mask_name = row.get("mask") or row.get("mask_name")
                    if img_name and mask_name:
                        mapping_dict[img_name] = mask_name
        except Exception as e:
            print(f" Warning reading mapping.csv: {e}")

    # Index dataset/images/
    if os.path.isdir(images_dir):
        for ext in ("*.jpg", "*.jpeg", "*.png", "*.JPG", "*.JPEG", "*.PNG"):
            for img_path in glob.glob(os.path.join(images_dir, ext)):
                fname = os.path.basename(img_path)
                fname_stem, _ = os.path.splitext(fname)

                # Locate corresponding mask
                mask_path = None
                if fname in mapping_dict:
                    candidate = os.path.join(masks_dir, mapping_dict[fname])
                    if os.path.exists(candidate):
                        mask_path = candidate

                if not mask_path and os.path.isdir(masks_dir):
                    for m_ext in (".png", ".jpg", ".jpeg", ".PNG", ".JPG"):
                        candidate = os.path.join(masks_dir, fname_stem + m_ext)
                        if os.path.exists(candidate):
                            mask_path = candidate
                            break

                try:
                    # File SHA256
                    with open(img_path, "rb") as f:
                        file_sha = hashlib.sha256(f.read()).hexdigest()

                    # Pixel SHA256
                    img_cv2 = cv2.imread(img_path)
                    if img_cv2 is None:
                        continue
                    img_rgb = cv2.cvtColor(img_cv2, cv2.COLOR_BGR2RGB)
                    pixel_sha = _compute_pixel_hash(img_rgb)

                    entry = {
                        "img_path": img_path,
                        "mask_path": mask_path,
                        "filename": fname,
                        "source": "expert_dataset",
                    }

                    _FILE_HASH_INDEX[file_sha] = entry
                    _PIXEL_HASH_INDEX[pixel_sha] = entry
                    count += 1
                except Exception as e:
                    print(f" Warning indexing {img_path}: {e}")

    # 2. Also index yolo_seg_dataset/ images as backup
    yolo_dirs = [
        (os.path.join(BASE_DIR, "yolo_seg_dataset", "images", "train"), os.path.join(BASE_DIR, "yolo_seg_dataset", "labels", "train")),
        (os.path.join(BASE_DIR, "yolo_seg_dataset", "images", "val"), os.path.join(BASE_DIR, "yolo_seg_dataset", "labels", "val")),
    ]
    for img_dir, lbl_dir in yolo_dirs:
        if not os.path.isdir(img_dir):
            continue
        for ext in ("*.jpg", "*.jpeg", "*.png", "*.JPG", "*.JPEG", "*.PNG"):
            for img_path in glob.glob(os.path.join(img_dir, ext)):
                fname = os.path.basename(img_path)
                fname_stem, _ = os.path.splitext(fname)
                lbl_path = os.path.join(lbl_dir, fname_stem + ".txt")

                try:
                    with open(img_path, "rb") as f:
                        file_sha = hashlib.sha256(f.read()).hexdigest()

                    img_cv2 = cv2.imread(img_path)
                    if img_cv2 is None:
                        continue
                    img_rgb = cv2.cvtColor(img_cv2, cv2.COLOR_BGR2RGB)
                    pixel_sha = _compute_pixel_hash(img_rgb)

                    entry = {
                        "img_path": img_path,
                        "mask_path": lbl_path if os.path.exists(lbl_path) else None,
                        "filename": fname,
                        "source": "yolo_dataset",
                    }

                    if file_sha not in _FILE_HASH_INDEX:
                        _FILE_HASH_INDEX[file_sha] = entry
                    if pixel_sha not in _PIXEL_HASH_INDEX:
                        _PIXEL_HASH_INDEX[pixel_sha] = entry
                    count += 1
                except Exception:
                    pass

    _INDEX_BUILT = True
    print(f" Dataset Match Index Complete: {count} images indexed.")
    print("=" * 60)
    return count


def lookup_dataset(image_bytes: bytes) -> dict | None:
    """
    Checks if uploaded image matches dataset/images.

    Returns:
        dict if exact match found (Mode 1: Expert Annotation (Dataset Match))
        None if no match found (Mode 2: AI Prediction)
    """
    if not _INDEX_BUILT:
        build_hash_index()

    # Check File SHA256
    file_sha = hashlib.sha256(image_bytes).hexdigest()
    entry = _FILE_HASH_INDEX.get(file_sha)

    # Check Pixel SHA256
    if entry is None:
        try:
            image_pil = Image.open(io.BytesIO(image_bytes)).convert("RGB")
            img_rgb = np.array(image_pil)
            pixel_sha = _compute_pixel_hash(img_rgb)
            entry = _PIXEL_HASH_INDEX.get(pixel_sha)
        except Exception:
            pass

    if entry is None:
        return None  # No match -> Fallback to Mode 2: AI Prediction

    # ── Match Found -> Mode 1: Expert Annotation (Dataset Match) ───────────
    img_path = entry["img_path"]
    mask_path = entry["mask_path"]

    image_pil = Image.open(io.BytesIO(image_bytes)).convert("RGB")
    orig_w, orig_h = image_pil.size
    image_cv2 = cv2.cvtColor(np.array(image_pil), cv2.COLOR_RGB2BGR)

    binary_mask = np.zeros((orig_h, orig_w), dtype=np.uint8)

    if mask_path and os.path.exists(mask_path):
        if mask_path.endswith(".txt"):
            binary_mask = (_build_gt_mask_from_yolo(mask_path, orig_w, orig_h) > 127).astype(np.uint8)
        else:
            mask_img = cv2.imread(mask_path, cv2.IMREAD_GRAYSCALE)
            if mask_img is not None:
                mask_resized = cv2.resize(mask_img, (orig_w, orig_h), interpolation=cv2.INTER_NEAREST)
                binary_mask = (mask_resized > 127).astype(np.uint8)

    lesion_pixels = int(np.sum(binary_mask > 0))
    total_pixels = binary_mask.size
    lesion_pct = round((lesion_pixels / total_pixels) * 100.0, 2)
    lesion_detected = lesion_pixels > 25 and lesion_pct > 0.0

    # Mask PNG Base64
    mask_png = (binary_mask * 255).astype(np.uint8)
    _, mask_buf = cv2.imencode(".png", mask_png)
    mask_b64 = base64.b64encode(mask_buf).decode("utf-8")

    # High-Precision Overlay (Green Fill + White Boundary Contour)
    overlay = image_cv2.copy()
    if lesion_detected:
        overlay[binary_mask > 0] = [0, 210, 0]  # BGR Green Fill
        cv2.addWeighted(overlay, 0.40, image_cv2, 0.60, 0, overlay)

        contours, _ = cv2.findContours(binary_mask, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
        cv2.drawContours(overlay, contours, -1, (255, 255, 255), 2, cv2.LINE_AA)

        if contours:
            all_pts = np.vstack(contours)
            x, y, w, h = cv2.boundingRect(all_pts)
            label = f"Expert Mask: {lesion_pct}%"
            cv2.putText(overlay, label, (max(10, x), max(30, y - 10)),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.75, (255, 255, 255), 2, cv2.LINE_AA)

    _, ov_buf = cv2.imencode(".jpg", overlay)
    overlay_b64 = base64.b64encode(ov_buf).decode("utf-8")

    risk_pct = int(min(99, max(30, round(lesion_pct * 3)))) if lesion_detected else 0
    prediction_mode = "Expert Annotation (Dataset Match)"

    return {
        "prediction_mode": prediction_mode,
        "mode_used": prediction_mode,
        "model_used": prediction_mode,
        "match_mode": "dataset",
        "matched_filename": entry["filename"],
        "lesion_detected": lesion_detected,
        "lesion_percentage": lesion_pct,
        "mask_image": f"data:image/png;base64,{mask_b64}",
        "overlay_image": f"data:image/jpeg;base64,{overlay_b64}",
        "image_base64": f"data:image/jpeg;base64,{overlay_b64}",
        "status": "success",
        "has_cancer": lesion_detected,
        "risk_level": "High" if lesion_detected else "Low",
        "risk_percentage": risk_pct,
        "message": f"Expert Annotation (Dataset Match) found for {entry['filename']}. Returned ground-truth mask ({lesion_pct}% lesion area)." if lesion_detected else f"Expert Annotation (Dataset Match) found for {entry['filename']}. Healthy scan (0% lesion area).",
    }
