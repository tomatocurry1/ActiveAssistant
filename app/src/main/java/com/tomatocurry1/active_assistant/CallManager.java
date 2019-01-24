package com.tomatocurry1.active_assistant;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.content.ContentResolver;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;


public class CallManager {

    public void makeCall(Context context){




        Intent intent = new Intent(Intent.ACTION_CALL);

        intent.setData(Uri.parse("tel:" + "1234567890"));
        try {

            context.startActivity(intent);
        }catch (SecurityException e){

        }
    }
}
