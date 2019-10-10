package com.lw.openlight;

import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.AppCompatSpinner;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.SpinnerAdapter;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    String[] open = {"setSwitchOff.fwp", "setSwitchOn.fwp"}; //开启关闭选项
    URL url;//连接url我也不知道为什么要弄成全局的，当时脑子抽了
    URLConnection conn;//全局的conn
    List<String> cookieList;//cookie
    Handler handler;//handler
    List<User> users;//用户列表
    Map<String,User> userMap = new HashMap<>();//用户map
    TextView quantity;
    TextView rest;
    TextView status;
    TextView login;
    Button openButton;
    AppCompatSpinner spinner;
    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        //消息处理
        handler = new Handler(message -> {
            if (message.what == 1) {
                Toast.makeText(MainActivity.this.getApplicationContext(), "已发送请求", Toast.LENGTH_LONG).show();
            } else if(message.what == -1) {
                Toast.makeText(MainActivity.this.getApplicationContext(), (String)message.obj, Toast.LENGTH_LONG).show();
            }
            else if(message.what == -2)
            {
                login.setBackgroundColor(0xFFE27171);
                login.setText("未连接成功 点击刷新");
                Toast.makeText(MainActivity.this.getApplicationContext(), "校园网连接失败", Toast.LENGTH_LONG).show();
                login.setEnabled(true);
                openButton.setEnabled(false);
                spinner.setEnabled(false);
            }
            else if(message.what == 2)
            {
                UserInfo userInfo = (UserInfo) message.obj;
                quantity.setText(userInfo.getCurrentQuantity());
                rest.setText(userInfo.getRemainMoney());
                status.setText(userInfo.getState());
            }
            else if(message.what == 3)
            {
                login.setBackgroundColor(0xFF7CE271);
                login.setText("连接成功");
                openButton.setEnabled(true);
                spinner.setEnabled(true);
                login.setEnabled(false);
            }
            return false;
        });

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //控件初始化
        openButton = findViewById(R.id.open);
        final Switch switchO = findViewById(R.id.switchOpen);
        spinner = findViewById(R.id.select);
        quantity = findViewById(R.id.quantity);
        rest = findViewById(R.id.rest);
        status = findViewById(R.id.status);
        login = findViewById(R.id.login);

        //加载存储信息
        sharedPreferences = getSharedPreferences("init", MODE_PRIVATE);
        String name = sharedPreferences.getString("floor", "W-101");
        //将下拉框选项调整为存储信息
        SpinnerAdapter adapter = spinner.getAdapter();
        int k = adapter.getCount();
        for (int i = 0; i < k; i++) {
            if (name.equals(adapter.getItem(i).toString())) {
                spinner.setSelection(i, true);
                break;
            }
        }
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                flushInfo();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        //登录系统，获取cookie
        login();
        //读取信息，并且将信息载入到users中
        StringBuilder userString = new StringBuilder();
        InputStream is = getResources().openRawResource(R.raw.users);
        InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
        BufferedReader br = new BufferedReader(isr);
        String str;
        try{
        while ((str = br.readLine()) != null) {
            userString.append(str);
        }
        } catch (IOException e) {
            e.printStackTrace();
        }
        users = JSON.parseArray(new String(userString), User.class);
        users.forEach((e)-> userMap.put(e.ammeterUserName,e));
        openButton.setOnClickListener(v -> {
            String room = spinner.getSelectedItem().toString();
            User u = userMap.get(room);
            new AlertDialog.Builder(MainActivity.this).setTitle("确定对" + room + "进行操作?")
                    .setPositiveButton("确定", (dialog, which) -> {
                        if (cookieList != null && cookieList.size() != 0)
                            new Thread(() -> {
                                try {
                                    String x;
                                    if (switchO.isChecked()) {
                                        x = open[1];
                                    } else {
                                        x = open[0];
                                    }
                                    url = new URL("http://172.16.254.254:9091/project/CPES_LC/fwp/" + x + "?_dc=" + new Date().getTime() + "&ammeterUserID=" + u.getAmmeterUserID() + "&ammeterCode=" + u.getAmmeterCode() + "&ammeterNodeID=" + u.getAmmeterNodeID() + "&operator=]");
                                    System.out.println(url.getQuery());
                                    conn = url.openConnection();
                                    conn.setConnectTimeout(1200);
                                    conn.setDoInput(true);
                                    conn.setDoOutput(true);
                                    conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
                                    conn.setRequestProperty("Referer", "http://172.16.254.254:9091/project/CPES_LC/");
                                    String cookie = "fUser1=admin;" + cookieList.get(0).split(";")[0];
                                    conn.setRequestProperty("Cookie", cookie);
                                    conn.connect();
                                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                                    String line;
                                    StringBuilder strBuf = new StringBuilder();
                                    while ((line = reader.readLine()) != null) {
                                        strBuf.append(line);
                                    }
                                    Message m = new Message();
                                    m.what = 1;
                                    handler.sendMessage(m);
                                } catch (Exception e) {
                                    Message m = new Message();
                                    m.what = -2;
                                    m.obj = e.getMessage();
                                    handler.sendMessage(m);
                                }
                            }).start();
                        else
                            Toast.makeText(MainActivity.this.getApplicationContext(), "未连接校园网", Toast.LENGTH_LONG).show();

                    })
                    .setNegativeButton("我再想想", (dialog, which) -> System.out.println("取消")).show();

        });
        login.setOnClickListener(view -> {
            login();
        });
    }
    void flushInfo() {
        if (cookieList != null && cookieList.size() != 0)
            new Thread(() -> {
                //获取剩余金额信息
                try {
                    String room = spinner.getSelectedItem().toString();
                    User u = userMap.get(room);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("floor", room);
                    editor.apply();
                    url = new URL("http://172.16.254.254:9091/project/CPES_LC/fwp/getAccountInfo.fwp?ammeterNodeID=" + u.ammeterNodeID + "&_dc=" + new Date().getTime());
                    conn = url.openConnection();
                    conn.setConnectTimeout(1200);
                    conn.setDoInput(true);
                    conn.setDoOutput(true);
                    conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
                    conn.setRequestProperty("Referer", "http://172.16.254.254:9091/project/CPES_LC/");
                    String cookie = "fUser1=admin;" + cookieList.get(0).split(";")[0];
                    conn.setRequestProperty("Cookie", cookie);
                    conn.connect();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                    String line;
                    StringBuilder strBuf = new StringBuilder();
                    while ((line = reader.readLine()) != null) {
                        strBuf.append(line);
                    }
                    UserInfo userInfo = JSON.parseObject(strBuf.toString(), UserInfo.class);
                    Message m = new Message();
                    m.what = 2;
                    m.obj = userInfo;
                    handler.sendMessage(m);
                } catch (Exception e) {
                    e.printStackTrace();
                    Message m = new Message();
                    m.what = -2;
                    handler.sendMessage(m);
                }
            }).start();
        else
            Toast.makeText(MainActivity.this.getApplicationContext(), "未连接校园网", Toast.LENGTH_LONG).show();
    }
    void login()
    {
        login.setText("正在连接...");
        login.setEnabled(false);
        openButton.setEnabled(false);
        spinner.setEnabled(false);
        new Thread(() ->
        {
            try {
                url = new URL("http://172.16.254.254:9091/fwp/login.fwps");
                conn = url.openConnection();
                conn.setConnectTimeout(1200);
                conn.setDoInput(true);
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
                conn.setRequestProperty("Referer", "http://172.16.254.254:9091/main.html");
                PrintWriter out = new PrintWriter(conn.getOutputStream());
                out.print("userName=admin&passHash=2a5f114a33ffbd1765c23c9013fee3c82deec759");
                out.flush();
                cookieList = conn.getHeaderFields().get("Set-Cookie");
                if(cookieList!=null&&cookieList.size()!=0)
                {
                    //成功
                    Message m = new Message();
                    m.what = 3;
                    handler.sendMessage(m);
                }
                flushInfo();
            } catch (Exception e) {
                e.printStackTrace();
                //失败
                Message m = new Message();
                m.what = -2;
                handler.sendMessage(m);
            }
        }).start();
    }
}
