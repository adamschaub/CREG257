package com.example.morebluetoothtesting;

import java.io.File;
import java.io.FileNotFoundException;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;

/* Very simple ContentProvider, for serving up the key/pass files to an email app.
 * Assumes the key/pass are in the file "tmpfile.phnky".
 */

public class KeyPassFileProvider extends ContentProvider {

    @Override
    public boolean onCreate() {
 
        return true;
    }
 
    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode)
            throws FileNotFoundException {

    	File f = new File(getContext().getFilesDir(), "tmpfile.phnky");
    	ParcelFileDescriptor pfd = null;
    	try {
    		ParcelFileDescriptor tmp = ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY);
    		pfd = tmp;
    	} catch (Exception e) { Log.e("Error!", e.toString()); }
    	
    	return pfd;
    }
  
    @Override
    public int update(Uri uri, ContentValues contentvalues, String s,
            String[] as) {
        return 0;
    }
 
    @Override
    public int delete(Uri uri, String s, String[] as) {
        return 0;
    }
 
    @Override
    public Uri insert(Uri uri, ContentValues contentvalues) {
        return null;
    }
 
    @Override
    public String getType(Uri uri) {
        return null;
    }
 
    @Override
    public Cursor query(Uri uri, String[] projection, String s, String[] as1,
            String s1) {
        return null;
    }
}