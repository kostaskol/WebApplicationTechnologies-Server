package com.WAT.airbnb.util.helpers;

import com.WAT.airbnb.etc.Constants;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.util.Base64;

public class FileHelper {
    static public String saveFile(InputStream uploadedInputStream, int id,
                                  FormDataContentDisposition fileDetails,
                                  boolean user) throws IOException {
        String fileUrl = null;
        String[] split = fileDetails.getFileName().split("\\.");
        if (user) {
            fileUrl = Constants.DIR + "/img/users/" + id;
        } else {
            fileUrl = Constants.DIR + "/img/houses/" + id;
        }
        File tmpFile = new File(fileUrl);

        for (int i = 0; tmpFile.exists(); i++) {
            System.out.println("Looping " + i);
            if (i < 10) {
                fileUrl = fileUrl.substring(0, fileUrl.length() - 1);
            } else if (i < 100) {
                fileUrl = fileUrl.substring(0, fileUrl.length() - 2);
            }

            fileUrl += i;
        }

        for (String s : split) {
            System.out.println(s);
        }

        System.out.println("Mime: " + split[split.length - 1]);

        fileUrl += "." + split[split.length - 1];

        System.out.println("FileUrl: " + fileUrl);

        OutputStream out = null;
        try {
            out = new FileOutputStream(new File(fileUrl));
            int read;
            byte[] bytes = new byte[1024];
            while ((read = uploadedInputStream.read(bytes)) != -1) {
                out.write(bytes, 0, read);
            }

            saveFileThumb(fileUrl, user);

            return fileUrl;
        } finally {
            if (out != null) {
                out.flush();
                out.close();
            }
        }
    }

    static public String getFileAsString(String localUrl) throws IOException {
        File file = new File(localUrl);
        return Base64.getEncoder().withoutPadding().encodeToString(Files.readAllBytes(file.toPath()));
    }

    static public String saveFileThumb(String localUrl, boolean user) throws IOException {
        if (!user) {
            // House thumb
            BufferedImage img = new BufferedImage(250, 250, BufferedImage.TYPE_INT_RGB);
            img.createGraphics().drawImage(
                    ImageIO.read(new File(localUrl))
                            .getScaledInstance(250, 250, Image.SCALE_SMOOTH),
                    0, 0, null);
            String[] tmp = localUrl.split("/");
            String fileName = tmp[tmp.length - 1];
            tmp = fileName.split("\\.");
            String fileExt = tmp[tmp.length - 1];

            String newFileName = tmp[0] + "_thumb." + fileExt;
            String thumbLocalUrl = Constants.DIR + "/thumbnails/houses/" + newFileName;
            File thumbFile = new File(thumbLocalUrl);
            if (!thumbFile.getParentFile().exists()) {
                if (!thumbFile.getParentFile().mkdirs()) {
                    throw new IOException("Directory could not be created");
                }
            }
            ImageIO.write(img, "jpg", new File(thumbLocalUrl));
            return thumbLocalUrl;
        }
        return null;
    }
}
