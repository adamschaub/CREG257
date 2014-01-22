
package com.example.morebluetoothtesting;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

public class BluetoothConnection {
	
	private String CLASS_NAME = this.getClass().getSimpleName();
	
	private static BluetoothConnection instance = null;

	public ConnectedThread mConnectedThread;
    private AcceptThread mInsecureAcceptThread;
	
	private int mState;

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device
    
    private int response = RESPONSE_NONE;
    
    public static final int RESPONSE_NONE = 0;
    public static final int RESPONSE_NAK = 1;
    public static final int RESPONSE_ACK = 2;
    
    public static synchronized BluetoothConnection getInstance() {
		if (instance == null)
			instance = new BluetoothConnection();
		return instance;
	}

	/**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     * @param socket  The BluetoothSocket on which the connection was made
     * @param device  The BluetoothDevice that has been connected
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice
            device, final String socketType) {

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket, socketType);
        mConnectedThread.start();

        // Send the name of the connected device back to the UI Activity
        Log.v(CLASS_NAME, "YEAH IT DID");
        
        setState(STATE_CONNECTED);
    }
    
    /**
     * Start the chat service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume() */
    public synchronized void start() {
    	Log.v(CLASS_NAME, "Starting AcceptThread");
    	setState(STATE_LISTEN);
    	mInsecureAcceptThread = new AcceptThread(false);
    	mInsecureAcceptThread.start();
    }
    
    /**
     * Set the current state of the chat connection
     * @param state  An integer defining the current connection state
     */
    private synchronized void setState(int state) {
        mState = state;
    }

    /**
     * Return the current connection state. */
    public synchronized int getState() {
        return mState;
    }
    
    private synchronized void setResponse(int response) {
    	this.response = response;
    }
    
    public synchronized int getResponse() {
    	int tmpResponse = response;
    	setResponse(RESPONSE_NONE);
    	return tmpResponse;
    }

    /**
     * Stop all threads
     */
    public synchronized void stop() {
    	if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        setState(STATE_NONE);
    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner
     * @param out The bytes to write
     * @see ConnectedThread#write(byte[])
     * Returns boolean, true=write succeeded, false=didn't
     */
    public boolean write(byte[] out) {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return false;
            r = mConnectedThread;
        }
        // Perform the write unsynchronized
        r.write(out);
        return true;
    }
    
    /**
     * Write to the ConnectedThread in an unsynchronized manner
     * Padded out to 32 bytes
     * @param out The bytes to write
     * @see ConnectedThread#write(byte[])
     */
    public void write32(byte[] out) {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        // Perform the write unsynchronized
        r.write(out);
        byte padding[] = new byte[32 - out.length];
        Arrays.fill(padding, (byte) 0);
        r.write(padding);
    }
    

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void connectionFailed() {
        // Start the service over to restart listening mode
        //connect();
    }
    
    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost() {
        // Start the service over to restart listening mode
        BluetoothConnection.this.start();
    }
    
    /**
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side client. It runs until a connection is accepted
     * (or until cancelled).
     */
    private class AcceptThread extends Thread {
        // The local server socket
        private final BluetoothServerSocket mmServerSocket;
        private String mSocketType;

        public AcceptThread(boolean secure) {
            BluetoothServerSocket tmp = null;
            mSocketType = secure ? "Secure":"Insecure";

            // Create a new listening server socket
            try {
                if (secure) {
                    tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord("MyNameSecure",
                    		UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66"));
                } else {
                    tmp = mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(
                    		"MyNameInsecure", UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"));
                }
            } catch (IOException e) {
                Log.e("Stuff", "Socket Type: " + mSocketType + "listen() failed", e);
            }
            mmServerSocket = tmp;
            if (mmServerSocket != null) {
            	Log.v(CLASS_NAME, "Got a socket...");
            }
        }
       
        public void run() {
            Log.v(CLASS_NAME, "run");
            setName("AcceptThread" + mSocketType);

            BluetoothSocket socket = null;

            // Listen to the server socket if we're not connected
            while (mState != STATE_CONNECTED) {
                try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                	Log.v(CLASS_NAME, "Before accept");
                    socket = mmServerSocket.accept();
                    Log.v(CLASS_NAME, "After accept");
                } catch (IOException e) {
                    Log.e("Stuff", "Socket Type: " + mSocketType + "accept() failed", e);
                    break;
                }

                // If a connection was accepted
                if (socket != null) {
                    synchronized (BluetoothConnection.this) {
                        switch (mState) {
                        case STATE_LISTEN:
                        	Log.v(CLASS_NAME, "Listening");
                        case STATE_CONNECTING:
                        	Log.v(CLASS_NAME, "Connecting");
                            // Situation normal. Start the connected thread.
                            connected(socket, socket.getRemoteDevice(),
                                    mSocketType);
                            break;
                        case STATE_NONE:
                        	Log.v(CLASS_NAME, "None");
                        case STATE_CONNECTED:
                        	Log.v(CLASS_NAME, "Connected");
                            // Either not ready or already connected. Terminate new socket.
                            try {
                                socket.close();
                            } catch (IOException e) {
                                Log.e(CLASS_NAME, "Could not close unwanted socket", e);
                            }
                            break;
                        }
                    }
                }
            }
        }

        public void cancel() {
            Log.d("Stuff", "Socket Type" + mSocketType + "cancel " + this);
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e("Stuff", "Socket Type" + mSocketType + "close() of server failed", e);
            }
        }
    }
	
	/**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket, String socketType) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e("Exception!", "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }
        
        public void run() {
        	String inStr;
            byte[] buffer = new byte[1024];
            int bytes;
            // Keep listening to the InputStream while connected
            while (true) {
                try {
                    // Read from the InputStream
                	if (mmInStream.available() > 0) {
                		bytes = mmInStream.read(buffer);
	                    if (bytes > 0) {
	                    	inStr = new String(buffer, 0, bytes, "ASCII");
	                    	Log.v("BT rcvd:", new String(buffer, 0, bytes, "ASCII"));
	                    	if (inStr.contains("ACK") || inStr.contains("NAK")) {
	                    		if (inStr.contains("ACK"))
	                    			setResponse(RESPONSE_ACK);
	                    		else
	                    			setResponse(RESPONSE_NAK);
		                    	synchronized (this) {
		                    		this.notify();
		                    	}
	                    	}
	                    }
                	}
                } catch (IOException e) {
                    Log.e("Exception!", "disconnected", e);
                    connectionLost();
                    // Start the service over to restart listening mode
                    BluetoothConnection.this.start();
                    break;
                }
            }
        }

        /**
         * Write to the connected OutStream.
         * @param buffer  The bytes to write
         */
        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);

            } catch (IOException e) {
                Log.e("Exception!", "Exception during write", e);
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e("Exception!", "close() of connect socket failed", e);
            }
        }
    }
	
	private BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

	public BluetoothConnection() {}
}
