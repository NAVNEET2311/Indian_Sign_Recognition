package com.example.mlrecognition;

import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;

public class TrainingSessionActivity extends AppCompatActivity {

    private VideoView videoView;
    private Button playButton, pauseButton, rewindButton, forwardButton, backButton;  // Use Button here
    private SeekBar seekBar;
    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_training_session);

        // Initialize UI components
        videoView = findViewById(R.id.videoView);
        playButton = findViewById(R.id.playButton);
        pauseButton = findViewById(R.id.pauseButton);
        rewindButton = findViewById(R.id.rewindButton);
        forwardButton = findViewById(R.id.forwardButton);
        backButton = findViewById(R.id.backButton);  // Back Button initialization
        seekBar = findViewById(R.id.seekBar);

        // Load the video from the 'raw' folder
        Uri videoUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.slowed);
        videoView.setVideoURI(videoUri);

        // Auto-restart video when it finishes
        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                videoView.start();
            }
        });

        // Back button functionality (navigate back to MainActivity)
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed(); // Navigate back to the previous activity (MainActivity)
            }
        });

        // Play button functionality
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!videoView.isPlaying()) {
                    videoView.start();
                }
            }
        });

        // Pause button functionality
        pauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (videoView.isPlaying()) {
                    videoView.pause();
                }
            }
        });

        // Rewind 5 seconds
        rewindButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int currentPosition = videoView.getCurrentPosition();
                int newPosition = Math.max(currentPosition - 5000, 0);  // Prevent seeking before start of video
                videoView.seekTo(newPosition);
            }
        });

        // Forward 5 seconds
        forwardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int currentPosition = videoView.getCurrentPosition();
                int maxPosition = videoView.getDuration();
                int newPosition = Math.min(currentPosition + 5000, maxPosition); // Prevent seeking past the end
                videoView.seekTo(newPosition);
            }
        });

        // Update SeekBar as the video plays
        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                seekBar.setMax(videoView.getDuration());

                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        seekBar.setProgress(videoView.getCurrentPosition());
                        handler.postDelayed(this, 500);
                    }
                }, 500);
            }
        });

        // Allow user to seek using SeekBar
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    videoView.seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null); // Prevent memory leaks
    }
}
