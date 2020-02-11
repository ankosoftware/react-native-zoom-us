package ch.milosz.rnzoomus;

import android.util.Log;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.LifecycleEventListener;

import us.zoom.sdk.ZoomSDK;
import us.zoom.sdk.ZoomError;
import us.zoom.sdk.ZoomSDKInitParams;
import us.zoom.sdk.ZoomSDKInitializeListener;
import us.zoom.sdk.ZoomSDKRawDataMemoryMode;

import us.zoom.sdk.MeetingStatus;
import us.zoom.sdk.MeetingError;
import us.zoom.sdk.MeetingService;
import us.zoom.sdk.MeetingServiceListener;

import us.zoom.sdk.StartMeetingOptions;
import us.zoom.sdk.StartMeetingParamsWithoutLogin;

import us.zoom.sdk.JoinMeetingOptions;
import us.zoom.sdk.JoinMeetingParams;

public class RNZoomUsModule extends ReactContextBaseJavaModule
    implements ZoomSDKInitializeListener, MeetingServiceListener, LifecycleEventListener {

  private final static String TAG = "RNZoomUs";
  private final ReactApplicationContext reactContext;

  private Promise initializePromise;
  private Promise meetingPromise;

  public RNZoomUsModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;
    reactContext.addLifecycleEventListener(this);
  }

  @Override
  public String getName() {
    return "RNZoomUs";
  }

  @Override
  public boolean canOverrideExistingModule() {
    return true;
  }

  @ReactMethod
  public void initialize(final String appKey, final String appSecret, final String webDomain, final Promise promise) {
    ZoomSDK zoomSDK = ZoomSDK.getInstance();
    if (zoomSDK.isInitialized()) {
      promise.resolve("Already initialize Zoom SDK successfully.");
      return;
    }

    try {
      initializePromise = promise;

      reactContext.getCurrentActivity().runOnUiThread(new Runnable() {
        @Override
        public void run() {
          ZoomSDKInitParams initParams = new ZoomSDKInitParams();
          initParams.appKey = appKey;
          initParams.appSecret = appSecret;
          initParams.domain = webDomain;
          initParams.autoRetryVerifyApp = true;
          initParams.enableLog = true;
          initParams.videoRawDataMemoryMode = ZoomSDKRawDataMemoryMode.ZoomSDKRawDataMemoryModeStack;
          ZoomSDK.getInstance().initialize(reactContext.getCurrentActivity(), RNZoomUsModule.this, initParams);
        }
      });
    } catch (Exception ex) {
      promise.reject("ERR_UNEXPECTED_EXCEPTION", ex);
    }
  }

  @ReactMethod
  public void startMeeting(final String displayName, final String meetingNo, final String userId, final String zac,
      Promise promise) {
    try {
      meetingPromise = promise;

      ZoomSDK zoomSDK = ZoomSDK.getInstance();
      if (!zoomSDK.isInitialized()) {
        promise.reject("ERR_ZOOM_START", "ZoomSDK has not been initialized successfully");
        return;
      }

      final MeetingService meetingService = zoomSDK.getMeetingService();
      if (meetingService.getMeetingStatus() != MeetingStatus.MEETING_STATUS_IDLE) {
        long lMeetingNo = 0;
        try {
          lMeetingNo = Long.parseLong(meetingNo);
        } catch (NumberFormatException e) {
          promise.reject("ERR_ZOOM_START", "Invalid meeting number: " + meetingNo);
          return;
        }

        if (meetingService.getCurrentRtcMeetingNumber() == lMeetingNo) {
          meetingService.returnToMeeting(reactContext.getCurrentActivity());
          promise.resolve("Already joined zoom meeting");
          return;
        }
      }
      zoomSDK.getMeetingSettingsHelper().disableChatUI(true);
      zoomSDK.getMeetingSettingsHelper().setAutoConnectVoIPWhenJoinMeeting(true);
      StartMeetingOptions opts = new StartMeetingOptions();
      opts.no_dial_in_via_phone = true;
      opts.no_dial_out_to_phone = true;
      opts.no_disconnect_audio = true;
      opts.no_invite = true;
      opts.no_share = true;
      opts.no_unmute_confirm_dialog = true;
      opts.no_webinar_register_dialog = true;
      StartMeetingParamsWithoutLogin params = new StartMeetingParamsWithoutLogin();
      params.displayName = displayName;
      params.meetingNo = meetingNo;
      params.userId = userId;
      params.userType = MeetingService.USER_TYPE_API_USER;
      params.zoomAccessToken = zac;
      params.zoomToken = "null";

      int startMeetingResult = meetingService.startMeetingWithParams(reactContext.getCurrentActivity(), params, opts);
      Log.i(TAG, "startMeeting, startMeetingResult=" + startMeetingResult);

      if (startMeetingResult != MeetingError.MEETING_ERROR_SUCCESS) {
        promise.reject("ERR_ZOOM_START", "startMeeting, errorCode=" + startMeetingResult);
      }
    } catch (Exception ex) {
      promise.reject("ERR_UNEXPECTED_EXCEPTION", ex);
    }
  }

  @ReactMethod
  public void joinMeeting(final String displayName, final String meetingNo, Promise promise) {
    try {
      meetingPromise = promise;

      ZoomSDK zoomSDK = ZoomSDK.getInstance();
      if (!zoomSDK.isInitialized()) {
        promise.reject("ERR_ZOOM_JOIN", "ZoomSDK has not been initialized successfully");
        return;
      }

      final MeetingService meetingService = zoomSDK.getMeetingService();

      JoinMeetingOptions opts = new JoinMeetingOptions();
      JoinMeetingParams params = new JoinMeetingParams();
      params.displayName = displayName;
      params.meetingNo = meetingNo;

      int joinMeetingResult = meetingService.joinMeetingWithParams(reactContext.getCurrentActivity(), params, opts);
      Log.i(TAG, "joinMeeting, joinMeetingResult=" + joinMeetingResult);

      if (joinMeetingResult != MeetingError.MEETING_ERROR_SUCCESS) {
        promise.reject("ERR_ZOOM_JOIN", "joinMeeting, errorCode=" + joinMeetingResult);
      }
    } catch (Exception ex) {
      promise.reject("ERR_UNEXPECTED_EXCEPTION", ex);
    }
  }

  @Override
  public void onZoomSDKInitializeResult(int errorCode, int internalErrorCode) {
    Log.i(TAG, "onZoomSDKInitializeResult, errorCode=" + errorCode + ", internalErrorCode=" + internalErrorCode);
    if (errorCode != ZoomError.ZOOM_ERROR_SUCCESS) {
      initializePromise.reject("ERR_ZOOM_INITIALIZATION",
          "Error: " + errorCode + ", internalErrorCode=" + internalErrorCode);
    } else {
      registerListener();
      initializePromise.resolve("Initialize Zoom SDK successfully.");
    }
  }

  @Override
  public void onMeetingStatusChanged(MeetingStatus meetingStatus, int errorCode, int internalErrorCode) {
    Log.i(TAG, "onMeetingStatusChanged, meetingStatus=" + meetingStatus + ", errorCode=" + errorCode + ", Message="
        + internalErrorCode);

    if (meetingPromise == null) {
      return;
    }

    if (meetingStatus == MeetingStatus.MEETING_STATUS_FAILED) {
      meetingPromise.reject("ERR_ZOOM_MEETING", "Error: " + errorCode + ", Message=" + internalErrorCode);
      meetingPromise = null;
    } else if (meetingStatus == MeetingStatus.MEETING_STATUS_INMEETING) {
      meetingPromise.resolve("Connected to zoom meeting");
      meetingPromise = null;
    }
  }

  private void registerListener() {
    Log.i(TAG, "registerListener");
    ZoomSDK zoomSDK = ZoomSDK.getInstance();
    MeetingService meetingService = zoomSDK.getMeetingService();
    if (meetingService != null) {
      meetingService.addListener(this);
    }
  }

  private void unregisterListener() {
    Log.i(TAG, "unregisterListener");
    ZoomSDK zoomSDK = ZoomSDK.getInstance();
    if (zoomSDK.isInitialized()) {
      MeetingService meetingService = zoomSDK.getMeetingService();
      meetingService.removeListener(this);
    }
  }

  @Override
  public void onCatalystInstanceDestroy() {
    unregisterListener();
  }

  // React LifeCycle
  @Override
  public void onHostDestroy() {
    unregisterListener();
  }

  @Override
  public void onHostPause() {
  }

  @Override
  public void onHostResume() {
  }

  @Override
  public void onZoomAuthIdentityExpired() {
    Log.d(TAG, "onZoomAuthIdentityExpired:");
  }
}
