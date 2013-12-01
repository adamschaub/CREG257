
package com.example.morebluetoothtesting;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

public class BluetoothConnection {
	
	private ConnectThread mConnectThread;
	private ConnectedThread mConnectedThread;
	
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

	
	/**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     * @param socket  The BluetoothSocket on which the connection was made
     * @param device  The BluetoothDevice that has been connected
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice
            device, final String socketType) {

        // Cancel the thread that completed the connection
        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket, socketType);
        mConnectedThread.start();

        // Send the name of the connected device back to the UI Activity
        Log.v("DID THIS WORK?!", "YEAH IT DID");
        
        setState(STATE_CONNECTED);
    }
    
    /**
     * Start the chat service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume() */
    public synchronized void start() {


    }
    
    /**
     * Set the current state of the chat connection
     * @param state  An integer defining the current connection state
     */
    private synchronized void setState(int state) {
        mState = state;

        // Give the new state to the Handler so the UI Activity can update
        //mHandler.obtainMessage(BluetoothChat.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
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

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

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
     */
    public void write(byte[] out) {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        // Perform the write unsynchronized
        r.write(out);
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
        connect();
    }
    
    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost() {
        // Start the service over to restart listening mode
        BluetoothConnection.this.start();
    }
	
	 /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private String mSocketType;

        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp = null;

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
            	tmp = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
            } catch (IOException e) {
                Log.e("Exception!", "Socket Type: " + mSocketType + "create() failed", e);
            }
            mmSocket = tmp;
        }
        
        public void run() {
            Log.i("Infoooo", "BEGIN mConnectThread SocketType:" + mSocketType);
            setName("ConnectThread" + mSocketType);

            // Always cancel discovery because it will slow down a connection
            //mAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket.connect();
            } catch (IOException e) {
                // Close the socket
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e("Exception!", "unable to close() " + mSocketType +
                            " socket during connection failure", e2);
                }
                Log.e("Exception!", e.toString());
                connectionFailed();
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (BluetoothConnection.this) {
                mConnectThread = null;
            }

            // Start the connected thread
            connected(mmSocket, mmDevice, mSocketType);
        }
        

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e("Exception!", "close() of connect " + mSocketType + " socket failed", e);
            }
        }
    }
	
	/**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
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
	                    	Log.v("BYTES:", new String(buffer, 0, bytes, "ASCII"));
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

	// XXX: Don't extend Thread! Look into runnable/callable
	/*private class ConnectThread extends Thread {
	    private final BluetoothSocket mmSocket;
	    private final BluetoothDevice mmDevice;
	   // private ConnectedThread manageThread;
	 
	    public ConnectThread(BluetoothDevice device) {
	        // Use a temporary object that is later assigned to mmSocket,
	        // because mmSocket is final
	        BluetoothSocket tmp = null;
	        mmDevice = device;
	 
	        // Get a BluetoothSocket to connect with the given BluetoothDevice
	        try {
	            // MY_UUID is the app's UUID string, also used by the server code
	            tmp = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
	        } catch (IOException e) { Log.e("Exception!", e.toString()); }
	        mmSocket = tmp;
	    }

	    public void run() {
	        try {
	            // Connect the device through the socket. This will block
	            // until it succeeds or throws an exception
	            mmSocket.connect();
	        } catch (IOException connectException) {
	            // Unable to connect; close the socket and get out
	            try {
	                mmSocket.close();
	            } catch (IOException closeException) { Log.e("Exception!", closeException.toString());  }
	            return;
	        }
	
	        // Do work to manage the connection (in a separate thread)
	        manageThread = new ConnectedThread(mmSocket);
	        manageThread.run();
	    }
	
	    /** Will cancel an in-progress connection, and close the socket *
	    public void cancel() {
	        try {
	            mmSocket.close();
	        } catch (IOException e) { }
	    }
	}*/

	/*private class ConnectedThread extends Thread {
	    private final BluetoothSocket mmSocket;
	    private final InputStream mmInStream;
	    private final DataInputStream mmDataInStream;
	    private final OutputStream mmOutStream;
	 
	    public ConnectedThread(BluetoothSocket socket) {
	        mmSocket = socket;
	        InputStream tmpIn = null;
	        OutputStream tmpOut = null;
	 
	        // Get the input and output streams, using temp objects because
	        // member streams are final
	        try {
	            tmpIn = socket.getInputStream();
	            tmpOut = socket.getOutputStream();
	        } catch (IOException e) { Log.e("Exception!", e.toString()); }
	 
	        mmInStream = tmpIn;
	        mmDataInStream = new DataInputStream(mmInStream);
	        mmOutStream = tmpOut;
	    }
	 
	    public void run() {
	        byte[] buffer = new byte[1024];  // buffer store for the stream
	        int bytes; // bytes returned from read()
	
	        Log.v("Clearly something is wrong", "...");
	        // Keep listening to the InputStream until an exception occurs
	        while (true) {
	            try {
	                // Read from the InputStream
	            	if (mmInStream.available() > 0) {
	            		//bytes = mmInStream.read(buffer);
	            		mmDataInStream.readFully(buffer, 0, 11);
	            		//Log.v("Read:", bytes+"");
	            		Log.v("Got: ", new String (buffer, "ASCII"));
	            	}
	                
	            } catch (IOException e) {
	            	Log.e("Exception!", e.toString());
	                break;
	            }
	        }
	    }

	    /* Call this from the main activity to send data to the remote device *
	    public void write(byte[] bytes) {
	        try {
	            mmOutStream.write(bytes);
	        } catch (IOException e) { Log.e("Exception!", e.toString()); }
	    }
	 
	    /* Call this from the main activity to shutdown the connection *
	    public void cancel() {
	        try {
	            mmSocket.close();
	        } catch (IOException e) { Log.e("Exception!", e.toString()); }
	    }
	}*/
	
	private BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
	private BluetoothDevice targetDevice = null;
	
	String deviceName;

	public BluetoothConnection(String deviceName) {
		this.deviceName = deviceName;
	}
	
	// Create a BroadcastReceiver for bluetooth related checks
	/*	public final BroadcastReceiver mReceiver = new BroadcastReceiver() {
		    public void onReceive(Context context, Intent intent) {
		    	Log.v("Holy shit!", "Found something!");
		        String action = intent.getAction();

		        //We don't want to reconnect to already connected device
		        if(isConnected==false){
		            // When discovery finds a device
		            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
		            	Log.v("Holy shit!", "Gonna try connecting!");
		                // Get the BluetoothDevice object from the Intent
		                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

		                // Check if the found device is one we had comm with
		                if(device.getAddress().equals(partnerDevAdd)==true) {
		                    ConnectThread thread = new ConnectThread(device);
		                    thread.run();
		                }
		            }
		        }

		        if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
		            // Get the BluetoothDevice object from the Intent
		            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

		            // Check if the connected device is one we had comm with
		            if(device.getAddress().equals(partnerDevAdd)==true)
		                isConnected=true;
		            
		            Log.v("Holy shit!", "Connected! isConnected=" + isConnected);
		            byte t[] = {'a', 'b', 'c', 0};
		    		//if  (!connectThread.write(t)) {
		    		//	Log.v("Well shit", "that didnt work");
		    		//}
		            if (manageThread != null)
		            	Log.v("manageThread = " , manageThread.toString());
		            else
		            	Log.v("what the hell", "its still null");
		        }else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
		            // Get the BluetoothDevice object from the Intent
		            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

		            // Check if the connected device is one we had comm with
		            if(device.getAddress().equals(partnerDevAdd)==true)
		                isConnected=false;
		            
		            Log.v("Holy shit!", "Disconnected! isConnected=" + isConnected);
		        }
		    }
		};*/

	/* Assumes that device `deviceName` has already been paired. */
	public void connect () {
		if (mBluetoothAdapter == null) {
		    Log.e("Error!", "Bluetooth is disabled!");
		}
	
		Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
		// If there are paired devices
		if (pairedDevices.size() > 0) {
		    // Loop through paired devices
		    for (BluetoothDevice device : pairedDevices) {
		        // This is the device we are looking for!
		        if (device.getName().equals(deviceName)) {
		        	Log.v("Good!", "Found our device!");
		        	targetDevice = device;
		        	break;
		        }
		    }
		}
	
		if (targetDevice == null) {
			Log.e("Error!", "Couldn't find target device!");
			return;
		}
	
		// Cancel any thread attempting to make a connection
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null; }
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(targetDevice);//, secure);
        mConnectThread.start();
        setState(STATE_CONNECTING);
        while(getState() != STATE_CONNECTED);
        Log.v("Done", "DOOOONE");
        
		//connectThread = new ConnectThread(targetDevice);
		//connectThread.start();
		//isConnected = true;
		//AcceptThread acceptThread = new AcceptThread();
		//acceptThread.run();
	}

	//private String partnerDevAdd="00:06:66:63:A7:0E";
	//private boolean isConnected=false;
}
