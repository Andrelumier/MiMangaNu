package ar.rulosoft.mimanganu.componentes.readers.paged;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.view.PagerAdapter;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import java.util.List;

import ar.rulosoft.mimanganu.R;
import ar.rulosoft.mimanganu.componentes.readers.Reader;
import it.sephiroth.android.library.TapListener;
import it.sephiroth.android.library.imagezoom.ImageViewTouch;
import it.sephiroth.android.library.imagezoom.ImageViewTouchBase;
import it.sephiroth.android.library.imagezoom.ImageViewTouchBase.InitialPosition;
import it.sephiroth.android.library.imagezoom.graphics.FastBitmapDrawable;

/**
 * Created by Raul on 24/06/2016.
 */

public abstract class PagedReader extends Reader {

    private static ImageViewTouchBase.DisplayType mScreenFit;
    List<String> paths;
    private InitialPosition iniPosition = InitialPosition.LEFT_UP;
    protected PageAdapter mPageAdapter;
    float mStartDragX;
    boolean firedListener = false;
    int currentPage = 0;

    public PagedReader(Context context) {
        super(context);
        init();
    }

    public void init() {
        String infService = Context.LAYOUT_INFLATER_SERVICE;
        LayoutInflater li = (LayoutInflater)getContext().getSystemService(infService);
        li.inflate(R.layout.view_paged_reader, this, true);
    }

    public abstract void setPagerAdapter(PageAdapter mPageAdapter);

    @Override
    public void setScreenFit(ImageViewTouchBase.DisplayType displayType) {
        mScreenFit = displayType;
        if (mPageAdapter != null)
            mPageAdapter.updateDisplayType();
    }

    @Override
    public void seekPage(int aPage) {
        goToPage(aPage);
    }

    @Override
    public void setPaths(List<String> paths) {
        this.paths = paths;
        setPagerAdapter(new PageAdapter());
    }

    @Override
    public void freeMemory() {

    }

    @Override
    public void freePage(int idx) {}

    @Override
    public String getPath(int idx) {
        return null;
    }

    @Override
    public void reloadImage(int idx) {

    }

    @Override
    public boolean isLastPageVisible() {
        return false;
    }

    @Override
    public void setScrollSensitive(float mScrollSensitive) {

    }
    @Override
    public boolean onDoubleTap(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {

    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        return false;
    }

    @Override
    public boolean hasFitFeature() {
        return true;
    }

    public class PageAdapter extends PagerAdapter {
        Page[] pages = new Page[paths.size()];

        public Page getCurrentPage() {
            return pages[currentPage];
        }

        public void setCurrentPage(int nCurrentPage) {
            if (mDirection == Direction.L2R)
                nCurrentPage = paths.size() - nCurrentPage;
            currentPage = nCurrentPage;
            for (int i = 0; i < pages.length; i++) {
                if (pages[i] != null) {
                    if (Math.abs(i - nCurrentPage) <= 1 && !pages[i].imageLoaded) {
                        pages[i].setImage();
                    } else if (Math.abs(i - nCurrentPage) > 1 && pages[i].imageLoaded) {
                        pages[i] = null;
                    }
                }
            }
        }

        public Page getPage(int position) {
            return pages[position];
        }

        @Override
        public int getCount() {
            return pages.length;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            if (mDirection == Direction.L2R)
                position = getCount() - position;
            Page page = pages[position];
            if (pages[position] != null) {
                container.addView(page, 0);
            } else {
                Context context = getContext();
                page = new Page(context);
                page.setImage(paths.get(position));
                container.addView(page, 0);
                page.index = position;
                pages[position] = page;
            }
            return page;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((Page) object);
        }

        public void updateDisplayType() {
            for (int i = 0; i < pages.length; i++) {
                if (pages[i] != null) {
                    pages[i].visor.setDisplayType(mScreenFit);
                }
            }
        }

        public void setPageScroll(float pageScroll) {
            for (int i = 0; i < pages.length; i++) {
                if (pages[i] != null) {
                    pages[i].visor.setScrollFactor(pageScroll);
                }
            }
        }

        public void pageDownloaded(int page) {
            if (pages[page] != null && currentPage == page) {
                pages[page].setImage();
            }
        }
    }

    public class Page extends RelativeLayout {
        public ImageViewTouch visor;
        ProgressBar loading;
        Runnable r = null;
        boolean imageLoaded = false;
        int index = 0;
        private String path = null;

        public Page(Context context) {
            super(context);
            init();
        }

        public void init() {
            String infService = Context.LAYOUT_INFLATER_SERVICE;
            LayoutInflater li = (LayoutInflater) getContext().getSystemService(infService);
            li.inflate(R.layout.view_reader_page, this, true);
            visor = (ImageViewTouch) findViewById(R.id.visor);
            visor.setDisplayType(mScreenFit);
            //TODO visor.setTapListener(PagedReader.this);
            visor.setScaleEnabled(false);
            loading = (ProgressBar) findViewById(R.id.loading);
            loading.bringToFront();
            visor.setScrollFactor(mScrollSensitive);
        }

        public void unloadImage() {
            if (visor != null) {
                if (visor.getDrawable() != null)
                    ((FastBitmapDrawable) visor.getDrawable()).getBitmap().recycle();
                visor.setImageDrawable(null);
                visor.setImageBitmap(null);
            }
            imageLoaded = false;
        }

        public void setImage() {
            if (!imageLoaded && visor != null)
                new SetImageTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }

        public void setImage(String path) {
            this.path = path;
            setImage();
        }

        public boolean canScroll(int dx) {
            return visor == null || visor.canScroll(dx);
        }

        public boolean canScrollV(int dx) {
            return visor == null || visor.canScrollV(dx);
        }

        public class SetImageTask extends AsyncTask<Void, Void, Bitmap> {

            @Override
            protected void onPreExecute() {
                if (loading != null)
                    loading.setVisibility(ProgressBar.VISIBLE);
                super.onPreExecute();
            }

            @Override
            protected Bitmap doInBackground(Void... params) {
                boolean notLoaded = true;
                Bitmap bitmap = null;
                BitmapFactory.Options opts = new BitmapFactory.Options();
                opts.inPreferredConfig = Bitmap.Config.RGB_565;
                while (notLoaded) {
                    try {
                        bitmap = BitmapFactory.decodeFile(path, opts);
                        notLoaded = false;
                    } catch (OutOfMemoryError oom) {
                        try {
                            Thread.sleep(3000);//time to free memory
                        } catch (InterruptedException ignored) {
                        }
                    }
                }
                return bitmap;
            }

            @Override
            protected void onPostExecute(Bitmap result) {
                if (result != null && visor != null) {
                    imageLoaded = true;
                    visor.setScaleEnabled(true);
                    if (mDirection == Direction.VERTICAL)
                        visor.setInitialPosition(iniPosition);
                    else visor.setInitialPosition(ImageViewTouchBase.InitialPosition.LEFT_UP);
                    if ((result.getHeight() > mTextureMax ||
                            result.getWidth() > mTextureMax) &&
                            Build.VERSION.SDK_INT >= 11) {
                        visor.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
                    }
                    visor.setAlpha(0f);
                    visor.setImageBitmap(result);
                    if (index == getCurrentPage()) {
                        ObjectAnimator.ofFloat(visor, "alpha", 1f).setDuration(500).start();
                    } else {
                        visor.setAlpha(1f);
                    }
                    loading.setVisibility(ProgressBar.INVISIBLE);
                }
                super.onPostExecute(result);
            }
        }
    }
}