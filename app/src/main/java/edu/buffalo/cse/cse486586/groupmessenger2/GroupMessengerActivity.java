package edu.buffalo.cse.cse486586.groupmessenger2;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

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
    static int sequence=0;
    int message_num=0;
    int process_num;
    int prop_prio=0;
    int agree_prio=0;
    int pro_no=0;
    static Map<Integer, Integer> message_count = new HashMap<Integer, Integer>();
    static Map<Integer, Integer> message_priority = new HashMap<Integer, Integer>();
    static Map<String,message> id_checker = new HashMap <String, message>();
    static PriorityQueue<message> queue = new PriorityQueue<message>(100,new priority_comparator());
    private String my_avd_Port;

    public static class message {
        String text_input,sender,message_type,unique_num;
        int message_no,process_no,priority_no;
        boolean agreement;

        public message(String text_input,String sender,String message_type,String unique_num,int message_no,int process_no,int priority_no,boolean agreement){
            this.text_input = text_input;
            this.sender = sender;
            this.message_type = message_type;
            this.unique_num = unique_num;
            this.message_no=message_no;
            this.process_no=process_no;
            this.priority_no = priority_no;
            this.agreement = agreement;
        }
    }

    public static class priority_comparator implements Comparator<message> {

        public int compare (message a, message b){
            if(a.priority_no>b.priority_no)
                return 1;
            else if (a.priority_no<b.priority_no)
                return -1;
            else
            {
                if(a.process_no>b.process_no)
                    return 1;
                else
                    return -1;
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

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
        //code takn for simple messenger
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));

        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));
        my_avd_Port = myPort;
        try {

            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);

            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
         } catch (IOException e) {
            Log.e(TAG, "Can't create a ServerSocket");
            return;
         }


        final EditText editText = (EditText) findViewById(R.id.editText1);

        findViewById(R.id.button4).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String msg = editText.getText().toString();
                editText.setText(""); // This is one way to reset the input box
                TextView localTextView = (TextView) findViewById(R.id.local_text_display);
                localTextView.append("\t" + msg); // This is one way to display a string.
                TextView remoteTextView = (TextView) findViewById(R.id.remote_text_display);
                remoteTextView.append("\n");
                String msg_type = "message";
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort,msg_type);
            }
        });
        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }
       //code taken from onPclicklistener
       private Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }



    //code taken from simplemessenger
    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];
            String inputLine;

            //the below code has been taken from https://docs.oracle.com/javase/tutorial/networking/sockets/index.html
        while(true)
         {
            try {

                Socket clientSocket = serverSocket.accept();
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                Log.d(TAG, "server create a ServerSocket");

                inputLine = in.readLine();
                if(inputLine!=null) {
                    publishProgress(inputLine);
                }
                in.close();
                clientSocket.close();

            } catch (UnknownHostException e) {
                Log.e(TAG, "ServerTask UnknownHostException");
                break;
            } catch (IOException e) {
                Log.e(TAG, "ServerTask socket IOException");
                break;
            }

         }


            /*
            * TODO: Fill in your server code that receives messages and passes them
             * to onProgressUpdate().

             */

            return null ;
        }

        protected void onProgressUpdate(String...strings) {


                String[] message_contents = strings[0].split(",");

            message new_object = new message(message_contents[0],message_contents[1],message_contents[2],message_contents[3],
                    Integer.parseInt(message_contents[4]),Integer.parseInt(message_contents[5]),
                    Integer.parseInt(message_contents[6]),Boolean.parseBoolean(message_contents[7]));
            if(message_contents[2].equals("message")){

                prop_prio=Math.max(prop_prio,agree_prio)+1;
                new_object.priority_no = prop_prio;
                new_object.message_type = "proposal";

               // for(int i=0;i<5;i++){
                 //   if(my_avd_Port.equals(REMOTE_PORT[i]))
                  //      new_object.process_no = i+1;

                //}


                queue.add(new_object);
                id_checker.put(new_object.unique_num,new_object);
                Log.d(TAG, "server send for proposal");

                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, new_object.text_input, new_object.sender,
                        new_object.message_type,new_object.unique_num,Integer.toString(new_object.message_no),
                        Integer.toString(new_object.process_no),Integer.toString(new_object.priority_no),
                        Boolean.toString(new_object.agreement));

            }

            else if(message_contents[2].equals("proposal")){



                if(message_count.containsKey(new_object.message_no)){

                    if(new_object.priority_no>message_priority.get(new_object.message_no)) {
                        message_priority.put(new_object.message_no, new_object.priority_no);
                        //pro_no = new_object.process_no;
                    }

                    int counter = message_count.get(new_object.message_no);
                    counter++;
                    message_count.put(new_object.message_no,counter);
                }

                else
                {
                    message_count.put(new_object.message_no,1);
                    message_priority.put(new_object.message_no,new_object.priority_no);

                }

                if(message_count.get(new_object.message_no)==5)
                {
                    new_object.message_type = "agreement";
                    //new_object.process_no = pro_no;
                    new_object.priority_no = message_priority.get(new_object.message_no);
                    new_object.agreement = true;
                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, new_object.text_input, new_object.sender,
                            new_object.message_type, new_object.unique_num, Integer.toString(new_object.message_no),
                            Integer.toString(new_object.process_no), Integer.toString(new_object.priority_no),
                            Boolean.toString(new_object.agreement));
                }


            }

            else if(message_contents[2].equals("agreement")){
                 agree_prio = Math.max(agree_prio,Integer.parseInt(message_contents[6]));
                 queue.remove(id_checker.get(new_object.unique_num));
                 id_checker.remove(new_object.unique_num);
                 queue.add(new_object);

            }

            //if(!queue.isEmpty()){

                while(!queue.isEmpty() && queue.peek().agreement==true ){
                    Log.d(TAG, "entered while loop");
                    String strReceived = queue.poll().text_input;
                    TextView remoteTextView = (TextView) findViewById(R.id.remote_text_display);
                    remoteTextView.append(strReceived + "\t\n");
                    TextView localTextView = (TextView) findViewById(R.id.local_text_display);
                    localTextView.append("\n");

                    Uri providUri = buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger2.provider");
                    ContentValues keyValueToInsert = new ContentValues() ;
                    keyValueToInsert.put("key", Integer.toString(sequence)) ;
                    keyValueToInsert.put("value",strReceived ) ;
                    getContentResolver().insert(providUri,keyValueToInsert ) ;
                    sequence++;


                }


            //}


           return;
        }
    }

    //code taken from simplemessenger

    private class ClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {

            String category = msgs[2];


            try {


                if(category.equals("message")) {
                    //here the message is multicast to the avds.
                    String my_Port = msgs[1];
                    //this is to check the processNO
                    for(int i=0;i<5;i++){
                        if(my_Port.equals(REMOTE_PORT[i]))
                            process_num = i+1;

                    }

                    message_num++;


                    String unique_num = message_num+"."+process_num;
                    String msgToSend = msgs[0];
                    msgToSend = msgToSend+","+ my_Port+","+"message" +","+unique_num+","+ message_num+","+process_num+","+0+","+false+",\n";

                    for (String port : REMOTE_PORT) {


                         Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                 Integer.parseInt(port));
                        Log.d(TAG, "client create a ServerSocket for message");
                        Log.d(TAG, String.valueOf(msgToSend));

                        //the below has been taken from https://docs.oracle.com/javase/tutorial/networking/sockets/index.html
                        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                        //https://docs.oracle.com/javase/7/docs/api/java/lang/String.html


                        out.print(msgToSend);
                        out.flush();

                    }
                }

               else if(category.equals("proposal")) {



                       //this part sends the proposal to the avd that has sent the message
                       String port_no = msgs[1];
                       String msgToSend = msgs[0]+","+msgs[1]+","+msgs[2] +","+msgs[3]+","+msgs[4]+","+msgs[5] +","+msgs[6]+","+msgs[7]+",\n";

                         Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                    Integer.parseInt(port_no));
                              Log.d(TAG, "client create a ServerSocket for proposal");
                               Log.d(TAG, String.valueOf(msgToSend));
                            //the below has been taken from https://docs.oracle.com/javase/tutorial/networking/sockets/index.html
                            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                            out.print(msgToSend);
                            out.flush();
                }

                else if(category.equals("agreement")) {
                         //this part multicast the agrrement message
                    for (String port : REMOTE_PORT) {

                        Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                Integer.parseInt(port));
                        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                        //https://docs.oracle.com/javase/7/docs/api/java/lang/String.html
                        String msgToSend = msgs[0]+","+msgs[1]+","+msgs[2] +","+msgs[3]+","+msgs[4]+","+msgs[5] +","+msgs[6]+","+msgs[7]+",\n";

                        Log.d(TAG, "client create a ServerSocket for agreement");
                         Log.d(TAG, String.valueOf(msgToSend));
                        out.print(msgToSend);
                        out.flush();

                    }

                }


            } catch (UnknownHostException e) {
                Log.e(TAG, "ClientTask UnknownHostException");
            } catch (IOException e) {
                Log.e(TAG   , "ClientTask socket IOException");
            }

            return null;
        }
    }


}
