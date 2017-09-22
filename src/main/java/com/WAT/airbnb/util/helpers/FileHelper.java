package com.WAT.airbnb.util.helpers;

import com.WAT.airbnb.etc.Constants;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.util.Base64;

/**
 *  1. Saves the given file objects to the filesystem
 *  2. Converts saved images to base64
 *  @author Kostas Kolivas
 *  @version 1.0
 */

// TODO: CHANGED
// Added boolean main parameter (only true if it's the main picture)
public class FileHelper {
    static public String saveFile(InputStream uploadedInputStream, int id,
                                  FormDataContentDisposition fileDetails,
                                  boolean user, boolean main) throws IOException {
        String fileUrl = null;
        String[] split = fileDetails.getFileName().split("\\.");

        // TODO: CHANGED
        // Fixed url system
        if (user) {
            fileUrl = Constants.DIR + "/img/users/" + id;
        } else {
            fileUrl = Constants.DIR + "/img/houses/" + id + "/" + System.currentTimeMillis();
        }

        fileUrl += "." + split[split.length - 1];

        OutputStream out = null;
        try {
            out = new FileOutputStream(new File(fileUrl));
            int read;
            byte[] bytes = new byte[1024];
            while ((read = uploadedInputStream.read(bytes)) != -1) {
                out.write(bytes, 0, read);
            }

            // Also save a thumbnail if it's a house picture
            if (!user && main) {
                saveFileThumb(fileUrl, id);
            }

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

    // TODO: CHANGED

    // Added id parameter 
    // Removed boolean user parameter 
    static public String saveFileThumb(String localUrl, int id) throws IOException {
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
        String thumbLocalUrl = Constants.DIR + "img/houses/" + id + "/thumb/" + newFileName;
        File thumbFile = new File(thumbLocalUrl);
        if (!thumbFile.getParentFile().exists()) {
            if (!thumbFile.getParentFile().mkdirs()) {
                throw new IOException("Directory could not be created");
            }
        }
        ImageIO.write(img, "jpg", new File(thumbLocalUrl));
        return thumbLocalUrl;
    }
}
