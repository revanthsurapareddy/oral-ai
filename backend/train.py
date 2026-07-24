from ultralytics import YOLO
import os

def main():
    print("Initializing YOLOv8 training...")
    
    # Load a pretrained YOLO model (recommended for transfer learning)
    model = YOLO('yolov8n.pt')

    # Path to your dataset definition
    data_path = r'c:\Users\vavil\OneDrive\Desktop\oralcancertrainig\roboflow\data.yaml'
    
    if not os.path.exists(data_path):
        print(f"ERROR: Could not find dataset at {data_path}")
        return

    # Train the model
    print(f"Starting training on {data_path} for 20 epochs...")
    results = model.train(
        data=data_path,
        epochs=20,
        imgsz=640,
        name='oral_cancer_model',
        project='runs/detect' # Ensure it saves to a predictable location
    )
    
    print("\n===========================================")
    print("Training complete!")
    print("Model weights saved to runs/detect/oral_cancer_model/weights/best.pt")
    print("===========================================")

if __name__ == '__main__':
    main()
