package com.google.zxing.client.android;

import java.io.FileNotFoundException;
import java.io.InputStream;

import android.app.Fragment;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.google.zxing.common.BitMatrix;

public class QRFragment extends Fragment {

    BitMatrix bitmap = null;
    private final static String TAG = "QRFragment";
    private static final String FILENAME = "qrcode";

    /**
     * This populates the fragment with a view that contains an ImageView and
     * that imageview is used to contain the bitmap of the QRCode.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view3 = inflater.inflate(R.layout.qr, container, false);
        createQRCode(view3);
        return view3;
    }

    /**
     * Checks to see if the bitmap file already exists in memory, if it doesn't
     * it will create one, if it does then it will populate the imageView with
     * it.
     * 
     * @param qrInputText
     *            - Personal QR code Bytes
     * @param view
     *            - The current fragment/view
     */
    private void createQRCode(View view) {
        ImageView myImage = (ImageView) view.findViewById(R.id.imageView1);

        InputStream is;
        try {
            is = getActivity().openFileInput(FILENAME);
            myImage.setImageBitmap(BitmapFactory.decodeStream(is));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}
