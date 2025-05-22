package com.example.acloc.utility;

import android.content.Context;
import android.net.Uri;

import com.example.acloc.api.LocationApiClient;
import com.example.acloc.service.UploadService;

import java.io.File;
import java.io.IOException;

import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UploadManager {
    public interface UploadCallback {
        void onSuccess(String message);
        void onError(String message);
    }

    /**
     * Uploads an image to the server
     *
     * @param context Application context
     * @param imageUri URI of the image to upload
     * @param callback Callback to handle the upload result
     */
    public static void uploadImage(Context context, Uri imageUri, UploadCallback callback) {
        if (context == null || imageUri == null || callback == null) {
            if (callback != null) {
                callback.onError("Invalid parameters");
            }
            return;
        }

        try {
            File file = ImagePicker.getFileFromUri(context, imageUri);
            MultipartBody.Part filePart = ImagePicker.prepareFilePart("file", file);

            performUpload(filePart, callback);
        } catch (IOException e) {
            callback.onError("Error preparing file: " + e.getMessage());
        }
    }

    private static void performUpload(MultipartBody.Part filePart, UploadCallback callback) {
        UploadService uploadService = LocationApiClient.getInstance().getUploadService();


        uploadService.uploadImage(filePart).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    try {
                        callback.onSuccess(response.body().string());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    callback.onError("Server error: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                callback.onError("Connection error: " + t.getMessage());
            }
        });
    }
}
