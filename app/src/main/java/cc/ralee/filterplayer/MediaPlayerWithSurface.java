package cc.ralee.filterplayer;

import android.media.MediaPlayer;
import android.util.Log;
import android.view.Surface;
import android.widget.SeekBar;

import java.io.File;
import java.io.IOException;

/**
 * MediaPlayer plays video to a surface ,then use OpenGL renderer the surface
 */


public class MediaPlayerWithSurface implements  MediaPlayer.OnPreparedListener {

    private MediaPlayer mediaPlayer;
    private String videoPath;
    private Surface mSurface;
    private SeekBar seekBar;
    private boolean isPlaying = false ;
    public MediaPlayerWithSurface(String videoPath, Surface mSurface, SeekBar seekBar) {
        this.mSurface = mSurface;
        this.videoPath = videoPath;
        mediaPlayer = new MediaPlayer();
        this.seekBar = seekBar;
    }
    public void playVideoToSurface() {
        preparePlayer();
    }
    public void changeVideoPath(String newVideoPath) {
        this.videoPath = newVideoPath;
    }
    private void preparePlayer() {
        mediaPlayer.reset();
        File file = new File(videoPath);
        Log.d("Moive File", "Exists:" + file.exists());
        try {
            mediaPlayer.setDataSource(file.getPath());
            mediaPlayer.setScreenOnWhilePlaying(true);
            mediaPlayer.setOnPreparedListener(this);
            mediaPlayer.setLooping(true);
            mediaPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mediaPlayer.setSurface(mSurface);
        mediaPlayer.start();
        isPlaying = true;
        seekBar.setProgress(0);
        seekBar.setMax(mediaPlayer.getDuration());
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress >= 0) {
                    if (fromUser) {
                        mediaPlayer.seekTo(progress);
                    }
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        final Boolean seekBarAutoFlag = true;
        // deal with the seekbar's move
        Thread moveThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while(seekBarAutoFlag) {
                        if( mediaPlayer != null && isPlaying) {
                            seekBar.setProgress(mediaPlayer.getCurrentPosition());
                        }
                    }
                }catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        moveThread.start();
    }

    public void pausePlay() {
        mediaPlayer.pause();
        isPlaying = false;
    }

    public void startPlay() {
        mediaPlayer.start();
        Log.d("Player", "startPlay: ");
        isPlaying = true;
    }

    public void restart() {
        try {
            mediaPlayer.reset();
            mediaPlayer.setDataSource(videoPath);
            mediaPlayer.setScreenOnWhilePlaying(true);
            mediaPlayer.setOnPreparedListener(this);
            mediaPlayer.prepareAsync();
            mediaPlayer.setLooping(true);

        }catch (IOException e ) {
            e.printStackTrace();
        }
    }

    public void release() {
        if(mediaPlayer != null ) {
            mediaPlayer.release();
            mediaPlayer = null ;
        }
    }

}
