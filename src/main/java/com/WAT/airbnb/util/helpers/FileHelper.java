package com.WAT.airbnb.util.helpers;

import com.WAT.airbnb.etc.Constants;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

/**
 *  1. Saves the given file objects to the filesystem
 *  2. Converts saved images to base64
 *  @author Kostas Kolivas
 *  @version 1.0
 */
public class FileHelper {

    /**
     *
     * @param uploadedInputStream The input stream of the picture
     * @param id The ID of the current house
     * @param fileDetails Various details about the house (mainly the name)
     * @param user Whether or not we are saving a picture for a user (changes the output
     *             directory and format)
     * @return The URL in the Local File System where the new picture has been saved
     * @throws Exception
     */
    static public String saveFile(InputStream uploadedInputStream, int id,
                                  FormDataContentDisposition fileDetails,
                                  boolean user) throws Exception {
        String fileUrl = null;
        String[] split = fileDetails.getFileName().split("\\.");

        if (user) {
            fileUrl = Constants.DIR + "/img/users/" + id;
        } else {
            fileUrl = Constants.DIR + "/img/houses/" + id + "/" + System.currentTimeMillis();
        }

        fileUrl += "." + split[split.length - 1];

        OutputStream out = null;
        try {
            String leadingUrl = getLeadingPath(fileUrl);
            Path leadingPath = Paths.get(leadingUrl);
            if (!Files.exists(leadingPath)) {
                try {
                    Files.createDirectories(leadingPath);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            out = new FileOutputStream(new File(fileUrl));
            int read;
            byte[] bytes = new byte[1024];
            while ((read = uploadedInputStream.read(bytes)) != -1) {
                out.write(bytes, 0, read);
            }

            return fileUrl;
        } finally {
            if (out != null) {
                out.flush();
                out.close();
            }
        }
    }

    /**
     * Returns the picture specified by localUrl in base64 format
     * @param localUrl The URL of the picture in the local File System
     * @return The picture in base64 encoding
     * @throws IOException
     */
    static public String getFileAsString(String localUrl) throws IOException {
        File file = new File(localUrl);
        return Base64.getEncoder().withoutPadding().encodeToString(Files.readAllBytes(file.toPath()));
    }

    /**
     * Converts the photograph in the specified local URL to a 250x250 thumbnail
     * Necessary to reduce the massive amount of memory that the client would consume
     * (~70MB for 8 base64 encoded pictures)
     * @param localUrl The URL of the original picture
     * @param id The ID of the current house
     * @return The local URL where the photograph was saved
     *         (Format: {WAR_DIR}/img/houses/{houseID}/thumb/{The current time in milliseconds})
     * @throws Exception
     */
    static public String saveFileThumb(String localUrl, int id) throws Exception {
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
        String leadingUrl = Constants.DIR + "/img/houses/" + id + "/thumb/";
        String thumbLocalUrl = leadingUrl + newFileName;
        Path leadingPath = Paths.get(leadingUrl);
        if (!Files.exists(leadingPath)) {
            try {
                Files.createDirectories(leadingPath);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        File thumbFile = new File(thumbLocalUrl);
        if (!thumbFile.getParentFile().exists()) {
            if (!thumbFile.getParentFile().mkdirs()) {
                throw new IOException("Directory could not be created");
            }
        }
        ImageIO.write(img, "jpg", new File(thumbLocalUrl));
        return thumbLocalUrl;
    }

    /**
     * Returns the path to a URL's parent directory
     * @param path The path of the file
     * @return The path to the parent folder
     */
    static public String getLeadingPath(String path) {
        String[] components = path.split(File.separator);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < components.length - 1; i++) {
            sb.append(components[i]).append("/");
        }
        return sb.toString();
    }
}
