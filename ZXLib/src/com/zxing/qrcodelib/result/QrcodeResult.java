package com.zxing.qrcodelib.result;

import java.io.Serializable;

import com.google.zxing.LuminanceSource;
import com.google.zxing.Result;

public class QrcodeResult implements Serializable{

	public static final long serialVersionUID = -4130458862988922323L;
	
	public Result result;
	public LuminanceSource source;
	public byte[] bitmap;
	public float scaleFactor;
}
