package com.yorhp.luckmoney.util;

import android.graphics.Bitmap;
import android.os.Environment;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.xml.xpath.XPath;

public class FileUtil {


    //将Bitmap图片保存到sd卡

    protected void saveBitmapToSD(Bitmap bt) {
        File path = Environment.getExternalStorageDirectory();
        File file = new File(path, System.currentTimeMillis() + ".jpg");
        System.out.println(Environment.getExternalStorageState() + "/Cool/" +"000000000000000000000000000");
        try {
            FileOutputStream  out = new FileOutputStream(file);
            bt.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * 保存bitmap到SD卡
     * @param bitName 保存的名字
     * @param mBitmap 图片对像
     * return 生成压缩图片后的图片路径
     */
    public static String saveMyBitmap(String bitName,Bitmap mBitmap) {
        File f = new File("/sdcard/" + bitName + ".png");
        try {
            f.createNewFile();
        } catch (IOException e) {
            System.out.println("在保存图片时出错：" + e.toString());
        }
        FileOutputStream fOut = null;
        try {
            fOut = new FileOutputStream(f);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        try {
            mBitmap.compress(Bitmap.CompressFormat.PNG, 100, fOut);
        } catch (Exception e) {
            return "create_bitmap_error";
        }
        try {
            fOut.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            fOut.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "/sdcard/" + bitName + ".png";
    }

    /**
     * 保存bitmap到SD卡
     * @param bitmap
     * @param imagename
     */
    public static String saveBitmapToSDCard(Bitmap bitmap, String imagename) {
        String root = "/sdcard/aaaa/";
        new File(root).mkdirs();
        String path = root + "img-" + imagename + ".jpg";
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(path);
            if (fos != null) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
                fos.close();
            }

            return path;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
