package cc.ralee.filterplayer;

import android.opengl.GLSurfaceView;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Spinner;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener, View.OnClickListener {
    public static final String TAG = "MainActivity";
    //change your file path here
    private static final String SAMPLE = Environment.getExternalStorageDirectory() + "/SAMPLE.mp4";

    private GLSurfaceView glSurfaceView ;
    private VideoRenderer videoRenderer;


    static final int FILTER_NONE = 0;
    static final int FILTER_BLACK_WHITE = 1;
    static final int FILTER_BLUR = 2;
    static final int FILTER_SHARPEN = 3;
    static final int FILTER_EDGE_DETECT = 4;
    static final int FILTER_EMBOSS = 5;


    private Button pause;
    static int PLAY_PAUSE_FLAG = 1;
    @Override
    protected void onDestroy() {
        super.onDestroy();
        videoRenderer.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
        glSurfaceView.onPause();
        videoRenderer.pausePlay(1);
    }

    @Override
    protected void onResume() {
        super.onResume();
        glSurfaceView.onResume();
        videoRenderer.onResume();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        glSurfaceView = (GLSurfaceView) findViewById(R.id.glSurfaceView);
        SeekBar seekBar = (SeekBar) findViewById(R.id.seekbar);
        videoRenderer = new VideoRenderer(MainActivity.this,glSurfaceView,seekBar);

        glSurfaceView.setEGLContextClientVersion(2);
        glSurfaceView.setRenderer(videoRenderer);
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        Log.d(TAG, SAMPLE);

        Spinner spinner = (Spinner) findViewById(R.id.cameraFilter_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.cameraFilterNames, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner.
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(this);

        pause = (Button) findViewById(R.id.pause);
        Button replay = (Button) findViewById(R.id.replay);
        pause.setOnClickListener(this);
        replay.setOnClickListener(this);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        Spinner spinner = (Spinner) parent;
        final int filterNum = spinner.getSelectedItemPosition();

        Log.d(TAG, "onItemSelected: " + filterNum);
        glSurfaceView.queueEvent(new Runnable() {
            @Override public void run() {
                // notify the renderer that we want to change the encoder's state
                videoRenderer.changeFilterMode(filterNum);
            }
        });
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
//            case R.id.play:
//                videoRenderer.startPlay();
//                Log.d(TAG, "onClick: ");
//                break;
            case R.id.pause:
                videoRenderer.pausePlay(PLAY_PAUSE_FLAG);
                if(PLAY_PAUSE_FLAG == 1) {
                    pause.setText("Continue");
                }else {
                    pause.setText("Pause");
                }
                PLAY_PAUSE_FLAG *= -1;

                break;
            case R.id.replay:
                videoRenderer.restart();
                PLAY_PAUSE_FLAG = 1;
                pause.setText("Pause");
                break;
            default:
                break;
        }
    }

}

