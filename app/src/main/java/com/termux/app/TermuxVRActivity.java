package com.termux.app;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Vibrator;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.google.vr.sdk.base.AndroidCompat;
import com.google.vr.sdk.base.Eye;
import com.google.vr.sdk.base.GvrActivity;
import com.google.vr.sdk.base.GvrView;
import com.google.vr.sdk.base.HeadTransform;
import com.google.vr.sdk.base.Viewport;
import com.termux.R;
import com.termux.opengl.Mesh;
import com.termux.opengl.MeshData;
import com.termux.opengl.OpenGLUtils;
import com.termux.opengl.ShaderUtils;
import com.termux.terminal.TerminalSession;
import com.termux.terminal.TextStyle;
import com.termux.view.TerminalView;

import javax.microedition.khronos.egl.EGLConfig;

public final class TermuxVRActivity extends GvrActivity implements GvrView.StereoRenderer, ServiceConnection {
    static final String TAG = "TermuxVrActivity";
    private static final int MAX_SESSIONS = 8;

    private static final int FLOOR_DEPTH = 20;

    private static final int SCREEN_WIDTH = 640;
    private static final int SCREEN_HEIGHT = 480;
    private static final int SCREEN_DISTANCE = -10;

    private static final float Z_NEAR = 0.1f;
    private static final float Z_FAR = 100.0f;

    private static final float CAMERA_Z = 0.01f;

    // We keep the light always position just above the user.
    private static final float[] LIGHT_POS_IN_WORLD_SPACE = new float[] {0.0f, 2.0f, 0.0f, 1.0f};

    private final float[] mLightPosInEyeSpace = new float[4];

    private float[] mCamera;
    private float[] mView;

    Vibrator mVibrator;

    private Mesh mFloor;
    private Mesh mScreen;

    TermuxPreferences mSettings;
    TermuxService mTermuxService;
    TerminalView mTerminalView;

    int mBackgroundColor = Color.BLACK;
    Bitmap mRenderTarget = Bitmap.createBitmap(SCREEN_WIDTH, SCREEN_HEIGHT, Bitmap.Config.ARGB_8888);

    final SoundPool mBellSoundPool = new SoundPool.Builder().setMaxStreams(1).setAudioAttributes(
        new AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION).build()).build();
    int mBellSoundId;

    boolean mScheduleRedraw;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initializeGvrView();

        mCamera = new float[16];
        mView = new float[16];

        mSettings = new TermuxPreferences(this);
        mBellSoundId = mBellSoundPool.load(this, R.raw.bell, 1);

        mTerminalView = findViewById(R.id.terminal_view);
        mTerminalView.setTextSize(mSettings.getFontSize());
        mTerminalView.setOnKeyListener(new TermuxVRViewClient(this));
        mTerminalView.requestFocus();

        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        mFloor = new Mesh();
        mScreen = new Mesh();

        mScheduleRedraw = false;

        Intent serviceIntent = new Intent(this, TermuxService.class);
        startService(serviceIntent);
        if (!bindService(serviceIntent, this, 0)) {
            throw new RuntimeException("bindService() failed");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mTerminalView.onScreenUpdated();
        mScheduleRedraw = true;
    }

    public void initializeGvrView() {
        setContentView(R.layout.vr_layout);

        GvrView gvrView = findViewById(R.id.gvr_view);
        gvrView.setEGLConfigChooser(8, 8, 8, 8, 16, 8);

        gvrView.setRenderer(this);
        gvrView.setTransitionViewEnabled(true);

        // A bit of a hack, as these overlay buttons can be focused by hardware keyboard and mess everything up
        findViewById(R.id.ui_back_button).setFocusable(false);
        findViewById(R.id.ui_settings_button).setFocusable(false);

        if (gvrView.setAsyncReprojectionEnabled(true)) {
            // Async reprojection decouples the app framerate from the display framerate,
            // allowing immersive interaction even at the throttled clockrates set by
            // sustained performance mode.
            AndroidCompat.setSustainedPerformanceMode(this, true);
        }

        setGvrView(gvrView);
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder binder) {
        mTermuxService = ((TermuxService.LocalBinder) binder).service;
        Log.d(TAG, "Service connected");

        mTermuxService.mSessionChangeCallback = new TerminalSession.SessionChangedCallback() {
            @Override
            public void onTextChanged(TerminalSession changedSession) {
                if (getCurrentTermSession() != changedSession) {
                    return;
                }
                mTerminalView.onScreenUpdated();
                mScheduleRedraw = true;
            }

            @Override
            public void onTitleChanged(TerminalSession updatedSession) {
                // FIXME: We don't care just yet
            }

            @Override
            public void onSessionFinished(final TerminalSession finishedSession) {
                if (mTermuxService.mWantsToStop) {
                    // The service wants to stop as soon as possible.
                    finish();
                }
            }

            @Override
            public void onClipboardText(TerminalSession session, String text) {
                // Just try not to break something useful
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                clipboard.setPrimaryClip(new ClipData(null, new String[]{"text/plain"}, new ClipData.Item(text)));
            }

            @Override
            public void onBell(TerminalSession session) {
                switch (mSettings.mBellBehaviour) {
                    case TermuxPreferences.BELL_BEEP:
                        mBellSoundPool.play(mBellSoundId, 1.f, 1.f, 1, 0, 1.f);
                        break;
                    case TermuxPreferences.BELL_VIBRATE:
                        ((Vibrator) getSystemService(VIBRATOR_SERVICE)).vibrate(50);
                        break;
                    case TermuxPreferences.BELL_IGNORE:
                        // Ignore the bell character.
                        break;
                }
            }

            @Override
            public void onColorsChanged(TerminalSession changedSession) {
                if (getCurrentTermSession() == changedSession) updateBackgroundColor();
            }
        };

        if (mTermuxService.getSessions().isEmpty()) {
            TermuxInstaller.setupIfNeeded(TermuxVRActivity.this, new Runnable() {
                @Override
                public void run() {
                    addNewSession(false, null);
                }
            });
        } else {
            Intent i = getIntent();
            if (i != null && Intent.ACTION_RUN.equals(i.getAction())) {
                // Android 7.1 app shortcut from res/xml/shortcuts.xml.
                addNewSession(false, null);
            } else {
                int numberOfSessions = mTermuxService.getSessions().size();
                switchToSession(mTermuxService.getSessions().get(numberOfSessions - 1));
            }
        }

        // Render initial frame
        mScheduleRedraw = true;
    }

    @Override
    @SuppressWarnings("VariableNotUsedInsideIf")
    public void onServiceDisconnected(ComponentName componentName) {
        if (mTermuxService != null) {
            Log.i(TAG, "Service died, exiting");
            finish();
        }
    }

    @Override
    public void onRendererShutdown() {
        Log.i(TAG, "onRendererShutdown");
    }

    @Override
    public void onSurfaceChanged(int width, int height) {
        Log.i(TAG, "onSurfaceChanged");
    }

    @Override
    public void onSurfaceCreated(EGLConfig config) {
        GLES20.glClearColor(0, 0, 0, 1);

        int vertexShader = ShaderUtils.loadGLShader(this, GLES20.GL_VERTEX_SHADER, R.raw.light_vertex);
        int gridShader = ShaderUtils.loadGLShader(this, GLES20.GL_FRAGMENT_SHADER, R.raw.grid_fragment);
        int passthroughShader = ShaderUtils.loadGLShader(this, GLES20.GL_FRAGMENT_SHADER, R.raw.passthrough_fragment);

        mFloor.init(MeshData.FLOOR_COORDS, MeshData.FLOOR_NORMALS, MeshData.FLOOR_COLORS, vertexShader, gridShader);
        Matrix.translateM(mFloor.getModelMatrix(), 0, 0, -FLOOR_DEPTH, 0);

        mScreen.init(MeshData.SCREEN_COORDS, MeshData.SCREEN_NORMALS, MeshData.SCREEN_COLORS, vertexShader, passthroughShader);
        mScreen.setTextureHandle(OpenGLUtils.makeTexture(renderTerminalView(), false));
        mScreen.setTextureCoords(MeshData.SCREEN_TEXTURE_COORDS);
        Matrix.translateM(mScreen.getModelMatrix(), 0, 0, 0, SCREEN_DISTANCE);
    }

    @Override
    public void onNewFrame(HeadTransform headTransform) {
        // Build the camera matrix and apply it to the ModelView.
        Matrix.setLookAtM(mCamera, 0, 0.0f, 0.0f, CAMERA_Z, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f);
        OpenGLUtils.checkGLError("onReadyToDraw");

        if (mScheduleRedraw) {
            OpenGLUtils.updateTexture(mScreen.getTextureHandle(), renderTerminalView());
            mScheduleRedraw = false;
        }
    }

    @Override
    public void onDrawEye(Eye eye) {
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        OpenGLUtils.checkGLError("colorParam");

        // Apply the eye transformation to the camera.
        Matrix.multiplyMM(mView, 0, eye.getEyeView(), 0, mCamera, 0);

        // Set the position of the light
        Matrix.multiplyMV(mLightPosInEyeSpace, 0, mView, 0, LIGHT_POS_IN_WORLD_SPACE, 0);

        float[] perspective = eye.getPerspective(Z_NEAR, Z_FAR);
        mFloor.draw(mLightPosInEyeSpace, mView, perspective);
        mScreen.draw(mLightPosInEyeSpace, mView, perspective);
    }

    @Override
    public void onFinishFrame(Viewport viewport) {}

    @Override
    public void onCardboardTrigger() {
        Log.i(TAG, "onCardboardTrigger");
        // Always give user feedback.
        mVibrator.vibrate(50);
        getGvrView().recenterHeadTracker();
    }

    private Bitmap renderTerminalView() {
        if (mTerminalView.getWidth() == 0 || mTerminalView.getHeight() == 0) {
            mTerminalView.layout(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);
        }
        Canvas canvas = new Canvas(mRenderTarget);
        canvas.drawColor(mBackgroundColor);
        mTerminalView.draw(canvas);
        return mRenderTarget;
    }

    /// METHODS CALLED BY ViewClient

    @Nullable
    TerminalSession getCurrentTermSession() {
        return mTerminalView.getCurrentSession();
    }

    void addNewSession(boolean failSafe, String sessionName) {
        if (mTermuxService.getSessions().size() >= MAX_SESSIONS) {
            // TODO: Notify the user somehow
            return;
        }
        String executablePath = (failSafe ? "/system/bin/sh" : null);
        TerminalSession newSession = mTermuxService.createTermSession(executablePath, null, null, failSafe);
        if (sessionName != null) {
            newSession.mSessionName = sessionName;
        }
        switchToSession(newSession);
    }

    void switchToSession(TerminalSession session) {
        if (mTerminalView.attachSession(session)) {
            updateBackgroundColor();
        }
    }

    void switchToSession(boolean forward) {
        TerminalSession currentSession = getCurrentTermSession();
        int index = mTermuxService.getSessions().indexOf(currentSession);
        if (forward) {
            if (++index >= mTermuxService.getSessions().size()) index = 0;
        } else {
            if (--index < 0) index = mTermuxService.getSessions().size() - 1;
        }
        switchToSession(mTermuxService.getSessions().get(index));
    }

    void changeFontSize(boolean increase) {
        mSettings.changeFontSize(this, increase);
        mTerminalView.setTextSize(mSettings.getFontSize());
    }

    void updateBackgroundColor() {
        TerminalSession session = getCurrentTermSession();
        if (session != null && session.getEmulator() != null) {
            mBackgroundColor = session.getEmulator().mColors.mCurrentColors[TextStyle.COLOR_INDEX_BACKGROUND];
        }
    }

    void removeFinishedSession(TerminalSession finishedSession) {
        int index = mTermuxService.removeTermSession(finishedSession);
        if (mTermuxService.getSessions().isEmpty()) {
            // There are no sessions to show, so finish the activity.
            finish();
        } else {
            if (index >= mTermuxService.getSessions().size()) {
                index = mTermuxService.getSessions().size() - 1;
            }
            switchToSession(mTermuxService.getSessions().get(index));
        }
    }

    void doPaste() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clipData = clipboard.getPrimaryClip();
        if (clipData == null) return;
        CharSequence paste = clipData.getItemAt(0).coerceToText(this);
        if (!TextUtils.isEmpty(paste))
            getCurrentTermSession().getEmulator().paste(paste.toString());
    }
}
