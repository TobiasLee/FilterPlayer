package cc.ralee.filterplayer;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Environment;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceView;
import android.widget.SeekBar;

import java.io.IOException;
import java.nio.ByteBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import cc.ralee.filterplayer.gles.FullFrameRect;
import cc.ralee.filterplayer.gles.Texture2dProgram;

/**
 * This is a VideoRenderer to renderer the video right away
 */

public class VideoRenderer extends SurfaceView implements GLSurfaceView.Renderer{
    private String videoPath = null;

    private Context context;
    private final String TAG = "VideoRenderer";
    private PlayerThread mPlayer;
    private FullFrameRect mFullScreen;
    private SurfaceTexture mSurfaceTexture;
    private final float[] mSTMatrix = new float[16];
    private int mTextureId;
    GLSurfaceView mGLSurfaceView ;
    private Surface mSurface;
    private MediaPlayerWithSurface playerWithSurface;
    private SeekBar seekBar;

    private int mCurrentFilter;
    private int mNewFilter;
    private boolean mIncomingSizeUpdated;

    public VideoRenderer(Context context, GLSurfaceView glSurfaceView, SeekBar seekBar, String videoPath) {
        super(context);
        this.mGLSurfaceView = glSurfaceView;
        Log.d(TAG, "VideoRenderer: created");
        mCurrentFilter = -1;
        mNewFilter = MainActivity.FILTER_NONE;
        this.seekBar = seekBar;
        this.videoPath = videoPath;
    }

    protected int surfaceWidth, surfaceHeight;

    public void updateFilter() {
        Texture2dProgram.ProgramType programType;
        float[] kernel = null;
        float colorAdj = 0.0f;

        Log.d(TAG, "Updating filter to " + mNewFilter);
        switch (mNewFilter) {
            case MainActivity.FILTER_NONE:
                programType = Texture2dProgram.ProgramType.TEXTURE_EXT;
                break;
            case MainActivity.FILTER_BLACK_WHITE:
                // (In a previous version the TEXTURE_EXT_BW variant was enabled by a flag called
                // ROSE_COLORED_GLASSES, because the shader set the red channel to the B&W color
                // and green/blue to zero.)
                programType = Texture2dProgram.ProgramType.TEXTURE_EXT_BW;
                break;
            case MainActivity.FILTER_BLUR:
                programType = Texture2dProgram.ProgramType.TEXTURE_EXT_FILT;
                kernel = new float[] {
                        1f/16f, 2f/16f, 1f/16f,
                        2f/16f, 4f/16f, 2f/16f,
                        1f/16f, 2f/16f, 1f/16f };
                break;
            case MainActivity.FILTER_SHARPEN:
                programType = Texture2dProgram.ProgramType.TEXTURE_EXT_FILT;
                kernel = new float[] {
                        0f, -1f, 0f,
                        -1f, 5f, -1f,
                        0f, -1f, 0f };
                break;
            case MainActivity.FILTER_EDGE_DETECT:
                programType = Texture2dProgram.ProgramType.TEXTURE_EXT_FILT;
                kernel = new float[] {
                        -1f, -1f, -1f,
                        -1f, 8f, -1f,
                        -1f, -1f, -1f };
                break;
            case MainActivity.FILTER_EMBOSS:
                programType = Texture2dProgram.ProgramType.TEXTURE_EXT_FILT;
                kernel = new float[] {
                        2f, 0f, 0f,
                        0f, -1f, 0f,
                        0f, 0f, -1f };
                colorAdj = 0.5f;
                break;
            default:
                throw new RuntimeException("Unknown filter mode " + mNewFilter);
        }

        if (programType != mFullScreen.getProgram().getProgramType()) {
            mFullScreen.changeProgram(new Texture2dProgram(programType));
            // If we created a new program, we need to initialize the texture width/height.
            mIncomingSizeUpdated = true;
        }

        // Update the filter kernel (if any).
        if (kernel != null) {
            mFullScreen.getProgram().setKernel(kernel, colorAdj);
        }
        mCurrentFilter = mNewFilter;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Log.d(TAG, "onSurfaceCreated: ");
        mFullScreen = new FullFrameRect(
                new Texture2dProgram(Texture2dProgram.ProgramType.TEXTURE_EXT));
        mTextureId = mFullScreen.createTextureObject();
        mSurfaceTexture = new SurfaceTexture(mTextureId);

        mSurface = new Surface(mSurfaceTexture);

        mSurfaceTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
            @Override
            public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                mGLSurfaceView.requestRender();
            }
        });
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        Log.d(TAG, "onSurfaceChanged:  changed");
        // use mediacodec to decode the video without sound
//        if(mPlayer == null) {
//            mPlayer = new PlayerThread(mSurface);
//            mPlayer.start();
//        }

        if( playerWithSurface == null ) {
            playerWithSurface = new MediaPlayerWithSurface(videoPath, mSurface, seekBar);
//            playerWithSurface.playVideoToSurface();
        }

        GLES20.glViewport(0,0,width, height);
        surfaceWidth = width;
        surfaceHeight = height;

    }

    public void changeVideoPathAndPlay(String videoPath) {
        playerWithSurface.changeVideoPath(videoPath);
        playerWithSurface.playVideoToSurface();
    }

    @Override
    public void onDrawFrame(GL10 gl) {

        mSurfaceTexture.updateTexImage();

        if (mCurrentFilter != mNewFilter) {
            updateFilter();
        }

        mSurfaceTexture.getTransformMatrix(mSTMatrix);
        mFullScreen.drawFrame(mTextureId, mSTMatrix);
    }

    public void changeFilterMode(int filter) {
        mNewFilter = filter;
    }


    //MediaCodec解码线程 输出到surface
    private class PlayerThread extends Thread {

        private MediaExtractor extractor;
        private MediaCodec decoder;
        private Surface surface;

        public PlayerThread(Surface surface) {
            this.surface = surface;
        }

        @Override
        public void run() {
            Log.d(TAG, "run: start to run");
            extractor = new MediaExtractor();
            try {
                extractor.setDataSource(videoPath);
                Log.d(TAG, "Data Source set");
            } catch (IOException e) {
                e.printStackTrace();
            }

            for (int i = 0; i < extractor.getTrackCount(); i++) {

                MediaFormat format = extractor.getTrackFormat(i);
                Log.d(TAG, "format: " + format);
                String mime = format.getString(MediaFormat.KEY_MIME);

                if(mime.startsWith("video/")) {
                    extractor.selectTrack(i);
                    Log.d(TAG, "Track selected");

                    try {
                        decoder = MediaCodec.createDecoderByType(mime);
                        Log.d(TAG, "Encoder created");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    format.setInteger(MediaFormat.KEY_BIT_RATE, 870000);
                    format.setInteger(MediaFormat.KEY_SAMPLE_RATE,44100 );
                    format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1);

                    Log.d(TAG, "format set");
                    if(surface != null) {
                        Log.d(TAG, "surface != null");
                    }
                    decoder.configure(format,surface,null,0);
                    Log.d(TAG, "Encoder configured");
                    break;
                }
            }

            if(decoder == null){
                Log.d(TAG, "can't find video info");
                return;
            }
            decoder.start();
            Log.d(TAG, "Encoder start");


            ByteBuffer[] inputBuffers = decoder.getInputBuffers();
            ByteBuffer[] outputBuffers = decoder.getOutputBuffers();

            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            boolean isEOS = false ;
            long startMs = System.currentTimeMillis();

            while(!Thread.interrupted()) {
                if(!isEOS) {
                    int inIndex = decoder.dequeueInputBuffer(100000);
                    if(inIndex >= 0 ) {
                        ByteBuffer buffer = inputBuffers[inIndex];
                        int sampleSize = extractor.readSampleData(buffer,0);
                        if(sampleSize < 0) {
                            //不终止 把 eos 标志传给 encoder 会再次 从 outputBuffer 中得到这个标志
                            Log.d(TAG, "InputBuffer BUFFER_FALG_END_OF_STREAM");
                            decoder.queueInputBuffer(inIndex,0,0,0,MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            isEOS = true;
                        } else {
                            decoder.queueInputBuffer(inIndex,0,sampleSize,extractor.getSampleTime(),0);
                            extractor.advance();
                        }
                    }
                }

                int outIndex = decoder.dequeueOutputBuffer(info,10000);
                switch (outIndex) {
                    case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                        Log.d(TAG, "INFO_OUTPUT_FORMAT_CHANGED");
                        break;
                    case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                        Log.d(TAG, "INFO_OUTPUT_BUFFERS_CHANGED");
                        outputBuffers = decoder.getOutputBuffers();
                        break;
                    case MediaCodec.INFO_TRY_AGAIN_LATER:
                        Log.d(TAG, "dequeueOutputBuffer out of time");
                        break;
                    default:
                        ByteBuffer buffer = outputBuffers[outIndex];
                        Log.v(TAG,"We can't use this buffer but render it due to API limit" + buffer);
                        // keep the fps using a simple clock
                        while(info.presentationTimeUs / 1000 > System.currentTimeMillis() - startMs) {
                            try {
                                sleep(10);
                            }catch (InterruptedException e) {
                                e.printStackTrace();
                                break;
                            }
                        }
                        decoder.releaseOutputBuffer(outIndex,true);
                        break;
                }

                if( (info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    Log.d(TAG, "BUFFER_FLAG_END_OFSTREAM");
                }
            }
            decoder.stop();
            decoder.release();
            extractor.release();
        }
    }

    public void pausePlay(int flag) {
        if( flag == 1) {
            playerWithSurface.pausePlay();
        }else {
            playerWithSurface.startPlay();
        }
    }

    private void startPlay() {
        playerWithSurface.startPlay();
    }

    public void restart() {
        playerWithSurface.restart();
    }

    public void onDestroy() {
        if(playerWithSurface != null ) {
            playerWithSurface.release();
            playerWithSurface = null ;
        }
    }

    public void onResume() {
        if(playerWithSurface != null )
            playerWithSurface.startPlay();
    }
}
