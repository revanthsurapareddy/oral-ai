import cv2
import numpy as np
import os
from PIL import Image

def get_thumb(img_array):
    up_gray = cv2.cvtColor(img_array, cv2.COLOR_RGB2GRAY)
    return cv2.resize(up_gray, (32, 32)).astype(np.float32)

base_dir = "c:/Users/vavil/OneDrive/Desktop/oral cancer backend/demo_outputs"
in_file = "1input.jpeg"
in_path = os.path.join(base_dir, in_file)

# Original thumbnail
ref_img = cv2.imread(in_path, cv2.IMREAD_GRAYSCALE)
ref_thumb = cv2.resize(ref_img, (32, 32)).astype(np.float32)

# Simulate canvas resize (800 max)
pil_img = Image.open(in_path).convert("RGB")
w, h = pil_img.size
if w > h and w > 800:
    h = int(h * 800 / w)
    w = 800
elif h > w and h > 800:
    w = int(w * 800 / h)
    h = 800

pil_img = pil_img.resize((w, h), Image.Resampling.LANCZOS)
up_thumb = get_thumb(np.array(pil_img))

mse = np.mean((up_thumb - ref_thumb) ** 2)
print("MSE between original and canvas-resized:", mse)
