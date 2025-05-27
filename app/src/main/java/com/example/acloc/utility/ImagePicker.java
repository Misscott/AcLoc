package com.example.acloc.utility;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

public class ImagePicker {

    /**
     * Get a file from a Uri.
     *
     * @param context The application context
     * @param uri The Uri to get the file from
     * @return The file
     * @throws IOException
     */
    public static File getFileFromUri(Context context, Uri uri) throws IOException {
        String fileName = getFileName(context, uri);
        File file = new File(context.getCacheDir(), fileName);

        // Copy the file to the cache directory
        try (InputStream inputStream = context.getContentResolver().openInputStream(uri);
             OutputStream outputStream = new FileOutputStream(file)) {

            if (inputStream == null) {
                throw new IOException("Failed to open input stream.");
            }

            byte[] buffer = new byte[4 * 1024]; // 4k buffer
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }

            outputStream.flush();
            return file;
        }
    }

    /**
     * Get the file name from a Uri
     *
     * @param context The application context
     * @param uri The Uri to get the file name from
     * @return The file name
     */
    private static String getFileName(Context context, Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        result = cursor.getString(nameIndex);
                    }
                }
            }
        }

        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }

        return result;
    }

    /**
     * Prepare a file part for a multipart request
     *
     * @param partName The name of the part
     * @param file The file to upload
     * @return The MultipartBody.Part
     */
    public static MultipartBody.Part prepareFilePart(String partName, File file) {
        // Get the MIME type
        String extension = MimeTypeMap.getFileExtensionFromUrl(file.getPath());
        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);

        if (mimeType == null) {
            // Default to image/jpeg if MIME type cannot be determined
            mimeType = "image/jpeg";
        }

        // Create RequestBody instance from file
        RequestBody requestFile = RequestBody.create(MediaType.parse(mimeType), file);

        // MultipartBody.Part is used to send the actual file
        return MultipartBody.Part.createFormData(partName, file.getName(), requestFile);
    }
}
