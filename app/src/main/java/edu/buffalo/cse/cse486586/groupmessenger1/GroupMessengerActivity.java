package edu.buffalo.cse.cse486586.groupmessenger1;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 * 
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {

    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    static final String[] REMOTE_PORT = {"11108","11112","11116","11120","11124"};
    static final int SERVER_PORT = 10000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));

        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            Log.e(TAG, "Can't create a ServerSocket "+e.getLocalizedMessage());
            return;
        }
        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */
        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());
        
        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));

        final Button btn = (Button)findViewById(R.id.button4);
        final EditText editText = (EditText) findViewById(R.id.editText1);
        Log.d("test","Before defining listeners");

        editText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                        (keyCode == KeyEvent.KEYCODE_ENTER)) {
            /*
             * If the key is pressed (i.e., KeyEvent.ACTION_DOWN) and it is an enter key
             * (i.e., KeyEvent.KEYCODE_ENTER), then we display the string. Then we create
             * an AsyncTask that sends the string to the remote AVD.
             */
                    String msg = editText.getText().toString() + "\n";
                    editText.setText(""); // This is one way to reset the input box.
//                    TextView localTextView = (TextView) findViewById(R.id.local_text_display);
//                    localTextView.append("\t" + msg); // This is one way to display a string.
//                    TextView remoteTextView = (TextView) findViewById(R.id.remote_text_display);
//                    remoteTextView.append("\n");
//
            /*
             * Note that the following AsyncTask uses AsyncTask.SERIAL_EXECUTOR, not
             * AsyncTask.THREAD_POOL_EXECUTOR as the above ServerTask does. To understand
             * the difference, please take a look at
             * http://developer.android.com/reference/android/os/AsyncTask.html
             */
                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort);
                    return true;
                }
                return false;
            }
        });
        Log.d("test","After defining send listeners");


        btn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String msg = editText.getText().toString() + "\n";
                editText.setText(""); // This is one way to reset the input box.
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort);

            }
        });
        Log.d("test","After defining  enter listeners");
  //      btn.setOnClickListener(new View.OnClickListener() {
    //        @Override
     //       public void onClick(View v) {
       //             String msg = edtxt.getText().toString() + "\n";
         //           edtxt.setText(""); // This is one way to reset the input box.
           //         new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort);
             //   }
           // });
    }
        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }

    private class ClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {
            //System.out.print(REMOTE_PORT.length);
            for (int i=0;i<REMOTE_PORT.length;i++) {
                try {

                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(REMOTE_PORT[i]));

                    String msgToSend = msgs[0];

                    DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                    dos.writeUTF(msgToSend);

                    dos.flush();
                    dos.close();

                    socket.close();
                } catch (UnknownHostException e) {
                    Log.e(TAG, "ClientTask UnknownHostException");
                } catch (IOException e) {
                    Log.e(TAG, "ClientTask socket IOException "+e.getLocalizedMessage());
                }
            }

            return null;
        }
    }
    private Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }

    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];
            DataInputStream dis = null;
            Socket socket = null;
            Uri uri = null;
            Integer seq=0;
            try {
                while(true) {
                    socket = serverSocket.accept();
                    dis = new DataInputStream(socket.getInputStream());
                    String str;
                    str = dis.readUTF();
                    uri =buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger1.provider");
                    ContentValues cntval = new ContentValues();
                    cntval.put("key", seq.toString());
                    cntval.put("value", str);

                    getContentResolver().insert(uri, cntval);

                    seq++;
                    publishProgress(str);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        protected void onProgressUpdate(String...strings) {

            String strReceived = strings[0].trim();
            TextView tvobj = (TextView) findViewById(R.id.textView1);
            tvobj.append(strReceived + "\t\n");

            String filename = "SimpleMessengerOutput";
            String string = strReceived + "\n";
            FileOutputStream outputStream;

            try {
                outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
                outputStream.write(string.getBytes());
                outputStream.close();
            } catch (Exception e) {
                Log.e(TAG, "File write failed");
            }

            return;
        }
    }

}
