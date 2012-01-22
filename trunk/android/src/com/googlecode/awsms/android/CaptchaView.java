package com.googlecode.awsms.android;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

/**
 * Image view that resizes automatically, keeping aspect ratio intact.
 * @author Andrea De Pasquale
 */
public class CaptchaView extends View {

  private Drawable captcha = null;
  
  public CaptchaView(Context context) {
    super(context);
  }

  public CaptchaView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public CaptchaView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
  }

  @Override
  public void setBackgroundDrawable(Drawable d) {
    super.setBackgroundDrawable(d);
    captcha = d;
  }
  
  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    if (captcha == null) {
      super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    } else {
      int width = MeasureSpec.getSize(widthMeasureSpec);
      int height = // keep aspect ratio
          width * captcha.getIntrinsicHeight() / captcha.getIntrinsicWidth();
      setMeasuredDimension(width, height);
    }
  }

}
