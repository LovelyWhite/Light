package com.lw.openlight;

import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.AppCompatSpinner;
import android.view.View;
import android.widget.Button;
import android.widget.SpinnerAdapter;
import android.widget.Switch;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button button = findViewById(R.id.open);
        final Switch switchO = findViewById(R.id.switchOpen);
        final AppCompatSpinner spinner = findViewById(R.id.select);
        try {
            FileInputStream fileInputStream  = openFileInput("init.ini");
            byte[] t = new byte[fileInputStream.available()];
            fileInputStream.read(t);
            String name = new String(t);
            System.out.println(name);
            SpinnerAdapter adapter = spinner.getAdapter();
            int k = adapter.getCount();
            for(int i =0;i<k;i++)
            {
                if(name.equals(adapter.getItem(i).toString()))
                {
                    spinner.setSelection(i,true);
                    break;
                }
            }
        } catch (FileNotFoundException e) {
            //e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        final Handler handler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message message) {
                if (message.what == 1) {
                    Toast.makeText(MainActivity.this.getApplicationContext(), "已发送请求", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(MainActivity.this.getApplicationContext(), message.getData().getString("Data"), Toast.LENGTH_LONG).show();
                }
                return false;
            }
        });
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String room = spinner.getSelectedItem().toString();
                final String open[] = new String[2];
                open[0]= "setSwitchOff.fwp";
                open[1]="setSwitchOn.fwp";
                new AlertDialog.Builder(MainActivity.this).setTitle("确定对"+room+"进行操作?")
                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                try {
                                    FileOutputStream fileOutputStream =openFileOutput("init.ini",MODE_PRIVATE);
                                    fileOutputStream.write(room.getBytes());
                                    fileOutputStream.close();
                                } catch (FileNotFoundException e) {
                                    e.printStackTrace();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            StringBuilder b = new StringBuilder();
                                            InputStream is = getResources().openRawResource(R.raw.users);
                                            InputStreamReader isr = new InputStreamReader(is, "UTF-8");
                                            BufferedReader br = new BufferedReader(isr);
                                            String str;
                                            while ((str = br.readLine()) != null) {
                                                b.append(str);
                                            }
                                            User u = new User();
                                            List<User> a = JSON.parseArray(new String(b), User.class);
                                            for (User i : a
                                                    ) {
                                                if (i.ammeterUserName.equals(room)) {
                                                    u.setAmmeterCode(i.getAmmeterCode());
                                                    u.setAmmeterNodeID(i.getAmmeterNodeID());
                                                    u.setAmmeterUserID(i.getAmmeterUserID());
                                                    break;
                                                }
                                            }
                                            StringBuilder result = new StringBuilder();
                                            URL url = new URL("http://172.16.254.254:9091/fwp/login.fwps");
                                            URLConnection conn = url.openConnection();
                                            conn.setDoInput(true);
                                            conn.setDoOutput(true);
                                            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
                                            conn.setRequestProperty("Referer", "http://172.16.254.254:9091/main.html");
                                            PrintWriter out = new PrintWriter(conn.getOutputStream());
                                            out.print("userName=admin&passHash=2a5f114a33ffbd1765c23c9013fee3c82deec759");
                                            out.flush();
                                            List<String> cookieList = conn.getHeaderFields().get("Set-Cookie");
                                            String x;
                                            if(switchO.isChecked())
                                            {
                                                x=open[1];
                                            }
                                                else {
                                                x=open[0];
                                            }
                                            url = new URL("http://172.16.254.254:9091/project/CPES_LC/fwp/"+x+"?_dc="+new Date().getTime()+"&ammeterUserID=" + u.getAmmeterUserID() + "&ammeterCode=" + u.getAmmeterCode() + "&ammeterNodeID=" + u.getAmmeterNodeID() + "&operator=]");
                                            System.out.println(url.getQuery());
                                            conn = url.openConnection();
                                            conn.setDoInput(true);
                                            conn.setDoOutput(true);
                                            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
                                            conn.setRequestProperty("Referer", "http://172.16.254.254:9091/project/CPES_LC/");
                                            String cookie = "fUser1=admin;" + cookieList.get(0).split(";")[0];
                                            conn.setRequestProperty("Cookie", cookie);
                                            conn.connect();
                                            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(),"UTF-8"));
                                            String line;
                                            StringBuilder strBuf = new StringBuilder();
                                            while ((line = reader.readLine()) != null) {
                                                strBuf.append(line);
                                            }
                                            System.out.println(strBuf.toString());
                                            Message m = new Message();
                                            m.what = 1;
                                            handler.sendMessage(m);
                                        } catch (IOException e) {
                                            Message m = new Message();
                                            Bundle b = new Bundle();
                                            b.putString("Data", e.getMessage());
                                            m.what = -1;
                                            m.setData(b);
                                            handler.sendMessage(m);
                                        }
                                    }
                                }).start();
                                System.out.println("确定");
                            }
                        })
                        .setNegativeButton("我再想想", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                System.out.println("取消");
                            }
                        }).show();

            }
        });

    }
}
