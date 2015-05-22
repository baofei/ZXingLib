package com.zxing.qrcodelib.decode;


import com.zxing.qrcodelib.result.QrcodeResult;

public interface  OnDecodeCallBack {
	boolean onDecodeCallBack(boolean isSuc, QrcodeResult result);
}
