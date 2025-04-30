import argparse
import os
import cv2
import json
from ultralytics import YOLO
import numpy as np

def process_video(video_path, model_path, output_dir):
    """
    비디오를 분석하고 감지된 객체 정보를 반환합니다.
    """
    # 모델 로드
    model = YOLO(model_path)
    
    # 비디오 열기
    cap = cv2.VideoCapture(video_path)
    frame_count = 0
    detections = []
    
    # 출력 디렉토리 생성
    os.makedirs(output_dir, exist_ok=True)
    
    # 결과 이미지를 저장할 경로
    result_image_path = os.path.join(output_dir, "detection_result.jpg")
    
    # 비디오 프레임 처리
    max_confidence = 0
    best_detection = None
    best_frame = None
    
    while cap.isOpened():
        success, frame = cap.read()
        if not success:
            break
            
        if frame_count % 30 == 0:  # 1초마다 분석 (30fps 기준)
            # 프레임 분석
            results = model(frame)
            
            # 결과 정보 추출
            for result in results:
                boxes = result.boxes
                for box in boxes:
                    confidence = float(box.conf[0])
                    class_id = int(box.cls[0])
                    class_name = model.names[class_id]
                    
                    # 가장 높은 신뢰도의 탐지 저장
                    if confidence > max_confidence:
                        max_confidence = confidence
                        x1, y1, x2, y2 = map(int, box.xyxy[0])
                        
                        # 탐지 정보 저장
                        best_detection = {
                            "class_name": class_name,
                            "confidence": confidence,
                            "box": [x1, y1, x2, y2]
                        }
                        best_frame = frame.copy()
        
        frame_count += 1
    
    cap.release()
    
    # 최고 신뢰도의 탐지 결과가 있으면 이미지 저장
    if best_detection and best_frame is not None:
        x1, y1, x2, y2 = best_detection["box"]
        cv2.rectangle(best_frame, (x1, y1), (x2, y2), (0, 255, 0), 2)
        cv2.putText(best_frame, f"{best_detection['class_name']} {best_detection['confidence']:.2f}", 
                   (x1, y1 - 10), cv2.FONT_HERSHEY_SIMPLEX, 0.5, (0, 255, 0), 2)
        
        # 결과 이미지 저장
        cv2.imwrite(result_image_path, best_frame)
        
        # 상대 경로로 변환
        relative_image_path = os.path.relpath(result_image_path, os.path.dirname(os.path.dirname(output_dir)))
        best_detection["image_path"] = "/" + relative_image_path.replace("\\", "/")
    
    # 결과 반환
    return {
        "detected_object": best_detection["class_name"] if best_detection else "None",
        "confidence": best_detection["confidence"] if best_detection else 0,
        "image_path": best_detection["image_path"] if best_detection else ""
    }

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Video analysis with YOLOv8")
    parser.add_argument("--video", required=True, help="Path to the video file")
    parser.add_argument("--model", required=True, help="Path to the YOLOv8 model file")
    parser.add_argument("--output", required=True, help="Output directory for results")
    
    args = parser.parse_args()
    
    result = process_video(args.video, args.model, args.output)
    
    # 결과를 JSON으로 출력 (Java에서 파싱할 수 있도록)
    print(json.dumps(result))