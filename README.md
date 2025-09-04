# 누구 (Noogoo)

### RoBERTa 모델을 활용한 보이스피싱 탐지 기반의 금융 사기 예방 애플리케이션

### 2025 모바일앱공모전 출품 프로젝트



## 팀원 소개 및 업무 분장

권관우 : 경북대학교 전자공학부모바일공학전공 24학번 | **와이어프레임 기획, 로그인, 홈, 프로필, 자연어 처리 모델 최적화, RAG**

김한서 : 경북대학교 전자공학부모바일공학전공 22학번 | **커뮤니티, 챗봇, 프로필 연동**

방승원 : 경북대학교 전자공학부모바일공학전공 22학번 | **실시간 뉴스, 전화번호 및 계좌번호 유효성 조회, 자연어 처리 모델 개발**

윤태준 : 경북대학교 전자공학부모바일공학전공 22학번 | **백엔드(머신러닝 모델, 파일 업로드)**

## 개발 환경 및 기술 스택

**Language**

<img src="https://img.shields.io/badge/kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=FFFFFF"> <img src="https://img.shields.io/badge/Python-3776AB?style=for-the-badge&logo=python&logoColor=FFFFFF">

**IDE**

<img src="https://img.shields.io/badge/Android%20Studio-3DDC84?style=for-the-badge&logo=androidstudio&logoColor=FFFFFF"> <img src="https://img.shields.io/badge/Visual%20Studio%20Code-0078d7.svg?style=for-the-badge&logo=visual-studio-code&logoColor=FFFFFF">

**Backend**

<img src="https://img.shields.io/badge/firebase-DD2C00?style=for-the-badge&logo=firebase&logoColor=FFFFFF"> <img src="https://img.shields.io/badge/gunicorn-%298729.svg?style=for-the-badge&logo=gunicorn&logoColor=FFFFFF"> <img src="https://img.shields.io/badge/flask-%23000.svg?style=for-the-badge&logo=flask&logoColor=FFFFFF"> <img src="https://img.shields.io/badge/Google%20Cloud-%234285F4.svg?style=for-the-badge&logo=google-cloud&logoColor=FFFFFF"> <img src="https://img.shields.io/badge/Naver%20CLOVA%20speech-03C75A?style=for-the-badge&logo=naver&logoColor=FFFFFF">

**Natural Language Processing**

<img src="https://img.shields.io/badge/PyTorch-%23EE4C2C.svg?style=for-the-badge&logo=PyTorch&logoColor=FFFFFF"> <img src="https://img.shields.io/badge/scikit--learn-%23F7931E.svg?style=for-the-badge&logo=scikit-learn&logoColor=FFFFFF"> <img src="https://img.shields.io/badge/pandas-%23150458.svg?style=for-the-badge&logo=pandas&logoColor=FFFFFF">

**Others**

<img src="https://img.shields.io/badge/Notion-%23000000.svg?style=for-the-badge&logo=notion&logoColor=FFFFFF"> <img src="https://img.shields.io/badge/figma-F24E1E?style=for-the-badge&logo=figma&logoColor=FFFFFF"> <img src="https://img.shields.io/badge/gemini-8E75B2?style=for-the-badge&logo=google%20gemini&logoColor=FFFFFF"> <img src="https://img.shields.io/badge/chatGPT-74aa9c?style=for-the-badge&logo=openai&logoColor=FFFFFF">

## 프로젝트 소개

### 서비스 기능

+ **통화 녹음 기반 보이스피싱 가능성 제시**
    + 통화 녹음 파일을 업로드하면 RoBERTa 모델이 보이스피싱 위험도를 측정하여 제공

+ **전화번호, 계좌번호 유효성 조회**
    + html 파싱을 통해 정부 공식 사이트에서 전화번호, 계좌번호 유효성 조회 후 결과 제공

+ **RAG를 적용한 AI 챗봇**
    + 문서를 학습한 Gemini 기반 챗봇

+ **보이스피싱 경험 공유 커뮤니티**
    + Firebase로 구현한 커뮤니티
 
### 와이어프레임

<img src="https://github.com/bbang427/QuadCore/blob/master/readmepic/noogoo_wireframe_landing1.png" width="200" height="400"> <img src="https://github.com/bbang427/QuadCore/blob/master/readmepic/noogoo_wireframe_landing2.png" width="200" height="400"> <img src="https://github.com/bbang427/QuadCore/blob/master/readmepic/noogoo_wireframe_landing3.png" width="200" height="400"> <img src="https://github.com/bbang427/QuadCore/blob/master/readmepic/noogoo_wireframe_landing4.png" width="200" height="400"> <img src="https://github.com/bbang427/QuadCore/blob/master/readmepic/noogoo_wireframe_landing5.png" width="200" height="400"> <img src="https://github.com/bbang427/QuadCore/blob/master/readmepic/noogoo_wireframe_landing6.png" width="200" height="400">

<img src="https://github.com/bbang427/QuadCore/blob/master/readmepic/noogoo_wireframe_home.png" width="200" height="400"> <img src="https://github.com/bbang427/QuadCore/blob/master/readmepic/noogoo_wirefrme_uploadfile.png" width="200" height="400"> <img src="https://github.com/bbang427/QuadCore/blob/master/readmepic/noogoo_wireframe_searchaccountnum.png" width="200" height="400"> <img src="https://github.com/bbang427/QuadCore/blob/master/readmepic/noogoo_wireframe_searchphonenum.png" width="200" height="400"> <img src="https://github.com/bbang427/QuadCore/blob/master/readmepic/noogoo_wireframe_reportnum.png" width="200" height="400"> <img src="https://github.com/bbang427/QuadCore/blob/master/readmepic/noogoo_wireframe_profile.png">


