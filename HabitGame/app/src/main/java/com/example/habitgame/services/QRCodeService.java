package com.example.habitgame.services;

import android.graphics.Bitmap;
import android.graphics.Color;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.QRCodeWriter;

public class QRCodeService {
    public Bitmap generateQRCode(String email) {
        QRCodeWriter writer = new QRCodeWriter();
        try {
            String deepLink = "habitgame://addfriend?email=" + email;
            com.google.zxing.common.BitMatrix bitMatrix = writer.encode(deepLink, BarcodeFormat.QR_CODE, 512, 512);

            Bitmap bitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.RGB_565);
            for (int x = 0; x < 512; x++) {
                for (int y = 0; y < 512; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            return bitmap;
        } catch (WriterException e) {
            e.printStackTrace();
        }
        return null;
    }
}
