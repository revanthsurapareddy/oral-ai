from ultralytics import YOLO
import os

def resume_interrupted():
    last_checkpoint = r'c:\Users\vavil\OneDrive\Desktop\oral cancer backend\runs\detect\runs\detect\oral_cancer_model_resumed\weights\last.pt'
    
    if not os.path.exists(last_checkpoint):
        print(f"ERROR: Could not find checkpoint at {last_checkpoint}")
        return

    print(f"Resuming YOLOv8 training from {last_checkpoint} exactly where it left off...")
    
    # Load the interrupted checkpoint
    model = YOLO(last_checkpoint)
    
    # Pass resume=True to continue exactly from the interrupted epoch
    model.train(resume=True)
    
    print("\n===========================================")
    print("Training complete!")
    print("===========================================")

if __name__ == '__main__':
    resume_interrupted()
