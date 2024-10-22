# AS IP Camera Streaming

Android 기기를 IP 카메라로 변환하여 실시간으로 카메라 영상을 스트리밍하고, 눈 깜박임 감지를 통해 경고를 전송하는 앱입니다.

## 데모

![앱 스크린샷](./screenshot.png)

## 주요 기능

- **실시간 카메라 스트리밍**: Android 기기의 카메라 영상을 웹 브라우저를 통해 실시간으로 볼 수 있습니다.
- **눈 깜박임 감지**: OpenCV와 MediaPipe를 활용하여 사용자의 눈 깜박임을 감지합니다.
- **경고 알림 및 진동**: 눈을 감고 있을 때 경고(진동)를 보내고, Android 기기에 진동을 울립니다.
- **전/후면 카메라 전환**: 버튼을 통해 전면 및 후면 카메라를 전환할 수 있습니다.

## 설치 방법

### Android 앱 설치

1. **Android Studio**를 설치합니다.
2. 이 저장소를 클론하거나 다운로드합니다.
3. Android Studio에서 프로젝트를 엽니다.
4. 필요한 의존성을 동기화하고 앱을 빌드합니다.
5. 실제 기기 또는 에뮬레이터에서 앱을 실행합니다.

### Python 스크립트 실행

1. **Python 3.x**를 설치합니다.
2. 필요한 패키지를 설치합니다.
3. `eye_detection.py` 스크립트의 IP 주소를 실제 환경에 맞게 수정합니다.
  (https://github.com/gjaischool/Final-Project-No/blob/main/eye_detection.py) 
4. 스크립트를 실행합니다.


## 사용 방법

1. **Android 앱을 실행**하고 필요한 권한을 허용합니다.
2. 앱 화면 상단에 표시된 **IP 주소**를 확인합니다.
3. **Python 스크립트**의 `phone_ip` 및 `ip_camera_url` 변수를 해당 IP 주소로 설정합니다.
4. Python 스크립트를 실행하여 **눈 깜박임 감지**를 시작합니다.
5. 웹 브라우저에서 `http://휴대폰_IP주소:8080/`으로 접속하여 **카메라 영상을 확인**합니다.
6. 눈을 감으면 앱에서 **진동 알림**이 울립니다.

## 기술 스택

- **Android**
- Kotlin
- CameraX
- NanoHTTPD
- **Python**
- OpenCV
- MediaPipe
- NumPy
- Requests

## 연락처

- **이메일**: noojoonoo@naver.com
- **GitHub**: [gjaischool](https://github.com/gjaischool)

