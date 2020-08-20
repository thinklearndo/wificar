package com.thinklearndo.wificar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.FragmentManager;

import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class MainActivity extends AppCompatActivity {

    ImageView left_right_IV;
    ImageView up_down_IV;

    FrameLayout lr_view;

    FrameLayout ud_view;

    int windowHeight = -1;
    int windowWidth = -1;

    float lr_touch_initialX  =-1;
    float left_buffer = -1;
    float right_buffer = -1;
    float lr_start_x = -1;

    float ud_touch_initialY  =-1;
    float top_buffer = -1;
    float bottom_buffer = -1;
    float ud_start_y = -1;

    int deadband = 20;

    float motor_scale = 2.55f;

    Boolean touching_lr = false;
    Boolean touching_ud = false;

    int port = 4210;
    DatagramSocket socket;
    String dest_ip = "192.168.2.255";
    InetAddress dest_address;

    Boolean should_send_packets = false;

    final Handler handler = new Handler();
    Runnable pacekt_sender_runnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();

        StrictMode.setThreadPolicy(policy);


        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);

        left_right_IV = (ImageView) findViewById(R.id.imageView_lr);
        up_down_IV = (ImageView) findViewById(R.id.imageView2_ud);

        lr_view = (FrameLayout) findViewById(R.id.lr_view);
        ud_view = (FrameLayout) findViewById(R.id.ud_view);

        lr_view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (windowHeight != -1) {
                    return;
                }
                windowHeight = lr_view.getHeight(); //height is ready
                windowWidth = lr_view.getWidth();

                left_right_IV.setY(windowHeight / 2);
                left_right_IV.setX(windowWidth / 2);
                lr_start_x = windowWidth / 2;
                right_buffer = windowWidth - left_right_IV.getWidth();
                left_buffer = left_right_IV.getWidth();
                up_down_IV.setY(windowHeight / 2);
                up_down_IV.setX(windowWidth / 2);
                top_buffer = up_down_IV.getHeight();
                bottom_buffer = windowHeight - up_down_IV.getHeight();
                ud_start_y = windowHeight / 2;

            }
        });

        left_right_IV.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        lr_touch_initialX = left_right_IV.getX() - event.getRawX();
                        touching_lr = true;
                        break;

                    case MotionEvent.ACTION_MOVE:
                        if (event.getRawX() + lr_touch_initialX > right_buffer) {
                            left_right_IV.setX(right_buffer);
                        } else if (event.getRawX() + lr_touch_initialX < left_buffer) {
                            left_right_IV.setX(left_buffer);
                        } else {
                            left_right_IV.setX(event.getRawX() + lr_touch_initialX);
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        left_right_IV.setX(lr_start_x);
                        touching_lr = false;
                    default:
                        return false;
                }
                return true;
            }
        });

        up_down_IV.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getActionMasked()) {

                    case MotionEvent.ACTION_DOWN:
                        ud_touch_initialY = up_down_IV.getY() - event.getRawY();
                        touching_ud = true;
                        break;

                    case MotionEvent.ACTION_MOVE:
                        if (event.getRawY() + ud_touch_initialY < top_buffer) {
                            up_down_IV.setY(top_buffer);
                        } else if (event.getRawY() + ud_touch_initialY > bottom_buffer) {
                            up_down_IV.setY(bottom_buffer);
                        } else {
                            up_down_IV.setY(event.getRawY() + ud_touch_initialY);
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        up_down_IV.setY(ud_start_y);
                        touching_ud = false;
                    default:
                        return false;
                }
                return true;
            }
        });

        pacekt_sender_runnable = new Runnable() {
            @Override
            public void run() {

                float x_position = left_right_IV.getX();
                float y_position = up_down_IV.getY();

                byte motor1_power = 0;
                char motor1_direction = 'F';

                byte motor2_power = 0;
                char motor2_direction = 'F';

                if (touching_lr == true) {

                    if (x_position <= lr_start_x) {
                        motor1_direction = 'R';

                        float start_with_buffer = lr_start_x - deadband;
                        float max_with_buffer = left_buffer + deadband;

                        if (x_position < max_with_buffer) {
                            motor1_power = -1; //unsigned
                        } else if (x_position > start_with_buffer) {
                            motor1_power = 0;
                        } else {
                            float commandedPower = ((x_position - start_with_buffer) * 100) / (max_with_buffer - start_with_buffer);

                            motor1_power = (byte) (commandedPower * motor_scale);
                        }

                    } else {
                        motor1_direction = 'F';

                        float start_with_buffer = lr_start_x + deadband;
                        float max_with_buffer = right_buffer - deadband;

                        if (x_position > max_with_buffer) {
                            motor1_power = -1; //unsigned
                        } else if (x_position < start_with_buffer) {
                            motor1_power = 0;
                        } else {

                            float commandedPower = ((x_position - start_with_buffer) * 100) / (max_with_buffer - start_with_buffer);

                            motor1_power = (byte) (commandedPower * motor_scale);
                        }
                    }
                }

                if (touching_ud == true) {

                    if (y_position >= ud_start_y) {
                        motor2_direction = 'R';

                        float start_with_buffer = ud_start_y + deadband;
                        float max_with_buffer = bottom_buffer - deadband;

                        if (y_position > max_with_buffer) {
                            motor2_power = -1; //unsigned
                        } else if (y_position <= start_with_buffer) {
                            motor2_power = 0;
                        } else {
                            float commandedPower = ((y_position - start_with_buffer) * 100) / (max_with_buffer - start_with_buffer);

                            motor2_power = (byte) (commandedPower * motor_scale);
                        }
                    } else {
                        motor2_direction = 'F';

                        float start_with_buffer = ud_start_y - deadband;
                        float max_with_buffer = top_buffer + deadband;

                        if (y_position < max_with_buffer) {
                            motor2_power = -1; //unsigned
                        } else if (x_position >= start_with_buffer) {
                            motor2_power = 0;
                        } else {

                            float commandedPower = ((y_position - start_with_buffer) * 100) / (max_with_buffer - start_with_buffer);

                            motor2_power = (byte) (commandedPower * motor_scale);
                        }
                    }
                }


                byte[] data = new byte[]{(byte) motor1_direction, motor1_power, (byte) motor2_direction, motor2_power};

                if (dest_address == null) {
                    try {
                        dest_address = InetAddress.getByName(dest_ip);
                    } catch (java.net.UnknownHostException exception) {
                        Toast.makeText(MainActivity.this, "Exception getting broadcast address by name!", Toast.LENGTH_LONG).show();
                        finish();
                    }
                }

                //DatagramPacket packet = new DatagramPacket(data, data.length, dest_address, port);

                DatagramSocket datagram_socket = null;
                try {
                    datagram_socket = new DatagramSocket(null);
                    datagram_socket.setReuseAddress(true);
                    datagram_socket.setBroadcast(true);
                    DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName("255.255.255.255"), port);
                    datagram_socket.send(packet);

                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (datagram_socket != null) {
                        datagram_socket.close();
                    }
                }

                if (should_send_packets) {

                    handler.postDelayed(this, 50);
                }
            }
        };
    }

    @Override
    protected void onResume() {
        // call the superclass method first
        super.onResume();

        should_send_packets = true;

        handler.postDelayed(pacekt_sender_runnable, 50);

    }

    @Override
    protected void onPause() {
        // call the superclass method first
        super.onPause();

        should_send_packets = false;

    }


}
