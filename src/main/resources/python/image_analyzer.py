import argparse
import os
import json
import cv2
from ultralytics import YOLO
import numpy as np

def analyze_image(image_path, model_path, output_dir):
    """
    이미지를 분석하고 감지된 객체 정보를 반환합니다.
    """
    # 모델 로드
    model = YOLO(model_path)
    
    # 이미지 로드
    image = cv2.imread(image_path)
    if image is None:
        raise ValueError(f"이미지를 읽을 수 없습니다: {image_path}")
    
    # 출력 디렉토리 생성
    os.makedirs(output_dir, exist_ok=True)
    
    # 결과 이미지를 저장할 경로
    filename = os.path.basename(image_path)
    result_image_path = os.path.join(output_dir, filename)
    
    # 이미지 분석
    results = model(image)
    
    # 결과 정보 추출
    detected_objects = []
    highest_confidence = 0
    main_object = None
    
    for result in results:
        boxes = result.boxes
        for box in boxes:
            confidence = float(box.conf[0])
            class_id = int(box.cls[0])
            class_name = model.names[class_id]
            
            x1, y1, x2, y2 = map(int, box.xyxy[0])
            
            # 감지 정보 저장
            detected_object = {
                "class_name": class_name,
                "confidence": confidence,
                "box": [x1, y1, x2, y2]
            }
            
            detected_objects.append(detected_object)
            
            # 가장 높은 신뢰도의 객체 저장
            if confidence > highest_confidence:
                highest_confidence = confidence
                main_object = detected_object
    
    # 결과 이미지에 바운딩 박스 그리기
    result_image = image.copy()
    for obj in detected_objects:
        x1, y1, x2, y2 = obj["box"]
        cv2.rectangle(result_image, (x1, y1), (x2, y2), (0, 255, 0), 2)
        cv2.putText(result_image, f"{obj['class_name']} {obj['confidence']:.2f}", 
                   (x1, y1 - 10), cv2.FONT_HERSHEY_SIMPLEX, 0.5, (0, 255, 0), 2)
    
    # 결과 이미지 저장
    cv2.imwrite(result_image_path, result_image)
    
    # 상대 경로로 변환
    relative_image_path = os.path.basename(result_image_path)
    
    # 결과 반환
    return {
        "detected_objects": detected_objects,
        "main_object": main_object["class_name"] if main_object else "Unknown",
        "main_confidence": main_object["confidence"] if main_object else 0,
        "detected_image": relative_image_path,
        "object_count": len(detected_objects)
    }

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Image analysis with YOLOv8")
    parser.add_argument("--image", required=True, help="Path to the image file")
    parser.add_argument("--model", required=True, help="Path to the YOLOv8 model file")
    parser.add_argument("--output", required=True, help="Output directory for results")
    
    args = parser.parse_args()
    
    try:
        result = analyze_image(args.image, args.model, args.output)
        print(json.dumps(result))
    except Exception as e:
        error_result = {
            "error": str(e),
            "detected_objects": [],
            "main_object": "Error",
            "main_confidence": 0,
            "detected_image": "",
            "object_count": 0
        }
        print(json.dumps(error_result))