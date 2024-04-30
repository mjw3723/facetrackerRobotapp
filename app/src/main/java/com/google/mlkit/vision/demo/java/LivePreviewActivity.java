/*
 * Copyright 2020 Google LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.mlkit.vision.demo.java;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.common.annotation.KeepName;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.demo.CameraSource;
import com.google.mlkit.vision.demo.CameraSourcePreview;
import com.google.mlkit.vision.demo.GraphicOverlay;
import com.google.mlkit.vision.demo.R;

import com.google.mlkit.vision.demo.java.facedetector.FaceDetectorProcessor;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Live preview demo for ML Kit APIs. */
@KeepName
public class LivePreviewActivity extends AppCompatActivity
    implements OnItemSelectedListener, CompoundButton.OnCheckedChangeListener,TextToSpeech.OnInitListener, FaceDetectionCallback {
  private VisionProcessorBase visionProcessorBase;
  /////////////////음성 출력 입력 ////////////////////////
  private TextToSpeech tts; //음성 출력용
  Context cThis;//context 설정
  String LogTT="[STT]";//LOG타이틀
  //음성 인식용
  Intent SttIntent;
  SpeechRecognizer mRecognizer;

  /////////////////얼굴 인식 ////////////////////////
  private static final String FACE_DETECTION = "Face Detection";

  private static final String TAG = "LivePreviewActivity";

  private CameraSource cameraSource = null;
  private CameraSourcePreview preview;
  private GraphicOverlay graphicOverlay;
  private String selectedModel;
  private boolean face;

  ///////////////////블루 투스 /////
  private static final int REQUEST_ENABLE_BT = 10; // 블루투스 활성화 상태
  private BluetoothAdapter bluetoothAdapter; // 블루투스 어댑터
  private Set<BluetoothDevice> devices; // 블루투스 디바이스 데이터 셋
  private BluetoothDevice bluetoothDevice; // 블루투스 디바이스
  private BluetoothSocket bluetoothSocket = null; // 블루투스 소켓
  private OutputStream outputStream = null;  // 블루투스에 데이터를 출력하기 위한 출력 스트림
  private InputStream inputStream = null; // 블루투스에 데이터를 입력하기 위한 입력 스트림
  private Thread workerThread = null; // 문자열 수신에 사용되는 쓰레드
  private byte[] readBuffer; // 수신 된 문자열을 저장하기 위한 버퍼
  private int readBufferPosition; // 버퍼 내 문자 저장 위치
  int pariedDeviceCount;

  Map<String,BluetoothSocket> mapping = new HashMap<>();
  //메인 이미지 태그
  public static ImageView faceview;

  /////////////////// 크롤링 결과 출력 메서드//////////////////////

  private final Handler mHandler = new Handler(Looper.getMainLooper()) {
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void handleMessage(Message msg) {
      String result = (String) msg.obj;
      speakOut(result);
    }
  };

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Log.d(TAG, "onCreate");
    
    setContentView(R.layout.activity_vision_live_preview);
    ///////////////////////////////얼굴인식/////////////////////////////////
    preview = findViewById(R.id.preview_view);
    if (preview == null) {
      Log.d(TAG, "Preview is null");
    }
    graphicOverlay = findViewById(R.id.graphic_overlay);
    if (graphicOverlay == null) {
      Log.d(TAG, "graphicOverlay is null");
    }
/////////////////////////////////////////////////////////////////////////////////////////////////////
    Spinner spinner = findViewById(R.id.spinner);
    List<String> options = new ArrayList<>();
    options.add(FACE_DETECTION);

    // Creating adapter for spinner
    ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(this, R.layout.spinner_style, options);
    // Drop down layout style - list view with radio button
    dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    // attaching data adapter to spinner
    spinner.setAdapter(dataAdapter);
    spinner.setOnItemSelectedListener(this);
    createCameraSource(selectedModel);

    SetTTS();
  }
  public void SetTTS(){
    tts = new TextToSpeech(this, this);
    cThis=this;
    SttIntent=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
    SttIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,getApplicationContext().getPackageName());
    SttIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"ko-KR");//한국어 사용
    mRecognizer=SpeechRecognizer.createSpeechRecognizer(cThis);
    mRecognizer.setRecognitionListener(listener);
    tts=new TextToSpeech(cThis, new TextToSpeech.OnInitListener() {
      @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
      @Override
      public void onInit(int status) {
        if(status!=android.speech.tts.TextToSpeech.ERROR){
          tts.setLanguage(Locale.KOREAN);
          Voice voice = new Voice("ko-kr-x-gender-male", Locale.KOREAN, Voice.QUALITY_VERY_HIGH, Voice.LATENCY_NORMAL, false, null);
          // Voice 객체를 설정합니다.
          tts.setVoice(voice);
          speakOut("메카돌이");
        }
      }
    });
  }


  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  @Override
  public synchronized void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
    // An item was selected. You can retrieve the selected item using
    // parent.getItemAtPosition(pos)
    selectedModel = parent.getItemAtPosition(0).toString();
    Log.d(TAG, "Selected model: " + selectedModel);
    preview.stop();
    createCameraSource("Face Detection");
    startCameraSource();
  }

  @Override
  public void onNothingSelected(AdapterView<?> parent) {}

  @Override
  public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
    Log.d(TAG, "Set facing");
    if (cameraSource != null) {
      if (isChecked) {
        cameraSource.setFacing(CameraSource.CAMERA_FACING_FRONT);
      } else {
        cameraSource.setFacing(CameraSource.CAMERA_FACING_BACK);
      }
    }
    preview.stop();
    startCameraSource();
  }

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  public void createCameraSource(String model) {
    // If there's no existing cameraSource, create one.
    if (cameraSource == null) {
      cameraSource = new CameraSource(this, graphicOverlay);
    }

    try {
      switch (model) {
        case FACE_DETECTION:
          Log.i(TAG, "Using Face Detector Processor");
          cameraSource.setFacing(CameraSource.CAMERA_FACING_FRONT);
          cameraSource.setMachineLearningFrameProcessor(new FaceDetectorProcessor(this,this,this));
          break;
        default:
          Log.e(TAG, "Unknown model: " + model);
      }
    } catch (RuntimeException e) {

    }
  }

  /**
   * Starts or restarts the camera source, if it exists. If the camera source doesn't exist yet
   * (e.g., because onResume was called before the camera source was created), this will be called
   * again when the camera source is created.
   */
  public void startCameraSource() {
    if (cameraSource != null) {
      try {
        if (preview == null) {
          Log.d(TAG, "resume: Preview is null");
        }
        if (graphicOverlay == null) {
          Log.d(TAG, "resume: graphOverlay is null");
        }
        preview.start(cameraSource, graphicOverlay);
      } catch (IOException e) {
        Log.e(TAG, "Unable to start camera source.", e);
        cameraSource.release();
        cameraSource = null;
      }
    }
  }

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  @Override
  public void onResume() {
    super.onResume();
    Log.d(TAG, "onResume");
    createCameraSource(selectedModel);
    startCameraSource();
  }

  /** Stops the camera. */
  @Override
  protected void onPause() {
    super.onPause();
    preview.stop();
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    if (cameraSource != null) {
      cameraSource.release();
    }
  }
  //////////////////////////////얼굴인식 call back
  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  @Override
  public void onFaceDetected(boolean faceDetected) {
    Log.d("얼굴인식",String.valueOf(faceDetected));
    faceview = findViewById(R.id.imageView3);
    if (faceDetected) {
      face = true;
      if(LivePreviewActivity.faceview ==null){
        Log.d("zz","null");
      }else{
        faceview.setImageResource(R.drawable.smile);
      }
      startSpeechRecognition();
    } else {
      face = false;
      faceview.setImageResource(R.drawable.face1);
    }
  }
  ////////////////////////얼굴 인식 후 TTS 실행
  private void startSpeechRecognition() {
    System.out.println("음성인식 시작!");
    if (ContextCompat.checkSelfPermission(cThis, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
      ActivityCompat.requestPermissions(LivePreviewActivity.this, new String[]{Manifest.permission.RECORD_AUDIO}, 1);
      // 권한을 허용하지 않는 경우
    } else {
      // 권한을 허용한 경우
      try {
        mRecognizer.startListening(SttIntent);
      } catch (SecurityException e) {
        e.printStackTrace();
      }
    }
  }
  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  @Override
  public void onInit(int status) { // OnInitListener를 통해서 TTS 초기화
    if(status == TextToSpeech.SUCCESS){
      int result = tts.setLanguage(Locale.KOREA); // TTS언어 한국어로 설정

      if(result == TextToSpeech.LANG_NOT_SUPPORTED || result == TextToSpeech.LANG_MISSING_DATA){
        Log.e("TTS", "This Language is not supported");
      }else{
        Log.e("TTS", "음성인식");
      }
    }else{
      Log.e("TTS", "Initialization Failed!");
    }
  }
  /////////////음성 출력 메서드 ////////////////
  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  public void speakOut(CharSequence text){

    tts.setPitch((float)0.5); // 음성 톤 높이 지정
    tts.setSpeechRate((float)1.0); // 음성 속도 지정
    Voice voice = new Voice("ko-KR-Standard-C", Locale.KOREAN, Voice.QUALITY_VERY_HIGH, Voice.LATENCY_NORMAL, false, null);

    // Voice 객체를 설정합니다.
    tts.setVoice(voice);
    // 첫 번째 매개변수: 음성 출력을 할 텍스트
    // 두 번째 매개변수: 1. TextToSpeech.QUEUE_FLUSH - 진행중인 음성 출력을 끊고 이번 TTS의 음성 출력
    //                 2. TextToSpeech.QUEUE_ADD - 진행중인 음성 출력이 끝난 후에 이번 TTS의 음성 출력
    tts.speak(text, TextToSpeech.QUEUE_ADD, null, "id1");
  }
  private RecognitionListener listener = new RecognitionListener() {
    //음성인식 시작 부분
    @Override
    public void onReadyForSpeech(Bundle bundle) {}
    @Override
    public void onBeginningOfSpeech() {
      //txtSystem.setText("지금부터 말을 해주세요..........."+"\r\n"+txtSystem.getText());
    }
    @Override
    public void onRmsChanged(float v) {}
    @Override
    public void onBufferReceived(byte[] bytes) {}
    @Override
    public void onEndOfSpeech() {}
    @Override
    public void onError(int i) {}
    /////////////////////////////음성인식 결과 텍스트 추출
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onResults(Bundle results) {
      String key= "";
      key = SpeechRecognizer.RESULTS_RECOGNITION;
      ArrayList<String> mResult =results.getStringArrayList(key);
      String[] rs = new String[mResult.size()];
      mResult.toArray(rs);
      FuncVoiceOrderCheck(rs[0]);
      mRecognizer.stopListening(); //말하고 난 후 마이크 정지
    }

    @Override
    public void onPartialResults(Bundle bundle) {}
    @Override
    public void onEvent(int i, Bundle bundle) {}
  };
  /////////////////////////////입력된 음성 메세지 확인 후 음성 출력 부분
  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  private void FuncVoiceOrderCheck(String VoiceMsg){
    if(VoiceMsg.length()<1)return;

    VoiceMsg=VoiceMsg.replace(" ","");//공백제거

    if(VoiceMsg.indexOf("메카트로닉스")>-1 || VoiceMsg.indexOf("메카트로닉스")>-1){
      speakOut("미래를 향한 도전의 발판, 경성대 메카트로닉스공학과에 여러분을 초대합니다. 메카트로닉스공학과는 기계공학, 전기전자공학, 컴퓨터공학의 융합을 기반으로 현대 사회의 다양한 분야에서 기술적 도전에 대처할 수 있는 창의적이고 전문적인 역량을 함양하는데 초점을 맞추고 있습니다.");
    }
    if(VoiceMsg.indexOf("조동현")>-1 || VoiceMsg.indexOf("조동현")>-1){
      speakOut("조동현은 경성대 메카트로닉스 19학번 인싸입니다.");
    }
    if(VoiceMsg.indexOf("블루투스")>-1 || VoiceMsg.indexOf("블루투스")>-1){
      setBluetooth();
      speakOut("연결할 블루투스를 선택해주세요.");
    }
    if(VoiceMsg.indexOf("하이")>-1 || VoiceMsg.indexOf("하이")>-1) {
      sendData("hi");
    }
    if(VoiceMsg.indexOf("라이더")>-1 || VoiceMsg.indexOf("라이더")>-1) {
      try {
        sendDataToSocket(mapping.get("LiDAR"), "hi");
      }catch (Exception e){
        e.printStackTrace();
      }
    }
    if(VoiceMsg.indexOf("오른쪽팔")>-1 || VoiceMsg.indexOf("왼쪽팔")>-1) {
      try {
        sendDataToSocket(mapping.get(" Ridar"), "안녕");
      }catch (Exception e){
        e.printStackTrace();
      }
    }
    if(VoiceMsg.indexOf("월학사일정")>-1 || VoiceMsg.indexOf("월 학사일정")>-1) {
      String month = VoiceMsg.substring(0,2);
      if(month.substring(1,2).equals("월")) {
        month = month.substring(0,1);
      }else{
        month = month.substring(0,2);
      }
      KsHaksa jsoupThread = new KsHaksa(month, mHandler);
      jsoupThread.start();
    }

  }

  @SuppressLint("MissingPermission")
  private void setBluetooth(){
    String[] permission_list;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      permission_list = new String[]{
              android.Manifest.permission.ACCESS_FINE_LOCATION,
              android.Manifest.permission.ACCESS_COARSE_LOCATION,
              android.Manifest.permission.BLUETOOTH_SCAN,
              android.Manifest.permission.BLUETOOTH_ADMIN,
              android.Manifest.permission.BLUETOOTH_ADVERTISE,
              android.Manifest.permission.BLUETOOTH_CONNECT,
      };
    } else {
      permission_list = new String[]{
              android.Manifest.permission.ACCESS_FINE_LOCATION,
              android.Manifest.permission.ACCESS_COARSE_LOCATION
      };
    }

    ActivityCompat.requestPermissions(LivePreviewActivity.this, permission_list, 1);
    bluetoothAdapter = BluetoothAdapter.getDefaultAdapter(); // 블루투스 어댑터를 디폴트 어댑터로 설정
    if(bluetoothAdapter == null) { // 디바이스가 블루투스를 지원하지 않을 때

    }else { // 디바이스가 블루투스를 지원 할 때
      if(bluetoothAdapter.isEnabled()) { // 블루투스가 활성화 상태 (기기에 블루투스가 켜져있음)
        selectBluetoothDevices(); // 블루투스 디바이스 선택 함수 호출
      }
      else { // 블루투스가 비 활성화 상태 (기기에 블루투스가 꺼져있음)
        // 블루투스를 활성화 하기 위한 다이얼로그 출력
        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        // 선택한 값이 onActivityResult 함수에서 콜백된다.
        startActivityForResult(intent, REQUEST_ENABLE_BT);
      }
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    switch (requestCode) {
      case REQUEST_ENABLE_BT :
        if(requestCode == RESULT_OK) { // '사용'을 눌렀을 때
          selectBluetoothDevices(); // 블루투스 디바이스 선택 함수 호출
        }
        break;
    }

  }

  @SuppressLint("MissingPermission")
  public void selectBluetoothDevices() {
    // 이미 페어링 되어있는 블루투스 기기를 찾습니다.
    devices = bluetoothAdapter.getBondedDevices();

    // 페어링 된 디바이스의 크기를 저장
    int pairedDeviceCount = devices.size();

    // 페어링 되어있는 장치가 없는 경우
    if (pairedDeviceCount == 0) {
      // 페어링을 하기위한 함수 호출
    } else {
      // 다이얼로그에 보여줄 디바이스 목록과 선택 여부를 저장하는 배열
      final boolean[] checkedDevices = new boolean[pairedDeviceCount];
      final BluetoothDevice[] selectedDevices = new BluetoothDevice[pairedDeviceCount];
      final String[] deviceNames = new String[pairedDeviceCount];
      int i = 0;

      // 디바이스 목록과 선택 여부 초기화
      for (BluetoothDevice bluetoothDevice : devices) {
        deviceNames[i] = bluetoothDevice.getName();
        checkedDevices[i] = false;
        selectedDevices[i] = bluetoothDevice;
        i++;
      }

      // 디바이스를 선택하기 위한 다이얼로그 생성
      AlertDialog.Builder builder = new AlertDialog.Builder(this);
      builder.setTitle("페어링 되어있는 블루투스 디바이스 목록");

      // 다이얼로그에 보여줄 체크 박스 목록 설정
      builder.setMultiChoiceItems(deviceNames, checkedDevices, new DialogInterface.OnMultiChoiceClickListener() {
        @Override
        public void onClick(DialogInterface dialogInterface, int i, boolean isChecked) {
          // 사용자가 체크 박스를 선택했을 때
          checkedDevices[i] = isChecked;
        }
      });

      // "확인" 버튼을 눌렀을 때 처리
      builder.setPositiveButton("확인", new DialogInterface.OnClickListener() {
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
          // 선택된 디바이스들에 대한 처리 수행
          List<BluetoothDevice> selectedDevicesList = new ArrayList<>();
          for (int j = 0; j < checkedDevices.length; j++) {
            if (checkedDevices[j]) {
              selectedDevicesList.add(selectedDevices[j]);
            }
          }
          // 선택된 장치들에 연결
          connectToSelectedDevices(selectedDevicesList);
        }
      });

      // "취소" 버튼을 눌렀을 때 처리
      builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
          // 다이얼로그를 닫기만 하고 아무런 처리를 하지 않음

        }
      });

      // 다이얼로그 생성
      AlertDialog alertDialog = builder.create();
      alertDialog.show();
    }
  }
  @SuppressLint("MissingPermission")
  List<BluetoothSocket> bluetoothSockets = new ArrayList<>(); //연결할 블루투스들

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  private void connectToSelectedDevices(List<BluetoothDevice> selectedDevices) {
    for (BluetoothDevice device : selectedDevices) {
      connectToDevice(device);
    }
  }

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  @SuppressLint("MissingPermission")
  private void connectToDevice(BluetoothDevice device) {
    UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    BluetoothSocket socket;
    try {
      socket = device.createRfcommSocketToServiceRecord(uuid);
      socket.connect();
      mapping.put(device.getName(),socket);
      bluetoothSockets.add(socket);
      // BluetoothSocket에서 inputStream 초기화
      InputStream tmpIn = null;
      tmpIn = socket.getInputStream();
      inputStream = new BufferedInputStream(tmpIn);
      speakOut(device.getName()+"연결완료"); //안드로이드 송신 완료음
      sendDataToSocket(socket,device.getName()+"와 안드로이드 연결완료"); //연결완료되었다고 블루투스 송신
      receiveData(socket); // 데이터 수신 시작
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
 /////////////////블루투스 기기에서 받아온 데이터 수신 메서드
  public void receiveData(final BluetoothSocket socket) {
    final Handler handler = new Handler();

    Thread workerThread = new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          InputStream inputStream = socket.getInputStream(); // 소켓 별도로 가져오기
          while (true) { // 무한 루프로 변경하여 계속해서 데이터를 수신하도록 함
            byte[] buffer = new byte[1024];
            int bytes;

            // 입력 스트림에서 데이터를 읽어옴
            bytes = inputStream.read(buffer);

            if (bytes != -1) { // 데이터가 유효한 경우
              final String text = new String(buffer, 0, bytes, "UTF-8");
              handler.post(new Runnable() {
                @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                @Override
                public void run() {
                  String replace_text = text.replace(" ","");
                  Log.d("Bluetooth 수신", "Received text: " +replace_text);
                  final String tt = "안녕하세요"; // 예시로 "rider1"로 설정
                  if(replace_text.trim().contains(tt.trim())){
                    sendDataToSocket(socket,"네안녕하세요.");
                  }
                  speakOut(text);

                }
              });
            }
          }
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    });
    workerThread.start();
  }
  ///////////////////////모든 소켓 데이터 송신
  private void sendData(String data) {     //모두에게 데이터 송신
    data = data+"\n";
    for (BluetoothSocket socket : bluetoothSockets) {
      try {
        OutputStream outputStream = socket.getOutputStream();
        outputStream.flush();
        outputStream.write(data.getBytes());
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }
  ///////////////////////특정 소켓에만 데이터 송신
  private void sendDataToSocket(BluetoothSocket socket, String data) {
    data = data + "\n";
    try {
      OutputStream outputStream = socket.getOutputStream();
      outputStream.flush();
      outputStream.write(data.getBytes());
    } catch (IOException e) {
      e.printStackTrace();
    }
  }


}
