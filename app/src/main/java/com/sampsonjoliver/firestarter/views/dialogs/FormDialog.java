package com.sampsonjoliver.firestarter.views.dialogs;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.sampsonjoliver.firestarter.R;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by samol on 12/01/2016.
 */
public final class FormDialog {
    protected abstract class FormObject {
        protected FieldValidator validator;
        protected String tag;

        public abstract boolean validate();
        public abstract String getTag();
        public abstract Serializable getInputData();
        public abstract View getView();
        public abstract void showError();
    }

    protected abstract class FormInputObject extends FormObject {

        public boolean validate() {
            return validator.fieldErrorMessage(getInputData()) == null;
        }

        public String getTag() {
            return tag;
        }
    }

    private class GroupLabel extends FormObject {
        public TextView view;

        public GroupLabel(Context context, String label) {
            view = new TextView(context);

            int marginPx = context.getResources().getDimensionPixelSize(R.dimen.spacingSmall);
            view.setText(label);
            view.setTextSize(16);
            view.setTypeface(null, Typeface.BOLD);
            LinearLayout.LayoutParams fieldParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            fieldParams.setMargins(0, 0, 0, marginPx);
            view.setLayoutParams(fieldParams);
        }

        @Override
        public boolean validate() {
            return true;
        }

        @Override
        public String getTag() {
            return null;
        }

        @Override
        public Serializable getInputData() {
            return null;
        }

        @Override
        public View getView() {
            return view;
        }

        @Override
        public void showError() {

        }
    }

    private class TextMultilineInputObject extends TextInputObject {
        public TextMultilineInputObject(Context context, String hint, String tag, int inputType, FieldValidator validator) {
            super(context, hint, tag, inputType, validator);
        }

        public TextMultilineInputObject(Context context, String hint, String tag, String fieldData, int inputType, FieldValidator validator) {
            super(context, hint, tag, fieldData, inputType, validator);
        }

        @Override
        public void buildInputGroup(Context context, String hint, int inputType) {
            super.buildInputGroup(context, hint, inputType);
            view.setSingleLine(false);
            view.setMaxLines(3);
        }
    }

    private class TextInputObject extends FormInputObject {
        public EditText view;
        public String fieldData;

        public TextInputObject(Context context, String hint, String tag, int inputType, FieldValidator validator) {
            this.tag = tag;
            this.validator = validator;
            this.fieldData = "";
            buildInputGroup(context, hint, inputType);
        }

        public TextInputObject(Context context, String hint, String tag, String fieldData, int inputType, FieldValidator validator) {
            this.tag = tag;
            this.validator = validator;
            this.fieldData = fieldData;
            buildInputGroup(context, hint, inputType);
        }

        public void buildInputGroup(Context context, String hint, int inputType) {
            int marginPx = context.getResources().getDimensionPixelSize(R.dimen.spacingSmall);

            view = new EditText(context);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 0, marginPx);
            view.setLayoutParams(params);
            view.setInputType(inputType);
            view.setHint(hint);
            view.setText(fieldData);
        }

        @Override
        public Serializable getInputData() {
            return view.getText().toString();
        }

        @Override
        public View getView() {
            return view;
        }

        @Override
        public void showError() {
            view.setError(validator.fieldErrorMessage(view.getText().toString()));
        }
    }

    private Context context;
    private List<FormObject> formObjects;

    public FormDialog(Context context) {
        this.context = context;
        formObjects = new ArrayList<>();
    }

    public FormDialog addFieldHeader(String label) {
        formObjects.add(new GroupLabel(context, label));

        return this;
    }

    public FormDialog addInputText(String hint, String tag, int inputType, FieldValidator validator) {
        formObjects.add(new TextInputObject(context, hint, tag, inputType, validator));

        return this;
    }

    public FormDialog addInputText(String hint, String tag, String fieldData, int inputType, FieldValidator validator) {
        formObjects.add(new TextInputObject(context, hint, tag, fieldData, inputType, validator));

        return this;
    }

    public FormDialog addMultilineInputText(String hint, String tag, int inputType, FieldValidator validator) {
        formObjects.add(new TextMultilineInputObject(context, hint, tag, inputType, validator));

        return this;
    }

    public FormDialog addMultilineInputText(String hint, String tag, String fieldData, int inputType, FieldValidator validator) {
        formObjects.add(new TextMultilineInputObject(context, hint, tag, fieldData, inputType, validator));

        return this;
    }

    public AlertDialog build(String title, String cancel, String accept, final IFormDialogListener listener) {
        return build(title, cancel, accept, listener, null);
    }

    public AlertDialog build(String title, String cancel, String accept, final IFormDialogListener listener, final DialogInterface.OnClickListener onCancel) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(buildView());

        if (title != null)
            builder.setTitle(title);

        builder.setNegativeButton(cancel, onCancel);
        builder.setPositiveButton(accept, null);

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(final DialogInterface dialog) {
                Button b = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
                b.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        boolean hasError = false;
                        Bundle extras = new Bundle();
                        for (FormObject object : formObjects) {
                            if (!object.validate()) {
                                hasError = true;
                                object.showError();
                            } else {
                                extras.putSerializable(object.getTag(), object.getInputData());
                            }
                        }

                        if (!hasError) {
                            listener.onFormSave(extras);
                            dialog.dismiss();
                        }
                    }
                });
            }
        });

        return dialog;
    }

    private View buildView() {
        ScrollView root = new ScrollView(context);
        LinearLayout container = new LinearLayout(context);

        int paddingPx = context.getResources().getDimensionPixelSize(R.dimen.spacingMedium);
        container.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);
        container.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        container.setOrientation(LinearLayout.VERTICAL);
        root.addView(container, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        for (FormObject object : formObjects) {
            container.addView(object.getView());
        }

        return root;
    }

    public interface FieldValidator {
        String fieldErrorMessage(Serializable fieldData);
    }

    public interface IFormDialogListener {
        void onFormSave(Bundle data);
    }
}

