package com.trng.todoapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.TestLooperManager;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private static final int SPLASH = 3300;

    Animation topAni, bottomAni;
    ImageView myImage;
    TextView txtAppName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        topAni = AnimationUtils.loadAnimation(this, R.anim.top_animation);
        bottomAni = AnimationUtils.loadAnimation(this, R.anim.bottom_animation);

        myImage = findViewById(R.id.myImage);
        txtAppName = findViewById(R.id.txtAppName);

        //set animation for image and name text
        myImage.setAnimation(topAni);
        txtAppName.setAnimation(bottomAni);

        //delay 3300 before switch to "todolist activity"
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(MainActivity.this, ToDoList.class);
                startActivity(intent);
                finish();
            }
        }, SPLASH);
    }
}