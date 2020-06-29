package com.towhid.pdfthumbnail;

import androidx.appcompat.app.AppCompatActivity;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ImageView;

import com.shockwave.pdfium.PdfDocument;
import com.shockwave.pdfium.PdfiumCore;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    String TAG = "data";
    ImageView iv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        iv = (ImageView) findViewById(R.id.imageView);
        search();



    }
    void search() {

        String selection = "_data LIKE '%.pdf'";
        String s;
        try (Cursor cursor = getApplicationContext().getContentResolver().query(MediaStore.Files.getContentUri("external"), null, selection, null, "_id ASC")) {
            if (cursor == null || cursor.getCount() <= 0 || !cursor.moveToFirst()) {
                // this means error, or simply no results found
                return;
            }
            do {
                 s = cursor.getString(1);

            } while (cursor.moveToNext());

            openPdf(Uri.fromFile(new File(s)));
        }

    }


    void openPdf(Uri pdfUri) {

        try {

            ParcelFileDescriptor fd = getContentResolver().openFileDescriptor(pdfUri, "r");
            int pageNum = 0;
            PdfiumCore pdfiumCore = new PdfiumCore(this);
            PdfDocument pdfDocument = pdfiumCore.newDocument(fd);

            pdfiumCore.openPage(pdfDocument, pageNum);

            int width = pdfiumCore.getPageWidthPoint(pdfDocument, pageNum);
            int height = pdfiumCore.getPageHeightPoint(pdfDocument, pageNum);

            // ARGB_8888 - best quality, high memory usage, higher possibility of OutOfMemoryError
            // RGB_565 - little worse quality, twice less memory usage
            Bitmap bitmap = Bitmap.createBitmap(width, height,
                    Bitmap.Config.RGB_565);
            pdfiumCore.renderPageBitmap(pdfDocument, bitmap, pageNum, 0, 0,
                    width, height);
            //if you need to render annotations and form fields, you can use
            //the same method above adding 'true' as last param

            iv.setImageBitmap(bitmap);

            printInfo(pdfiumCore, pdfDocument);

            pdfiumCore.closeDocument(pdfDocument); // important!
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void printInfo(PdfiumCore core, PdfDocument doc) {
        PdfDocument.Meta meta = core.getDocumentMeta(doc);
        Log.e(TAG, "title = " + meta.getTitle());
        Log.e(TAG, "author = " + meta.getAuthor());
        Log.e(TAG, "subject = " + meta.getSubject());
        Log.e(TAG, "keywords = " + meta.getKeywords());
        Log.e(TAG, "creator = " + meta.getCreator());
        Log.e(TAG, "producer = " + meta.getProducer());
        Log.e(TAG, "creationDate = " + meta.getCreationDate());
        Log.e(TAG, "modDate = " + meta.getModDate());

        printBookmarksTree(core.getTableOfContents(doc), "-");

    }

    public void printBookmarksTree(List<PdfDocument.Bookmark> tree, String sep) {
        for (PdfDocument.Bookmark b : tree) {

            Log.e(TAG, String.format("%s %s, p %d", sep, b.getTitle(), b.getPageIdx()));

            if (b.hasChildren()) {
                printBookmarksTree(b.getChildren(), sep + "-");
            }
        }
    }
}
