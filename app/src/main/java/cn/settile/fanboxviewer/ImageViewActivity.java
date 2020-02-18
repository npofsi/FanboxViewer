package cn.settile.fanboxviewer;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.palette.graphics.Palette;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.veinhorn.scrollgalleryview.MediaInfo;
import com.veinhorn.scrollgalleryview.ScrollGalleryView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import cn.settile.fanboxviewer.Network.Common;
import cn.settile.fanboxviewer.Network.CustomPicassoLoader;

import static cn.settile.fanboxviewer.Util.Util.createImageFile;
import static cn.settile.fanboxviewer.Util.Util.galleryAddPic;
import static cn.settile.fanboxviewer.Util.Util.toBitmap;

public class ImageViewActivity extends AppCompatActivity {

    private List<String> images;
    private String detail;

    private final String TAG = this.getClass().getName();
    private int pos;
    private List<Integer> colors = new ArrayList<>();
    private List<String> thumbs = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_view);

        Intent i = getIntent();
        this.images = i.getStringArrayListExtra("Images");
        this.thumbs = i.getStringArrayListExtra("Thumbnails");
        this.detail = i.getStringExtra("Details");
        this.pos = i.getIntExtra("Position", 0);

        for(int index = 0; index < thumbs.size(); index++){
            colors.add(Color.BLACK);
        }

        ScrollGalleryView view = findViewById(R.id.image_view_main);
        view.setThumbnailSize(200)
                .setZoom(true)
                .withHiddenThumbnails(false)
                .hideThumbnailsOnClick(false)
                .addOnImageLongClickListener(position -> {
                    Snackbar.make(getWindow().getDecorView(), "Downloading", Snackbar.LENGTH_LONG).show();
                    File image;
                    try {
                        image = createImageFile(detail + "_" + position +
                                images.get(position)
                                        .substring(images.get(position).lastIndexOf('.') - 1));
                        Common.downloadThread(images.get(position), image,
                                () -> Snackbar.make(getWindow().getDecorView(), "Downloaded", Snackbar.LENGTH_LONG).show(),
                                () -> Snackbar.make(getWindow().getDecorView(), "Fail to download", Snackbar.LENGTH_LONG).show());
                        galleryAddPic(image.getAbsolutePath(), this);
                    }catch (Exception ex){
                        Log.e(TAG, "onCreate: ", ex);
                    }
                })
                .setFragmentManager(getSupportFragmentManager());
        for(int index = 0; index< images.size(); index++){
            CustomPicassoLoader mi = new CustomPicassoLoader(thumbs.get(index));
            final int index1 = index;
            mi.onLoaded((b) -> {
                colors.set(index1,
                        Palette.from(toBitmap(b))
                                .generate()
                                .getDarkMutedColor(Color.BLACK));
            });
            view.addMedia(
                    MediaInfo.mediaLoader(mi)
            );
        }
        view.setCurrentItem(pos);
        new Thread(() -> {
            while (colors.size() - 1 < pos){}
            view.setBackgroundColor(colors.get(pos));
            view.invalidate();
        }).start();
        view.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener(){
            @Override
            public void onPageSelected(int position) {
                Log.d(TAG, "size=" + (colors.size() - 1) + " pos=" + position);
                if (colors.size() - 1 < position){
                    return;
                }
                view.setBackgroundColor(colors.get(position));
                view.invalidate();
            }
        });

        FloatingActionButton fab = findViewById(R.id.image_view_download);
        fab.setOnClickListener(v -> {
            Snackbar.make(getWindow().getDecorView(), "Downloading", Snackbar.LENGTH_LONG).show();
            File image;
            int position = view.getCurrentItem();
            try {
                image = createImageFile(detail + "_" + position +
                        images.get(position)
                                .substring(images.get(position).lastIndexOf('.') - 1));
                Common.downloadThread(images.get(position), image,
                        () -> Snackbar.make(getWindow().getDecorView(), "Downloaded", Snackbar.LENGTH_LONG).show(),
                        () -> Snackbar.make(getWindow().getDecorView(), "Fail to download", Snackbar.LENGTH_LONG).show());
                galleryAddPic(image.getAbsolutePath(), this);
            }catch (Exception ex){
                Log.e(TAG, "onCreate: ", ex);
            }
        });
    }
}