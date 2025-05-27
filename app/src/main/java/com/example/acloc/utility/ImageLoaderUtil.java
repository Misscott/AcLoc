package com.example.acloc.utility;

import android.net.Uri;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

/**
 * Utility class to handle image loading operations
 * Separates image loading responsibility from activities
 */
public class ImageLoaderUtil {

    /**
     * Loads an image from a Uri into an ImageView
     *
     * @param imageUri Source Uri of the image
     * @param targetView Target ImageView where the image will be displayed
     */
    public static void loadImage(Uri imageUri, ImageView targetView) {
        if (imageUri != null && targetView != null) {
            Picasso.get().load(imageUri).into(targetView);
        }
    }

    /**
     * Loads an image from a Uri into an ImageView with placeholder and error handling
     *
     * @param imageUri Source Uri of the image
     * @param targetView Target ImageView where the image will be displayed
     * @param placeholderResId Resource ID for the placeholder image
     * @param errorResId Resource ID for the error image
     */
    public static void loadImageWithPlaceholder(
            Uri imageUri,
            ImageView targetView,
            int placeholderResId,
            int errorResId) {
        if (imageUri != null && targetView != null) {
            Picasso.get()
                    .load(imageUri)
                    .placeholder(placeholderResId)
                    .error(errorResId)
                    .into(targetView);
        }
    }
}
