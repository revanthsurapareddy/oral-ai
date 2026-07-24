from ultralytics import YOLO
import os

def resume():
    # Path to the 20-epoch checkpoint you will save today
    last_checkpoint = r'c:\Users\vavil\OneDrive\Desktop\oral cancer backend\runs\detect\runs\detect\oral_cancer_model\weights\best.pt'
    
    if not os.path.exists(last_checkpoint):
        print(f"ERROR: Could not find checkpoint at {last_checkpoint}")
        print("Make sure you completed the initial 20 epochs of training in train.py.")
        return

    print(f"Resuming YOLOv8 training from {last_checkpoint} for the remaining 30 epochs...")
    
    # Load the checkpoint as the starting weights for a new run
    model = YOLO(last_checkpoint)
    data_path = r'c:\Users\vavil\OneDrive\Desktop\oralcancertrainig\roboflow\data.yaml'

    # Continue training for the remaining epochs
    results = model.train(
        data=data_path,
        epochs=30, # the remaining epochs you want to train
        imgsz=640,
        name='oral_cancer_model_resumed',
        project='runs/detect'
    )
    
    print("\n===========================================")
    print("Continued training complete!")
    print("Final model weights saved to runs/detect/oral_cancer_model_resumed/weights/best.pt")
    print("===========================================")

if __name__ == '__main__':
    resume()
