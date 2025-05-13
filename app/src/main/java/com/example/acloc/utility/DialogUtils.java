package com.example.acloc.utility;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Window;
import android.widget.TextView;

import com.ieslamar.acloc.R;
import com.example.acloc.activity.LoginActivity;

public class DialogUtils {
    private static Dialog dialog;

    public static void showLoadingDialog(Context context, String message) {
        if (dialog != null && dialog.isShowing()) {
            return;
        }

        dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.loading);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.setCancelable(false);

        TextView tvLoadingText = dialog.findViewById(R.id.tvLoadingText);
        tvLoadingText.setText(message);

        dialog.show();
    }

    public static void dismissDialog() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
            dialog = null;
        }
    }

    public static AlertDialog logoutDialog(final Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        builder.setMessage(context.getString(R.string.Are_you_sure_you_want_to_logout));

        builder.setPositiveButton("Yes", (dialog, id) -> {
            dialog.cancel();
            int flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_CLEAR_TASK
                    | Intent.FLAG_ACTIVITY_NEW_TASK;
            SharedPref.setIsLoggedIn(context,false);
            SharedPref.deleteAll(context);
            Helper.goToWithFlags(context, LoginActivity.class, flags);
        });

        builder.setNegativeButton("No", (dialog, id) -> dialog.cancel());
        return builder.create();
    }

//    public static AlertDialog confirmationDialog(final Context context, String confirmationText
//            , DialogInterface.OnClickListener onDeleteClickListener) {
//        AlertDialog.Builder builder = new AlertDialog.Builder(context);
//        if (confirmationText == null) confirmationText = "perform this operation";
//        builder.setMessage("Are you sure you want to " + confirmationText + "?" +
//                "\nWARNING: This action cannot be undone");
//        builder.setPositiveButton("Yes", onDeleteClickListener);
//        builder.setNegativeButton("No", (dialog, id) -> dialog.cancel());
//        return builder.create();
//    }

    public static AlertDialog confirmationDialog(final Context context, String confirmationText
            , DialogInterface.OnClickListener onDeleteClickListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        if (confirmationText == null) confirmationText = "perform this operation";
        String message = context.getString(R.string.confirmation_message, confirmationText);
        builder.setMessage(message);
        builder.setPositiveButton(context.getString(R.string.yes), onDeleteClickListener);
        builder.setNegativeButton(context.getString(R.string.no), (dialog, id) -> dialog.cancel());
        return builder.create();
    }


}
