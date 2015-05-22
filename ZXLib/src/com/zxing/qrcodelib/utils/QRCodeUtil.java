package com.zxing.qrcodelib.utils;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import com.google.zxing.BarcodeFormat;
import com.zxing.qrcodelib.encode.Contents;
import com.zxing.qrcodelib.encode.EncodeParameters;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.text.TextUtils;

public class QRCodeUtil {
	//保存图片到sd卡中，此方法用于debug
	public static  boolean savePicture(Bitmap bitmap, String name) {
        BitmapFactory.Options op = new BitmapFactory.Options();
        op.inSampleSize = 8; //这个数字越大,图片大小越小.
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(Environment.getExternalStorageDirectory().getPath() + "/"
                    + name + ".jpg");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        if (bitmap != null && fos != null) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            return true;
        }
        return false;
    }
	
	 /**
	  * *根据联系人uri,返回二维码生成参数
	  * 
	  * @param context
	  * @param contactUri A Uri of the form content://contacts/people/17
	  * @param format BarcodeFormat，码的类型，如二维码BarcodeFormat.QR_CODE等
	  * @return
	  */
	  public static EncodeParameters getContactAsBarcode(Context context, Uri contactUri,  String  format) {
	    if (contactUri == null) {
	      return null; // Show error?
	    }
	    ContentResolver resolver = context.getContentResolver();

	    Cursor cursor;
	    try {
	      // We're seeing about six reports a week of this exception although I don't understand why.
	      cursor = resolver.query(contactUri, null, null, null, null);
	    } catch (IllegalArgumentException ignored) {
	      return null;
	    }
	    if (cursor == null) {
	      return null;
	    }

	    String id;
	    String name;
	    boolean hasPhone;
	    try {
	      if (!cursor.moveToFirst()) {
	        return null;
	      }

	      id = cursor.getString(cursor.getColumnIndex(BaseColumns._ID));
	      name = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
	      hasPhone = cursor.getInt(cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)) > 0;


	    } finally {
	      cursor.close();
	    }

	    // Don't require a name to be present, this contact might be just a phone number.
	    Bundle bundle = new Bundle();
	    if (name != null && !name.isEmpty()) {
	      bundle.putString(ContactsContract.Intents.Insert.NAME, massageContactData(name));
	    }

	    if (hasPhone) {
	      Cursor phonesCursor = resolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
	                                           null,
	                                           ContactsContract.CommonDataKinds.Phone.CONTACT_ID + '=' + id,
	                                           null,
	                                           null);
	      if (phonesCursor != null) {
	        try {
	          int foundPhone = 0;
	          int phonesNumberColumn = phonesCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
	          int phoneTypeColumn = phonesCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE);
	          while (phonesCursor.moveToNext() && foundPhone < Contents.PHONE_KEYS.length) {
	            String number = phonesCursor.getString(phonesNumberColumn);
	            if (number != null && !number.isEmpty()) {
	              bundle.putString(Contents.PHONE_KEYS[foundPhone], massageContactData(number));
	            }
	            int type = phonesCursor.getInt(phoneTypeColumn);
	            bundle.putInt(Contents.PHONE_TYPE_KEYS[foundPhone], type);
	            foundPhone++;
	          }
	        } finally {
	          phonesCursor.close();
	        }
	      }
	    }

	    Cursor methodsCursor = resolver.query(ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_URI,
	                                          null,
	                                          ContactsContract.CommonDataKinds.StructuredPostal.CONTACT_ID + '=' + id,
	                                          null,
	                                          null);
	    if (methodsCursor != null) {
	      try {
	        if (methodsCursor.moveToNext()) {
	          String data = methodsCursor.getString(
	              methodsCursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS));
	          if (data != null && !data.isEmpty()) {
	            bundle.putString(ContactsContract.Intents.Insert.POSTAL, massageContactData(data));
	          }
	        }
	      } finally {
	        methodsCursor.close();
	      }
	    }

	    Cursor emailCursor = resolver.query(ContactsContract.CommonDataKinds.Email.CONTENT_URI,
	                                        null,
	                                        ContactsContract.CommonDataKinds.Email.CONTACT_ID + '=' + id,
	                                        null,
	                                        null);
	    if (emailCursor != null) {
	      try {
	        int foundEmail = 0;
	        int emailColumn = emailCursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA);
	        while (emailCursor.moveToNext() && foundEmail < Contents.EMAIL_KEYS.length) {
	          String email = emailCursor.getString(emailColumn);
	          if (email != null && !email.isEmpty()) {
	            bundle.putString(Contents.EMAIL_KEYS[foundEmail], massageContactData(email));
	          }
	          foundEmail++;
	        }
	      } finally {
	        emailCursor.close();
	      }
	    }
	    EncodeParameters par = new EncodeParameters();
	    par.type = Contents.Type.CONTACT;
	    par.data = bundle;
	    if(TextUtils.isEmpty(format)){
	    	par.format = BarcodeFormat.QR_CODE.toString();
	    }else{
	    	par.format =format;
	    }
	    return par;
	  }
	  
	  private static String massageContactData(String data) {
		    // For now -- make sure we don't put newlines in shared contact data. It messes up
		    // any known encoding of contact data. Replace with space.
		    if (data.indexOf('\n') >= 0) {
		      data = data.replace("\n", " ");
		    }
		    if (data.indexOf('\r') >= 0) {
		      data = data.replace("\r", " ");
		    }
		    return data;
		  }
}
