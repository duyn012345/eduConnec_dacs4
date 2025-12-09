//package com.example.demo1;
//
//import javax.imageio.ImageIO;
//import java.awt.image.BufferedImage;
//import java.io.ByteArrayOutputStream;
//import java.io.IOException;
//
//public class ImageUtils {
//
//    // Chuyển BufferedImage → byte JPEG
//    public static byte[] bufferedImageToJpegBytes(BufferedImage image, float quality) {
//        try {
//            ByteArrayOutputStream baos = new ByteArrayOutputStream();
//            ImageIO.write(image, "jpg", baos); // dùng JPG thay vì PNG
//            baos.flush();
//            byte[] bytes = baos.toByteArray();
//            baos.close();
//            return bytes;
//        } catch (IOException e) {
//            e.printStackTrace();
//            return new byte[0];
//        }
//    }
//}
package org.example.educonnec_dacs4.utils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ImageUtils {

    public static byte[] bufferedImageToJpegBytes(BufferedImage image) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "jpg", baos);
            baos.flush();
            byte[] bytes = baos.toByteArray();
            baos.close();
            return bytes;
        } catch (IOException e) {
            e.printStackTrace();
            return new byte[0];
        }
    }
}
