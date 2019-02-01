package com.tomatocurry1.active_assistant.voice_manager;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.content.ContentResolver;
import android.os.Build;
import android.os.CancellationSignal;
import android.os.Handler;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.util.Log;


public class CallManager {

    String phoneNumber;

    ContentResolver contentResolver;
    Handler handler = new Handler();
    Context parentContext;

    class CallRunner implements Runnable{

        private final String phoneNumber;
        private final CancellationSignal cancellationSignal;

        private CallRunner(String number, CancellationSignal cancel){
            phoneNumber = number;
            cancellationSignal = cancel;

        }

        @Override
        public void run() {
            if (phoneNumber!=null && !cancellationSignal.isCanceled()){

                Intent intent = new Intent(Intent.ACTION_CALL);

                intent.setData(Uri.parse("tel:" + phoneNumber));
                try {
                    parentContext.startActivity(intent);
                }catch (SecurityException e){

                }

            }
            if(cancellationSignal.isCanceled())
                Log.println(Log.INFO, "Call", "Call cancelled via cancellation signal");
        }
    }

    public boolean readyCall(Context context, String callName, CancellationSignal cancellationSignal){
        parentContext = context;
        if (Build.VERSION.SDK_INT < 26)
            return false;

        contentResolver = context.getContentResolver();

        Cursor cursor = contentResolver.query(Contacts.CONTENT_URI, new String[]{Contacts._ID, Contacts.DISPLAY_NAME, Contacts.HAS_PHONE_NUMBER}, null, null);
        if (cursor != null && cursor.getCount() != 0){
            while (cursor.moveToNext()){
                String id = cursor.getString(cursor.getColumnIndex(Contacts._ID));
                String name = cursor.getString(cursor.getColumnIndex(Contacts.DISPLAY_NAME));
                if(callName.equalsIgnoreCase(name)){
                    if(cursor.getInt(cursor.getColumnIndex(Contacts.HAS_PHONE_NUMBER)) > 0){
                        Cursor phoneNumberCursor = contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?", new String[]{id}, null);
                        if(phoneNumberCursor.moveToNext()){
                            phoneNumber = phoneNumberCursor.getString(phoneNumberCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                            break;
                        }
                        phoneNumberCursor.close();

                    }
                }
            }
            cursor.close();
        }

        if(phoneNumber == null)
            return false;

        final CallRunner runner = new CallRunner(phoneNumber, cancellationSignal);
        handler.postDelayed(runner, 5000);



        //Not sure if this is necessary. Perhaps it would be preferable to see the runner run a cancel method?
        cancellationSignal.setOnCancelListener(new CancellationSignal.OnCancelListener() {
            @Override
            public void onCancel() {
                handler.removeCallbacks(runner);
            }
        });


        return true;
    }
}
