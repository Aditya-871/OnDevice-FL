package org.tensorflow.lite.examples.transfer;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class ServerConnection {
    private Socket socket;
    private static final int SERVER_PORT = 5000;
    private static final String SERVER_IP = "172.17.40.2";

    // Connect to server
    public void connectToServer() {
        new ConnectTask().execute();
    }

    // Send data to server
    public void sendData(String data) {
        new SendDataTask().execute(data);
    }

    // Receive data from server
    public void receiveData() {
        new ReceiveDataTask().execute();
    }

    private class ConnectTask extends AsyncTask<Void, Void, Void> {
       // Log.i("cnct", "connecting to server");
        @Override
        protected Void doInBackground(Void... params) {
            Log.i("cnct", "called connecttask function");
            try {
                Log.i("cnct", "connecting to server");
                InetAddress serverAddr = InetAddress.getByName(SERVER_IP);
                socket = new Socket(serverAddr, SERVER_PORT);
            } catch (UnknownHostException e1) {
                e1.printStackTrace();
                Log.i("EEE", "some exception");
            } catch (IOException e1) {
                e1.printStackTrace();
                System.out.println("I/O error: " + e1.getMessage());
            }
            return null;
        }
    }

    private class SendDataTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... params) {
            try {
                OutputStream out = socket.getOutputStream();
                PrintWriter output = new PrintWriter(out);
                output.println(params[0]);
                output.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    private class ReceiveDataTask extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void... params) {
            String data = null;
            try {
                InputStream in = socket.getInputStream();
                BufferedReader input = new BufferedReader(new InputStreamReader(in));
                data = input.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return data;
        }

        @Override
        protected void onPostExecute(String data) {
            // Use the received data as needed
        }
    }
}
