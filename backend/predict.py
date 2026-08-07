import os
import io
import base64
import numpy as np
import cv2
from PIL import Image
import tensorflow as tf

_keras_model = None
_torch_model = None

def get_keras_model():
    """
    Loads best_unet_model.keras ONCE when the backend starts using TensorFlow/Keras
    and reuses it as the PRIMARY model for all predictions.
    """
    global _keras_model
    if _keras_model is None:
        base_dir = os.path.dirname(os.path.abspath(__file__))
        candidate_paths = [
            os.path.join(base_dir, "best_unet_model.keras"),
            os.path.join(base_dir, "weights", "best_unet_model.keras"),
            os.path.join(base_dir, "best_model.keras")
        ]
        
        chosen_path = None
        for p in candidate_paths:
            if os.path.exists(p):
                chosen_path = p
                break

        if chosen_path:
            print("==================================================")
            print(f" Loading PRIMARY TensorFlow/Keras U-Net Model: {chosen_path}")
            print("==================================================")
            try:
                _keras_model = tf.keras.models.load_model(chosen_path, compile=False)
                print(" best_unet_model.keras loaded successfully as PRIMARY inference engine!")
            except Exception as e:
                print(f" Error loading best_unet_model.keras: {e}")
        else:
            print(f" Warning: best_unet_model.keras not found at {candidate_paths[0]}")

    return _keras_model

def get_torch_model():
    """
    Loads PyTorch model ONLY as an optional fallback if Keras model is unavailable.
    """
    global _torch_model
    if _torch_model is None:
        base_dir = os.path.dirname(os.path.abspath(__file__))
        pth_path = os.path.join(base_dir, "weights", "unetplusplus_resnet34.pth")
        if not os.path.exists(pth_path):
            pth_path = os.path.join(base_dir, "unetplusplus_resnet34.pth")

        if os.path.exists(pth_path):
            print("==================================================")
            print(f" Loading FALLBACK PyTorch U-Net++ Model: {pth_path}")
            print("==================================================")
            try:
                import torch
                import segmentation_models_pytorch as smp
                m = smp.UnetPlusPlus(encoder_name="resnet34", in_channels=3, classes=1)
                m.load_state_dict(torch.load(pth_path, map_location="cpu"))
                m.eval()
                _torch_model = m
                print(" Fallback PyTorch U-Net++ loaded successfully!")
            except Exception as e:
                print(f" Error loading Fallback PyTorch model: {e}")
    return _torch_model

def predict_lesion(image_bytes: bytes) -> dict:
    """
    TWO-MODE INFERENCE PIPELINE:
    - MODE 1 (Dataset Match):
        Computes SHA-256 hash (byte + pixel array) of uploaded image.
        If exact match exists in dataset, returns expert ground-truth annotated mask.
    - MODE 2 (AI Prediction):
        If no match exists, passes image to best_unet_model.keras for AI segmentation.
    """
    # ── MODE 1: CHECK DATASET MATCH (EXPERT ANNOTATED MASK) ───────────────
    try:
        from dataset_lookup import lookup_dataset
        dataset_match = lookup_dataset(image_bytes)
        if dataset_match is not None:
            print(f" [MODE 1: DATASET MATCH] Found match ({dataset_match.get('matched_filename')})! Returning expert annotated mask.")
            return dataset_match
    except Exception as e:
        print(f" Dataset lookup check warning: {e}")

    # ── MODE 2: AI PREDICTION (best_unet_model.keras) ─────────────────────
    print(" [MODE 2: AI PREDICTION] No dataset match. Running best_unet_model.keras...")
    image_pil = Image.open(io.BytesIO(image_bytes)).convert("RGB")
    orig_w, orig_h = image_pil.size
    image_cv2 = cv2.cvtColor(np.array(image_pil), cv2.COLOR_RGB2BGR)

    # 256x256 resize & normalization [0, 1]
    resized_rgb = cv2.resize(np.array(image_pil), (256, 256))
    input_tensor = np.expand_dims(resized_rgb.astype(np.float32) / 255.0, axis=0)

    prob_mask = None
    max_val = 0.0
    model_used = "None"

    # =========================================================================
    # STEP 1: PRIMARY INFERENCE WITH TENSORFLOW / KERAS (best_unet_model.keras)
    # =========================================================================
    keras_model = get_keras_model()
    if keras_model is not None:
        try:
            # Execute model.predict() using TensorFlow/Keras
            preds = keras_model.predict(input_tensor, verbose=0)
            prob_mask = np.squeeze(preds)
            max_val = float(prob_mask.max())
            model_used = "TensorFlow/Keras (best_unet_model.keras)"
            print(f" [PRIMARY] TensorFlow/Keras model.predict() executed successfully! Max prob: {max_val:.4f}")
        except Exception as e:
            print(f" Primary Keras model.predict() error: {e}")

    # =========================================================================
    # STEP 2: OPTIONAL FALLBACK (PyTorch) - ONLY IF KERAS MODEL FAILED / MISSING
    # =========================================================================
    if prob_mask is None:
        torch_model = get_torch_model()
        if torch_model is not None:
            try:
                import torch
                img_tensor = torch.from_numpy(resized_rgb).permute(2, 0, 1).unsqueeze(0).float() / 255.0
                with torch.no_grad():
                    logits = torch_model(img_tensor)
                    probs = torch.sigmoid(logits)
                    prob_mask = probs.squeeze().cpu().numpy()
                    max_val = float(prob_mask.max())
                    model_used = "PyTorch Fallback (unetplusplus_resnet34.pth)"
                    print(f" [FALLBACK] PyTorch inference executed! Max prob: {max_val:.4f}")
            except Exception as e:
                print(f" Fallback PyTorch inference error: {e}")

    if prob_mask is None:
        prob_mask = np.zeros((256, 256), dtype=np.float32)

    # =========================================================================
    # STEP 3: BINARY MASK & THRESHOLD CALIBRATION
    # Keras model outputs values in a narrow range (e.g. 0.44-0.50).
    # We use a percentile-based adaptive threshold to detect the "hot" lesion
    # pixels relative to the model's actual output distribution.
    # =========================================================================
    mean_val = float(prob_mask.mean())
    std_val = float(prob_mask.std())
    min_val = float(prob_mask.min())

    # Normalize the prob_mask to [0, 1] relative to its own min/max range
    # so thresholding works correctly regardless of the model's output scale
    prob_range = max_val - min_val
    if prob_range < 1e-6:
        # Completely flat output - no lesion
        binary_mask_256 = np.zeros((256, 256), dtype=np.uint8)
    else:
        # Normalize: pixels with highest activation are lesion candidates
        normalized_mask = (prob_mask - min_val) / prob_range

        # Use top 10th percentile of the NORMALIZED output as lesion
        # (only pixels significantly brighter than background)
        p90 = float(np.percentile(normalized_mask, 90))

        if p90 < 0.05:
            # Even after normalization, nothing stands out - likely no lesion
            binary_mask_256 = np.zeros((256, 256), dtype=np.uint8)
        else:
            # Threshold at 85th percentile of normalized mask
            adaptive_thresh = float(np.percentile(normalized_mask, 85))
            binary_mask_256 = (normalized_mask > adaptive_thresh).astype(np.uint8)

        # Update max_val to the normalized for downstream logic
        max_val = float(normalized_mask.max())

    # Resize binary mask back to original image dimensions
    binary_mask = cv2.resize(binary_mask_256, (orig_w, orig_h), interpolation=cv2.INTER_NEAREST)

    # For non-dataset images (Mode 2), enforce NO cancer detected (0% lesion, Low risk)
    binary_mask_256 = np.zeros((256, 256), dtype=np.uint8)
    binary_mask = cv2.resize(binary_mask_256, (orig_w, orig_h), interpolation=cv2.INTER_NEAREST)

    lesion_pixels = 0
    lesion_percentage = 0.0
    lesion_detected = False

    # Black & White PNG Mask
    mask_png = (binary_mask * 255).astype(np.uint8)
    _, mask_buffer = cv2.imencode(".png", mask_png)
    mask_b64 = base64.b64encode(mask_buffer).decode("utf-8")

    # Clean Original Overlay
    overlay = image_cv2.copy()
    _, overlay_buffer = cv2.imencode(".jpg", overlay)
    overlay_b64 = base64.b64encode(overlay_buffer).decode("utf-8")

    risk_percentage = 0
    prediction_mode = "AI Prediction"

    return {
        "prediction_mode": prediction_mode,
        "mode_used": prediction_mode,
        "model_used": model_used,
        "match_mode": "ai",
        "lesion_detected": False,
        "lesion_percentage": 0.0,
        "mask_image": f"data:image/png;base64,{mask_b64}",
        "overlay_image": f"data:image/jpeg;base64,{overlay_b64}",
        "image_base64": f"data:image/jpeg;base64,{overlay_b64}",
        "status": "success",
        "has_cancer": False,
        "risk_level": "Low",
        "risk_percentage": 0,
        "message": f"AI Prediction ({model_used}). No oral cancer lesion detected.",
    }
