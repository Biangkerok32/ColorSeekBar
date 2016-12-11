package com.rtugeek.android.colorseekbar;


import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import android.os.Build;
import android.support.annotation.ArrayRes;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class ColorSeekBar extends View {
    private int mBackgroundColor = 0xffffffff;
    private int[] mColorSeeds = new int[]{0xFF000000, 0xFF9900FF, 0xFF0000FF, 0xFF00FF00, 0xFF00FFFF, 0xFFFF0000, 0xFFFF00FF, 0xFFFF6600, 0xFFFFFF00, 0xFFFFFFFF, 0xFF000000};
    ;
    private int c0, c1, mAlpha, mRed, mGreen, mBlue;
    private float x, y;
    private OnColorChangeListener mOnColorChangeLister;
    private Context mContext;
    private boolean mIsShowAlphaBar = false;
    private Bitmap mTransparentBitmap;
    private boolean mMovingColorBar;
    private boolean mMovingAlphaBar;
    private Rect mColorRect;
    private int mThumbHeight = 20;
    private int mBarHeight = 2;
    private LinearGradient mColorGradient;
    private Paint mColorRectPaint;
    private int realLeft;
    private int realRight;
    private int realTop;
    private int realBottom;
    private int mBarWidth;
    private int mMaxPosition;
    private Rect mAlphaRect;
    private int mColorBarPosition;
    private int mAlphaBarPosition;
    private float mThumbRadius;
    private int mBarMargin = 5;
    private int mPaddingSize;
    private int mViewWidth;
    private int mViewHeight;
    private List<Integer> mColors = new ArrayList<>();
    private int mColorsToInvoke = -1;
    private boolean mInit = false;
    private boolean mFirstDraw = true;
    public ColorSeekBar(Context context) {
        super(context);
        init(context, null, 0, 0);
    }

    public ColorSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0, 0);
    }

    public ColorSeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr, 0);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ColorSeekBar(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs, defStyleAttr, defStyleRes);
    }

    protected void init(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        applyStyle(context, attrs, defStyleAttr, defStyleRes);
    }

    public void applyStyle(int resId) {
        applyStyle(getContext(), null, 0, resId);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        Logger.i("onMeasure");
        mViewWidth = widthMeasureSpec;
        mViewHeight = heightMeasureSpec;

        int speMode = MeasureSpec.getMode(heightMeasureSpec);
        if (speMode == MeasureSpec.AT_MOST || speMode == MeasureSpec.UNSPECIFIED) {
            if (mIsShowAlphaBar) {
                setMeasuredDimension(mViewWidth, mThumbHeight * 2 + mBarHeight * 2 + mBarMargin);
            } else {
                setMeasuredDimension(mViewWidth, mThumbHeight + mBarHeight);
            }
        }
    }


    protected void applyStyle(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        mContext = context;
        //get attributes
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ColorSeekBar, defStyleAttr, defStyleRes);
        int colorsId = a.getResourceId(R.styleable.ColorSeekBar_colorSeeds, 0);
        mMaxPosition = a.getInteger(R.styleable.ColorSeekBar_maxPosition, 100);
        mColorBarPosition = a.getInteger(R.styleable.ColorSeekBar_colorBarPosition, 0);
        mAlphaBarPosition = a.getInteger(R.styleable.ColorSeekBar_alphaBarPosition, 0);
        mIsShowAlphaBar = a.getBoolean(R.styleable.ColorSeekBar_showAlphaBar, false);
        mBackgroundColor = a.getColor(R.styleable.ColorSeekBar_bgColor, Color.TRANSPARENT);
        mBarHeight = (int) a.getDimension(R.styleable.ColorSeekBar_barHeight, (float) dp2px(2));
        mThumbHeight = (int) a.getDimension(R.styleable.ColorSeekBar_thumbHeight, (float) dp2px(30));
        mBarMargin = (int) a.getDimension(R.styleable.ColorSeekBar_barMargin, (float) dp2px(5));
        a.recycle();

        if (colorsId != 0) mColorSeeds = getColorsById(colorsId);

        setBackgroundColor(mBackgroundColor);

    }

    /**
     * @param id
     * @return
     */
    private int[] getColorsById(int id) {
        if (isInEditMode()) {
            String[] s = mContext.getResources().getStringArray(id);
            int[] colors = new int[s.length];
            for (int j = 0; j < s.length; j++) {
                colors[j] = Color.parseColor(s[j]);
            }
            return colors;
        } else {
            TypedArray typedArray = mContext.getResources().obtainTypedArray(id);
            int[] colors = new int[typedArray.length()];
            for (int j = 0; j < typedArray.length(); j++) {
                colors[j] = typedArray.getColor(j, Color.BLACK);
            }
            typedArray.recycle();
            return colors;
        }
    }

    private void init() {
        Logger.i("init");
        //init size
        mThumbRadius = mThumbHeight / 2;
        mPaddingSize = (int) mThumbRadius;

        //init l r t b
        realLeft = getPaddingLeft() + mPaddingSize;
        realRight = getWidth() - getPaddingRight() - mPaddingSize;
        realTop = getPaddingTop() + mPaddingSize;
        realBottom = getHeight() - getPaddingBottom() - mPaddingSize;

        mBarWidth = realRight - realLeft;

        //init rect
        mColorRect = new Rect(realLeft, realTop, realRight, realTop + mBarHeight);

        //init paint
        mColorGradient = new LinearGradient(0, 0, mColorRect.width(), 0, mColorSeeds, null, Shader.TileMode.MIRROR);
        mColorRectPaint = new Paint();
        mColorRectPaint.setShader(mColorGradient);
        mColorRectPaint.setAntiAlias(true);
        cacheColors();
        setAlphaValue();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        Logger.i("onSizeChanged");
        mTransparentBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_4444);
        mTransparentBitmap.eraseColor(Color.TRANSPARENT);
        init();
        mInit = true;
        if(mColorsToInvoke != -1) setColor(mColorsToInvoke);
    }



    private void cacheColors() {
        //if the view's size hasn't been initialized. do not cache.
        if (mBarWidth < 1) return;
        mColors.clear();
        for (int i = 0; i <= mMaxPosition; i++) {
            mColors.add(pickColor(i));
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Logger.i("onDraw");
        float colorPosition = (float) mColorBarPosition / mMaxPosition * mBarWidth;

        Paint colorPaint = new Paint();
        colorPaint.setAntiAlias(true);
        int color = getColor(false);
        int colorToTransparent = Color.argb(0, Color.red(color), Color.green(color), Color.blue(color));
        colorPaint.setColor(color);
        int[] toAlpha = new int[]{color, colorToTransparent};
        //clear
        canvas.drawBitmap(mTransparentBitmap, 0, 0, null);

        //draw color bar
        canvas.drawRect(mColorRect, mColorRectPaint);

        //draw color bar thumb
        float thumbX = colorPosition + realLeft;
        float thumbY = mColorRect.top + mColorRect.height() / 2;
        canvas.drawCircle(thumbX, thumbY, mBarHeight / 2 + 5, colorPaint);

        //draw color bar thumb radial gradient shader
        RadialGradient thumbShader = new RadialGradient(thumbX, thumbY, mThumbRadius, toAlpha, null, Shader.TileMode.MIRROR);
        Paint thumbGradientPaint = new Paint();
        thumbGradientPaint.setAntiAlias(true);
        thumbGradientPaint.setShader(thumbShader);
        canvas.drawCircle(thumbX, thumbY, mThumbHeight / 2, thumbGradientPaint);

        if (mIsShowAlphaBar) {

            //init rect
            int top = (int) (mThumbHeight + mThumbRadius + mBarHeight + mBarMargin);
            mAlphaRect = new Rect(realLeft, top, realRight, top + mBarHeight);

            //draw alpha bar
            Paint alphaBarPaint = new Paint();
            alphaBarPaint.setAntiAlias(true);
            LinearGradient alphaBarShader = new LinearGradient(0, 0, mAlphaRect.width(), 0, toAlpha, null, Shader.TileMode.MIRROR);
            alphaBarPaint.setShader(alphaBarShader);
            canvas.drawRect(mAlphaRect, alphaBarPaint);

            //draw alpha bar thumb
            float alphaPosition = (float) mAlphaBarPosition / 255 * mBarWidth;
            float alphaThumbX = alphaPosition + realLeft;
            float alphaThumbY = mAlphaRect.top + mAlphaRect.height() / 2;
            canvas.drawCircle(alphaThumbX, alphaThumbY, mBarHeight / 2 + 5, colorPaint);

            //draw alpha bar thumb radial gradient shader
            RadialGradient alphaThumbShader = new RadialGradient(alphaThumbX, alphaThumbY, mThumbRadius, toAlpha, null, Shader.TileMode.MIRROR);
            Paint alphaThumbGradientPaint = new Paint();
            alphaThumbGradientPaint.setAntiAlias(true);
            alphaThumbGradientPaint.setShader(alphaThumbShader);
            canvas.drawCircle(alphaThumbX, alphaThumbY, mThumbHeight / 2, alphaThumbGradientPaint);
        }

        if(mFirstDraw){

            if(mOnColorChangeLister != null){
                mOnColorChangeLister.onColorChangeListener(mColorBarPosition,mAlphaBarPosition,getColor());
            }

            mFirstDraw = false;
        }

        super.onDraw(canvas);
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        x = event.getX();
        y = event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (isOnBar(mColorRect, x, y)) {
                    mMovingColorBar = true;
                } else if (mIsShowAlphaBar) {
                    if (isOnBar(mAlphaRect, x, y)) {
                        mMovingAlphaBar = true;
                    }
                }
                break;
            case MotionEvent.ACTION_MOVE:
                getParent().requestDisallowInterceptTouchEvent(true);
                if (mMovingColorBar) {
                    float value = (x - realLeft) / mBarWidth * mMaxPosition;
                    mColorBarPosition = (int) value;
                    if (mColorBarPosition < 0) mColorBarPosition = 0;
                    if (mColorBarPosition > mMaxPosition) mColorBarPosition = mMaxPosition;
                } else if (mIsShowAlphaBar) {
                    if (mMovingAlphaBar) {
                        float value = (x - realLeft) / mBarWidth * 255;
                        mAlphaBarPosition = (int) value;
                        if (mAlphaBarPosition < 0) mAlphaBarPosition = 0;
                        if (mAlphaBarPosition > 255) mAlphaBarPosition = 255;
                        setAlphaValue();
                    }
                }
                if (mOnColorChangeLister != null && (mMovingAlphaBar || mMovingColorBar))
                    mOnColorChangeLister.onColorChangeListener(mColorBarPosition, mAlphaBarPosition, getColor());
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                mMovingColorBar = false;
                mMovingAlphaBar = false;
                break;
        }
        return true;
    }


    /**
     * @param r
     * @param x
     * @param y
     * @return whether MotionEvent is performing on bar or not
     */
    private boolean isOnBar(Rect r, float x, float y) {
        if (r.left - mThumbRadius < x && x < r.right + mThumbRadius && r.top - mThumbRadius < y && y < r.bottom + mThumbRadius) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isFirstDraw(){
        return mFirstDraw;
    }



    /**
     * @param value
     * @return color
     */
    private int pickColor(int value) {
        return pickColor((float) value / mMaxPosition * mBarWidth);
    }

    /**
     * @param position
     * @return color
     */
    private int pickColor(float position) {
        float unit = position / mBarWidth;
        if (unit <= 0.0)
            return mColorSeeds[0];

        if (unit >= 1)
            return mColorSeeds[mColorSeeds.length - 1];

        float colorPosition = unit * (mColorSeeds.length - 1);
        int i = (int) colorPosition;
        colorPosition -= i;
        c0 = mColorSeeds[i];
        c1 = mColorSeeds[i + 1];
//         mAlpha = mix(Color.alpha(c0), Color.alpha(c1), colorPosition);
        mRed = mix(Color.red(c0), Color.red(c1), colorPosition);
        mGreen = mix(Color.green(c0), Color.green(c1), colorPosition);
        mBlue = mix(Color.blue(c0), Color.blue(c1), colorPosition);
        return Color.rgb(mRed, mGreen, mBlue);
    }

    /**
     * @param start
     * @param end
     * @param position
     * @return
     */
    private int mix(int start, int end, float position) {
        return start + Math.round(position * (end - start));
    }

    public int getColor() {
        return getColor(mIsShowAlphaBar);
    }

    /**
     * @param withAlpha
     * @return
     */
    public int getColor(boolean withAlpha) {
        //pick mode
        if (mColorBarPosition >= mColors.size()) {
            int color = pickColor(mColorBarPosition);
            if(withAlpha){
                return color;
            }else {
                return Color.argb(getAlphaValue(), Color.red(color), Color.green(color), Color.blue(color));
            }
        }

        //cache mode
        int color = mColors.get(mColorBarPosition);

        if (withAlpha) {
            return Color.argb(getAlphaValue(), Color.red(color), Color.green(color), Color.blue(color));
        }
        return color;
    }

    public int getAlphaBarPosition() {
        return mAlphaBarPosition;
    }

    public int getAlphaValue() {
        return mAlpha;
    }

    public interface OnColorChangeListener {
        /**
         * @param colorBarPosition between 0-maxValue
         * @param alphaBarPosition    between 0-255
         * @param color         return the color contains alpha value whether showAlphaBar is true or without alpha value
         */
        void onColorChangeListener(int colorBarPosition, int alphaBarPosition, int color);
    }

    /**
     * @param onColorChangeListener
     */
    public void setOnColorChangeListener(OnColorChangeListener onColorChangeListener) {
        this.mOnColorChangeLister = onColorChangeListener;
    }


    public int dp2px(float dpValue) {
        final float scale = mContext.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    /**
     * Set colors by resource id. The resource's type must be ArrayRes
     *
     * @param resId
     */
    public void setColorSeeds(@ArrayRes int resId) {
        setColorSeeds(getColorsById(resId));
    }

    public void setColorSeeds(int[] colors) {
        mColorSeeds = colors;
        invalidate();
        cacheColors();
        setAlphaValue();
        if (mOnColorChangeLister != null)
            mOnColorChangeLister.onColorChangeListener(mColorBarPosition, mAlphaBarPosition, getColor());
    }

    /**
     * @param color
     * @return the color's position in the bar, if not in the bar ,return -1;
     */
    public int getColorIndexPosition(int color) {
        return mColors.indexOf(color);
    }

    public List<Integer> getColors() {
        return mColors;
    }

    public boolean isShowAlphaBar() {
        return mIsShowAlphaBar;
    }

    private void refreshLayoutParams() {
        mThumbHeight = mThumbHeight < 2 ? 2 : mThumbHeight;
        mBarHeight = mBarHeight < 2 ? 2 : mBarHeight;

        int singleHeight = mThumbHeight + mBarHeight;
        int doubleHeight = mThumbHeight * 2 + mBarHeight * 2 + mBarMargin;

        if (getLayoutParams().height == -2) {
            if (mIsShowAlphaBar) {
                getLayoutParams().height = doubleHeight;
                setLayoutParams(getLayoutParams());
            } else {
                getLayoutParams().height = singleHeight;
                setLayoutParams(getLayoutParams());
            }
        } else if (getLayoutParams().height >= 0) {
            if (mIsShowAlphaBar) {
                getLayoutParams().height = doubleHeight;
                setLayoutParams(getLayoutParams());
            } else {
                getLayoutParams().height = singleHeight;
                setLayoutParams(getLayoutParams());
            }
        }
    }

    public void setShowAlphaBar(boolean show) {
        mIsShowAlphaBar = show;
        refreshLayoutParams();
        invalidate();
        if (mOnColorChangeLister != null)
            mOnColorChangeLister.onColorChangeListener(mColorBarPosition, mAlphaBarPosition, getColor());
    }

    /**
     * @param dp
     */
    public void setBarHeight(float dp) {
        mBarHeight = dp2px(dp);
        refreshLayoutParams();
        invalidate();
    }

    /**
     * @param px
     */
    public void setBarHeightPx(int px) {
        mBarHeight = px;
        refreshLayoutParams();
        invalidate();
    }

    private void setAlphaValue() {
        mAlpha = 255 - mAlphaBarPosition;
    }

    public void setAlphaBarPosition(int value) {
        this.mAlphaBarPosition = value;
        setAlphaValue();
        invalidate();
    }

    public int getMaxValue() {
        return mMaxPosition;
    }

    public void setMaxPosition(int value) {
        this.mMaxPosition = value;
        invalidate();
        cacheColors();
    }

    /**
     * set margin between bars
     *
     * @param mBarMargin
     */
    public void setBarMargin(float mBarMargin) {
        this.mBarMargin = dp2px(mBarMargin);
        refreshLayoutParams();
        invalidate();
    }

    /**
     * set margin between bars
     *
     * @param mBarMargin
     */
    public void setBarMarginPx(int mBarMargin) {
        this.mBarMargin = mBarMargin;
        refreshLayoutParams();
        invalidate();
    }


    /**
     * Set the value of color bar, if out of bounds , it will be 0 or maxValue;
     *
     * @param value
     */
    public void setColorBarPosition(int value) {
        this.mColorBarPosition = value;
        mColorBarPosition = mColorBarPosition > mMaxPosition ? mMaxPosition : mColorBarPosition;
        mColorBarPosition = mColorBarPosition < 0 ? 0 : mColorBarPosition;
        invalidate();
        if (mOnColorChangeLister != null)
            mOnColorChangeLister.onColorChangeListener(mColorBarPosition, mAlphaBarPosition, getColor());
    }


    /**
     * Set color, it must correspond to the value, if not , setColorBarPosition(0);
     *
     * @paam color
     */
    public void setColor(int color) {
        int withoutAlphaColor = Color.rgb(Color.red(color), Color.green(color), Color.blue(color));

        if (mInit) {
            int value = mColors.indexOf(withoutAlphaColor);
//            mColorsToInvoke = color;
            setColorBarPosition(value);
        } else {
            mColorsToInvoke = color;
        }

    }

    /**
     * set thumb's height by dpi
     *
     * @param dp
     */
    public void setThumbHeight(float dp) {
        this.mThumbHeight = dp2px(dp);
        refreshLayoutParams();
        invalidate();
    }

    /**
     * set thumb's height by pixels
     *
     * @param px
     */
    public void setThumbHeightPx(int px) {
        this.mThumbHeight = px;
        refreshLayoutParams();
        invalidate();
    }

    public int getBarHeight() {
        return mBarHeight;
    }

    public int getThumbHeight() {
        return mThumbHeight;
    }

    public int getBarMargin() {
        return mBarMargin;
    }

    public float getColorBarValue() {
        return mColorBarPosition;
    }

}