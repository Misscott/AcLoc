package com.example.acloc.utility;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.view.ViewParent;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

public class Helper {

    private static final String TAG = Helper.class.getSimpleName();

    public static void showToast(Context context, String message) {
        try {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        } catch (Exception ignored) {
        }
    }

    public static void makeSnackBar(View view, String message) {
        try {
            Snackbar.make(view, message, Snackbar.LENGTH_LONG).show();
        } catch (Exception ignored) {
            showToast(view.getContext(), message);
        }
    }

    public static void goTo(Context context, Class<?> activity) {
        Intent intent = new Intent(context, activity);
        context.startActivity(intent);
    }

    public static void goToAndFinish(Context context, Class<?> activity) {
        Intent intent = new Intent(context, activity);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);

        // No need to finish explicitly here, flags handle it
        if (context instanceof Activity) {
            ((Activity) context).finish();
        }
    }

    public static void goToAndFinish(Context context, Class<?> activity, String key, Serializable object) {
        Intent intent = new Intent(context, activity);
        intent.putExtra(key, object);
        context.startActivity(intent);

        if (context instanceof Activity) {
            ((Activity) context).finish();
        }
    }

    public static void goTo(Context context, Class<?> activity, String key, Serializable object) {
        Intent intent = new Intent(context, activity);
        intent.putExtra(key, object);
        context.startActivity(intent);

//        if (context instanceof Activity) {
//            ((Activity) context).finish();
//        }
    }

    public static int getIntValueFromString(String value) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return 0;
        }
    }

    public static double getDoubleValueFromString(String value) {
        try {
            return Double.parseDouble(value);
        } catch (Exception e) {
            return 0;
        }
    }

    public static String getStringFromInput(View view) {
        try {
            if (view instanceof TextInputEditText) {
                TextInputEditText editText = (TextInputEditText) view;
                return Objects.requireNonNull(editText.getText()).toString().trim();
            } else if (view instanceof MaterialAutoCompleteTextView) {
                MaterialAutoCompleteTextView editText = (MaterialAutoCompleteTextView) view;
                return Objects.requireNonNull(editText.getText()).toString();
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    public static boolean isEmptyFieldValidation(TextInputEditText editText) {
        boolean isValidate = true;
        try {
            TextInputLayout textInputLayout = null;
            ViewParent parent = editText.getParent().getParent();
            if (parent instanceof TextInputLayout) {
                textInputLayout = (TextInputLayout) parent;
            }
            if (Objects.requireNonNull(editText.getText()).toString().trim().isEmpty()) {
                if (textInputLayout != null) {
                    textInputLayout.isHelperTextEnabled();
                    textInputLayout.setError("Please " + textInputLayout.getHint());
                    textInputLayout.setErrorEnabled(true);
                } else {
                    editText.setError("Empty");
                }
                isValidate = false;
            } else {
                if (textInputLayout != null) {
                    textInputLayout.setErrorEnabled(false);
                } else {
                    editText.setError(null);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in Helper Class: ", e);
            isValidate = false;
        }
        return isValidate;
    }

    public static boolean isEmptyFieldValidation(View[] inputFields) {
        boolean isValidate = true;
        try {
            for (View view : inputFields) {
                TextInputLayout textInputLayout = null;
                ViewParent parent = view.getParent().getParent();
                if (parent instanceof TextInputLayout) {
                    textInputLayout = (TextInputLayout) parent;
                }

                String inputText = "";
                if (view instanceof TextInputEditText) {
                    inputText = Objects.requireNonNull(((TextInputEditText) view).getText()).toString().trim();
                } else if (view instanceof MaterialAutoCompleteTextView) {
                    inputText = Objects.requireNonNull(((MaterialAutoCompleteTextView) view).getText()).toString().trim();
                }

                if (inputText.isEmpty()) {
                    if (textInputLayout != null) {
                        textInputLayout.setError("Please " + textInputLayout.getHint());
                        textInputLayout.setErrorEnabled(true);
                    } else {
                        if (view instanceof TextInputEditText) {
                            ((TextInputEditText) view).setError("Empty");
                        } else if (view instanceof MaterialAutoCompleteTextView) {
                            ((MaterialAutoCompleteTextView) view).setError("Empty");
                        }
                    }
                    isValidate = false;
                } else {
                    if (textInputLayout != null) {
                        textInputLayout.setErrorEnabled(false);
                    } else {
                        if (view instanceof TextInputEditText) {
                            ((TextInputEditText) view).setError(null);
                        } else if (view instanceof MaterialAutoCompleteTextView) {
                            ((MaterialAutoCompleteTextView) view).setError(null);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in validation: ", e);
            isValidate = false;
        }
        return isValidate;
    }

    public static boolean isEmailValid(View emailView) {
        boolean isValidate = true;
        try {
            TextInputLayout textInputLayout = null;
            ViewParent parent = emailView.getParent().getParent();
            if (parent instanceof TextInputLayout) {
                textInputLayout = (TextInputLayout) parent;
            }

            String emailText = "";
            if (emailView instanceof TextInputEditText) {
                emailText = Objects.requireNonNull(((TextInputEditText) emailView).getText()).toString().trim();
            } else if (emailView instanceof MaterialAutoCompleteTextView) {
                emailText = Objects.requireNonNull(((MaterialAutoCompleteTextView) emailView).getText()).toString().trim();
            }

            // Regex for email validation
            if (!emailText.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$")) {
                if (textInputLayout != null) {
                    textInputLayout.setError("Invalid Email Address");
                    textInputLayout.setErrorEnabled(true);
                } else {
                    if (emailView instanceof TextInputEditText) {
                        ((TextInputEditText) emailView).setError("Invalid Email Address");
                    } else if (emailView instanceof MaterialAutoCompleteTextView) {
                        ((MaterialAutoCompleteTextView) emailView).setError("Invalid Email Address");
                    }
                }
                isValidate = false;
            } else {
                if (textInputLayout != null) {
                    textInputLayout.setErrorEnabled(false);
                } else {
                    if (emailView instanceof TextInputEditText) {
                        ((TextInputEditText) emailView).setError(null);
                    } else if (emailView instanceof MaterialAutoCompleteTextView) {
                        ((MaterialAutoCompleteTextView) emailView).setError(null);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in email validation: ", e);
            isValidate = false;
        }
        return isValidate;
    }

    public static boolean isPasswordValid(View passwordView) {
        boolean isValidate = true;
        try {
            TextInputLayout textInputLayout = null;
            ViewParent parent = passwordView.getParent().getParent();
            if (parent instanceof TextInputLayout) {
                textInputLayout = (TextInputLayout) parent;
            }

            String passwordText = "";
            if (passwordView instanceof TextInputEditText) {
                passwordText = Objects.requireNonNull(((TextInputEditText) passwordView).getText()).toString().trim();
            } else if (passwordView instanceof MaterialAutoCompleteTextView) {
                passwordText = Objects.requireNonNull(((MaterialAutoCompleteTextView) passwordView).getText()).toString().trim();
            }

            // Regex for strong password validation
            String passwordPattern = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{6,}$";

            if (!passwordText.matches(passwordPattern)) {
                if (textInputLayout != null) {
                    textInputLayout.setError("Password must be at least 6 characters, include 1 uppercase, 1 lowercase, 1 digit, and 1 special character.");
                    textInputLayout.setErrorEnabled(true);
                } else {
                    if (passwordView instanceof TextInputEditText) {
                        ((TextInputEditText) passwordView).setError("Password must be at least 6 characters, include 1 uppercase, 1 lowercase, 1 digit, and 1 special character.");
                    } else if (passwordView instanceof MaterialAutoCompleteTextView) {
                        ((MaterialAutoCompleteTextView) passwordView).setError("Password must be at least 6 characters, include 1 uppercase, 1 lowercase, 1 digit, and 1 special character.");
                    }
                }
                isValidate = false;
            } else {
                if (textInputLayout != null) {
                    textInputLayout.setErrorEnabled(false);
                } else {
                    if (passwordView instanceof TextInputEditText) {
                        ((TextInputEditText) passwordView).setError(null);
                    } else if (passwordView instanceof MaterialAutoCompleteTextView) {
                        ((MaterialAutoCompleteTextView) passwordView).setError(null);
                    }
                }
            }
        } catch (Exception e) {
            Log.e("Helper", "Error in password validation: ", e);
            isValidate = false;
        }
        return isValidate;
    }

    public static boolean isContactValid(View contactView) {
        boolean isValidate = true;
        try {
            TextInputLayout textInputLayout = null;
            ViewParent parent = contactView.getParent().getParent();
            if (parent instanceof TextInputLayout) {
                textInputLayout = (TextInputLayout) parent;
            }

            String contactText = "";
            if (contactView instanceof TextInputEditText) {
                contactText = Objects.requireNonNull(((TextInputEditText) contactView).getText()).toString().trim();
            } else if (contactView instanceof MaterialAutoCompleteTextView) {
                contactText = Objects.requireNonNull(((MaterialAutoCompleteTextView) contactView).getText()).toString().trim();
            }

            // Regex for validating contact (Assuming 10-digit phone number)
            if (!contactText.matches("^[0-9]{10}$")) {
                if (textInputLayout != null) {
                    textInputLayout.setError("Invalid Contact Number");
                    textInputLayout.setErrorEnabled(true);
                } else {
                    if (contactView instanceof TextInputEditText) {
                        ((TextInputEditText) contactView).setError("Invalid Contact Number");
                    } else if (contactView instanceof MaterialAutoCompleteTextView) {
                        ((MaterialAutoCompleteTextView) contactView).setError("Invalid Contact Number");
                    }
                }
                isValidate = false;
            } else {
                if (textInputLayout != null) {
                    textInputLayout.setErrorEnabled(false);
                } else {
                    if (contactView instanceof TextInputEditText) {
                        ((TextInputEditText) contactView).setError(null);
                    } else if (contactView instanceof MaterialAutoCompleteTextView) {
                        ((MaterialAutoCompleteTextView) contactView).setError(null);
                    }
                }
            }
        } catch (Exception e) {
            Log.e("ValidationHelper", "Error in contact validation: ", e);
            isValidate = false;
        }
        return isValidate;
    }

    public static String getCurrentDate() {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
            Date date = new Date();
            return dateFormat.format(date);
        } catch (Exception e) {
            Log.e(TAG, "Error in getting current date: ", e);
            return null;
        }
    }

    public static String getCurrentTime() {
        try {
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
            Date date = new Date();
            return timeFormat.format(date);
        } catch (Exception e) {
            Log.e(TAG, "Error in getting current time: ", e);
            return null;
        }
    }

    public static void goToWithFlags(Context context, Class<?> activity, int flags) {
        Intent intent = new Intent(context, activity);
        intent.setFlags(flags);
        context.startActivity(intent);
    }

    public static LinearLayoutManager getVerticalManager(Context context) {
        return new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false);
    }
}

