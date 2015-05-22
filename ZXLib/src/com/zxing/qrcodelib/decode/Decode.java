package com.zxing.qrcodelib.decode;

import java.io.ByteArrayOutputStream;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;

import android.graphics.Bitmap;
import android.os.AsyncTask;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.DecodeHintType;
import com.google.zxing.FormatException;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.ResultPointCallback;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import com.zxing.qrcodelib.camera.CameraManager;
import com.zxing.qrcodelib.result.QrcodeResult;

public class Decode {
	
	private final MultiFormatReader multiFormatReader;
	
	private final Map<DecodeHintType,Object> hints;
	private CameraManager cameraManager;
	private boolean isRun = false;
	private boolean stop = true;
	private OnDecodeCallBack mOnDecodeCallBack;
	private DecodeTask mDecodeTask;
	public Decode(CameraManager cameraManager, ResultPointCallback resultPointCallback) {
		this.cameraManager = cameraManager;
		multiFormatReader = new MultiFormatReader();
		hints = new EnumMap<DecodeHintType, Object>(DecodeHintType.class);
		EnumSet<BarcodeFormat> decodeFormats = EnumSet.noneOf(BarcodeFormat.class);
		decodeFormats.addAll(DecodeFormatManager.PRODUCT_FORMATS);
		decodeFormats.addAll(DecodeFormatManager.INDUSTRIAL_FORMATS);
		decodeFormats.addAll(DecodeFormatManager.QR_CODE_FORMATS);
		decodeFormats.addAll(DecodeFormatManager.DATA_MATRIX_FORMATS);
	    hints.put(DecodeHintType.POSSIBLE_FORMATS, decodeFormats);
	    hints.put(DecodeHintType.NEED_RESULT_POINT_CALLBACK, resultPointCallback);
	    multiFormatReader.setHints(hints);
	}


	public OnDecodeCallBack getOnDecodeCallBack() {
		return mOnDecodeCallBack;
	}

	public void setDecodeCallBack(OnDecodeCallBack onDecodeCallBack) {
		this.mOnDecodeCallBack = onDecodeCallBack;
	}

	/**
     * Decode the data within the viewfinder rectangle, and time how long it
     * took. For efficiency, reuse the same reader objects from one decode to
     * the next.
     * 
     * @param data The YUV preview frame.
     * @param width The width of the preview frame.
     * @param height The height of the preview frame.
     */
    public QrcodeResult decode(byte[] data, int width, int height) {
    	if(data == null){
    		return null;
    	}
        Result rawResult = null;
        // modify here
        byte[] rotatedData = new byte[data.length];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++)
                rotatedData[x * height + height - y - 1] = data[x + y * width];
        }
        int tmp = width; // Here we are swapping, that's the difference to #11
        width = height;
        height = tmp;
        PlanarYUVLuminanceSource source = cameraManager.buildLuminanceSource(rotatedData,
                width, height);
        if(source == null){
        	return null;
        }
        BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(source));      
        try {
            rawResult = multiFormatReader.decodeWithState(binaryBitmap);
        } catch (ReaderException re) {
            // continue
        } finally {
            multiFormatReader.reset();
        }
        return qrcodeResult(source, rawResult);
    }
    public QrcodeResult decode(Bitmap bitmap) {
    	if(bitmap == null){
    		return null;
    	}
    	Result rawResult = null;
    	int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
        RGBLuminanceSource source = new RGBLuminanceSource(width, height, pixels);
        BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(source));
        QRCodeReader reader = new QRCodeReader();
        try {
			rawResult =  reader.decode(binaryBitmap, hints);
		} catch (NotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ChecksumException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        return qrcodeResult(source, rawResult, bitmap);
    }
    
    private QrcodeResult qrcodeResult(PlanarYUVLuminanceSource source, Result rawResult) {
    	QrcodeResult result = null;
    	if(rawResult != null){
	    	result = new QrcodeResult();
	    	int[] pixels = source.renderThumbnail();
	        int width = source.getThumbnailWidth();
	        int height = source.getThumbnailHeight();
	        Bitmap bitmap = Bitmap.createBitmap(pixels, 0, width, width, height, Bitmap.Config.ARGB_8888);
	        ByteArrayOutputStream out = new ByteArrayOutputStream();    
	        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, out);
	        result.bitmap = out.toByteArray();
	        result.scaleFactor = (float) width / source.getWidth();
	        result.source = source;
	    	result.result = rawResult;
    	}
        return result;
    }
    
    private QrcodeResult qrcodeResult(RGBLuminanceSource source, Result rawResult, Bitmap bitmap) {
    	QrcodeResult result = null;
    	if(rawResult != null){
	    	result = new QrcodeResult();
	    	ByteArrayOutputStream out = new ByteArrayOutputStream();    
		    bitmap.compress(Bitmap.CompressFormat.JPEG, 50, out);
	        result.bitmap = out.toByteArray();
	        result.scaleFactor = 1.0f;
	        result.source = source;
	    	result.result = rawResult;
    	}
        return result;
    }
    /**
     * 异步解码
     * @param data
     * @param width
     * @param height
     */
    public void decodeQrcode(byte[] data, int width, int height){
    	if(mOnDecodeCallBack != null && !stop && !isRun){
    		isRun = true;
    		mDecodeTask = new DecodeTask(data, width, height);
    		mDecodeTask.execute();
    	}
    }
    public void start(){
    	stop = false;
    	isRun = false;
    }
    public void stop(){
    	stop = true;
    	if(mDecodeTask != null && !mDecodeTask.isCancelled()){
    		mDecodeTask.cancel(true);
    	}
    }
    
    class DecodeTask extends AsyncTask<String, Boolean, QrcodeResult>{

    	private byte[] data;
    	private int width;
    	private int height;
    	
		public DecodeTask(byte[] data, int width, int height) {
			super();
			this.data = data;
			this.width = width;
			this.height = height;
		}

		@Override
		protected QrcodeResult doInBackground(String... arg0) {
			return decode(data, width, height);
		}
    	
		@Override
		protected void onPostExecute(QrcodeResult result) {
			if(mOnDecodeCallBack != null && !stop){
				stop = mOnDecodeCallBack.onDecodeCallBack(result == null ? false : true,
						result);
			}
			isRun = false;
			super.onPostExecute(result);
		}
    }
}
