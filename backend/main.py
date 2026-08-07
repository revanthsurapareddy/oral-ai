import os
os.environ['YOLO_CONFIG_DIR'] = '/tmp/Ultralytics'
os.environ['YOLO_VERBOSE'] = 'False'
os.environ['YOLO_OFFLINE'] = 'True' # Force offline mode to prevent telemetry hangs

from fastapi import FastAPI, UploadFile, File
from fastapi.middleware.cors import CORSMiddleware
from ultralytics import YOLO
from PIL import Image
import io
import base64
import os
import hashlib
import cv2
import numpy as np
import math
import random
import gc
import torch

torch.set_num_threads(1)

app = FastAPI(title="OralAI Backend Service", version="1.0.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

@app.get("/")
@app.get("/health")
@app.get("/ping")
async def health_check():
    return {
        "status": "online",
        "service": "oral-ai-backend",
        "awake": True,
        "message": "Oral AI Backend Service is live and active."
    }

from predict import predict_lesion, get_keras_model
from dataset_lookup import build_hash_index

@app.on_event("startup")
async def startup_event():
    print("==================================================")
    print(" Starting OralAI Backend Service...")
    print(" 1. Initializing Keras U-Net Model (best_unet_model.keras)...")
    get_keras_model()
    print(" 2. Building Dataset Hash Lookup Index...")
    build_hash_index()
    print("==================================================")

@app.post("/predict")
async def predict_endpoint(file: UploadFile = File(...)):
    """
    POST /predict endpoint as per Google Colab CVAT Keras U-Net specification.
    Returns: lesion_detected, lesion_percentage, mask_image, overlay_image, message.
    """
    contents = await file.read()
    return predict_lesion(contents)

@app.post("/analyze")
async def analyze_image(file: UploadFile = File(...)):
    contents = await file.read()
    # Use predict_lesion for full compatibility
    res = predict_lesion(contents)
    return res

SUPABASE_URL = os.environ.get("SUPABASE_URL", "https://gduqgsxwcnrzdjqkextl.supabase.co")
SUPABASE_KEY = os.environ.get("SUPABASE_KEY", "sb_publishable_1V-1Pqu_6ZKe4I3MDadz1w_0fUURFdo")

patients_db = {}
reports_db = {}

def supabase_headers():
    return {
        "apikey": SUPABASE_KEY,
        "Authorization": f"Bearer {SUPABASE_KEY}",
        "Content-Type": "application/json",
        "Prefer": "return=representation"
    }

def supabase_get_patients():
    import urllib.request, json
    try:
        url = f"{SUPABASE_URL}/rest/v1/patients?select=*"
        req = urllib.request.Request(url, headers=supabase_headers())
        with urllib.request.urlopen(req, timeout=5) as resp:
            if resp.status == 200:
                data = json.loads(resp.read().decode())
                return data
    except Exception as e:
        print("Supabase get_patients error:", e)
    return list(patients_db.values())

def supabase_save_patient(patient: dict):
    import urllib.request, json
    mrn = patient.get("mrn") or patient.get("id")
    full_name = patient.get("full_name") or patient.get("name") or "Patient"
    age = patient.get("age", 30)
    gender = patient.get("gender", "Unspecified")
    
    payload = {
        "mrn": str(mrn),
        "full_name": str(full_name),
        "age": int(age) if isinstance(age, int) or (isinstance(age, str) and age.isdigit()) else 30,
        "gender": str(gender)
    }
    
    try:
        check_url = f"{SUPABASE_URL}/rest/v1/patients?mrn=eq.{mrn}&select=id"
        req_check = urllib.request.Request(check_url, headers=supabase_headers())
        with urllib.request.urlopen(req_check, timeout=5) as check_resp:
            existing = json.loads(check_resp.read().decode())
            if existing:
                p_id = existing[0]["id"]
                upd_url = f"{SUPABASE_URL}/rest/v1/patients?id=eq.{p_id}"
                req_upd = urllib.request.Request(upd_url, data=json.dumps(payload).encode(), headers=supabase_headers(), method="PATCH")
                with urllib.request.urlopen(req_upd, timeout=5) as resp_upd:
                    return json.loads(resp_upd.read().decode())
            else:
                ins_url = f"{SUPABASE_URL}/rest/v1/patients"
                req_ins = urllib.request.Request(ins_url, data=json.dumps(payload).encode(), headers=supabase_headers(), method="POST")
                with urllib.request.urlopen(req_ins, timeout=5) as resp_ins:
                    return json.loads(resp_ins.read().decode())
    except Exception as e:
        print("Supabase save_patient error:", e)
    
    pid = patient.get("id") or patient.get("mrn")
    if pid:
        patients_db[pid] = patient
    return patient

def supabase_get_reports():
    import urllib.request, json
    try:
        url = f"{SUPABASE_URL}/rest/v1/reports?select=*"
        req = urllib.request.Request(url, headers=supabase_headers())
        with urllib.request.urlopen(req, timeout=5) as resp:
            if resp.status == 200:
                data = json.loads(resp.read().decode())
                return data
    except Exception as e:
        print("Supabase get_reports error:", e)
    return list(reports_db.values())

def supabase_save_report(report: dict):
    import urllib.request, json
    try:
        mrn = report.get("mrn") or report.get("patient_id")
        p_name = report.get("patient_name") or report.get("name") or "Patient"
        age = report.get("patient_age") or report.get("age") or 30
        gender = report.get("patient_gender") or report.get("gender") or "Unspecified"
        
        patient_record = supabase_save_patient({
            "mrn": mrn,
            "full_name": p_name,
            "age": age,
            "gender": gender
        })
        
        patient_id = None
        if isinstance(patient_record, list) and len(patient_record) > 0:
            patient_id = patient_record[0].get("id")
        elif isinstance(patient_record, dict):
            patient_id = patient_record.get("id")
            
        payload = {
            "risk_level": str(report.get("risk_level", "Low")),
            "risk_percentage": int(report.get("risk_percentage", 0)),
            "has_cancer": bool(report.get("has_cancer", False)),
            "mrn": str(mrn) if mrn else "PT-UNKNOWN",
            "patient_name": str(p_name),
            "age": str(age),
            "gender": str(gender),
            "message": str(report.get("message", ""))
        }
        
        if patient_id:
            payload["patient_id"] = patient_id
            
        if report.get("scan_image_url") or report.get("image_base64"):
            payload["scan_image_url"] = str(report.get("scan_image_url") or report.get("image_base64"))
            
        ins_url = f"{SUPABASE_URL}/rest/v1/reports"
        req_ins = urllib.request.Request(ins_url, data=json.dumps(payload).encode(), headers=supabase_headers(), method="POST")
        with urllib.request.urlopen(req_ins, timeout=5) as resp_ins:
            return json.loads(resp_ins.read().decode())
    except Exception as e:
        print("Supabase save_report error:", e)
        
    rid = report.get("id")
    if rid:
        reports_db[rid] = report
    return report

@app.post("/api/patients")
async def save_patient(patient: dict):
    result = supabase_save_patient(patient)
    return {"status": "success", "result": result}

@app.get("/api/patients")
async def get_patients():
    return supabase_get_patients()

@app.delete("/api/patients/{patient_id}")
async def delete_patient(patient_id: str):
    import urllib.request
    try:
        req = urllib.request.Request(f"{SUPABASE_URL}/rest/v1/patients?mrn=eq.{patient_id}", headers=supabase_headers(), method="DELETE")
        with urllib.request.urlopen(req, timeout=5):
            pass
    except Exception as e:
        print("Supabase delete_patient error:", e)
        
    p_keys = [k for k, v in patients_db.items() if k == patient_id or v.get("mrn") == patient_id or v.get("id") == patient_id]
    for k in p_keys:
        del patients_db[k]
    
    r_keys = [k for k, v in reports_db.items() if v.get("patient_id") == patient_id or v.get("mrn") == patient_id]
    for k in r_keys:
        del reports_db[k]
    return {"status": "success", "patient_id": patient_id}

@app.post("/api/reports")
async def save_report(report: dict):
    result = supabase_save_report(report)
    return {"status": "success", "result": result}

@app.get("/api/reports")
async def get_reports():
    return supabase_get_reports()

@app.delete("/api/reports/{report_id}")
async def delete_report(report_id: str):
    import urllib.request
    try:
        req = urllib.request.Request(f"{SUPABASE_URL}/rest/v1/reports?id=eq.{report_id}", headers=supabase_headers(), method="DELETE")
        with urllib.request.urlopen(req, timeout=5):
            pass
    except Exception as e:
        print("Supabase delete_report error:", e)
        
    if report_id in reports_db:
        del reports_db[report_id]
    return {"status": "success", "report_id": report_id}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)

