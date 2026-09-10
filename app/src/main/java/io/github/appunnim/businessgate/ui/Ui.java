package io.github.appunnim.businessgate.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import io.github.appunnim.businessgate.R;

final class Ui {
    private Ui(){}
    static int dp(Context c,float value){return Math.round(value*c.getResources().getDisplayMetrics().density);}
    static LinearLayout column(Context c){LinearLayout v=new LinearLayout(c);v.setOrientation(LinearLayout.VERTICAL);return v;}
    static LinearLayout row(Context c){LinearLayout v=new LinearLayout(c);v.setOrientation(LinearLayout.HORIZONTAL);v.setGravity(Gravity.CENTER_VERTICAL);return v;}
    static TextView text(Context c,CharSequence value,float sp,int color,boolean bold){
        TextView v=new TextView(c);v.setText(value);v.setTextSize(sp);v.setTextColor(c.getColor(color));
        v.setFontFeatureSettings("kern");v.setLineSpacing(dp(c,2),1.05f);v.setIncludeFontPadding(true);
        if(bold)v.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));return v;
    }
    static GradientDrawable shape(Context c,int color,boolean stroke){
        GradientDrawable d=new GradientDrawable();d.setColor(c.getColor(color));d.setCornerRadius(dp(c,12));if(stroke)d.setStroke(dp(c,1),c.getColor(R.color.line));return d;
    }
    static Button button(Context c,String label,boolean primary,Runnable action){
        Button b=new Button(c);b.setText(label);b.setAllCaps(false);b.setTextSize(14);b.setMinHeight(dp(c,48));b.setMinimumHeight(dp(c,48));
        b.setMinWidth(dp(c,48));b.setMinimumWidth(dp(c,48));b.setPadding(dp(c,12),dp(c,8),dp(c,12),dp(c,8));
        b.setTextColor(c.getColor(primary?R.color.on_accent:R.color.accent));
        b.setStateListAnimator(null);b.setElevation(0);b.setTranslationZ(0);
        b.setBackground(new RippleDrawable(ColorStateList.valueOf(c.getColor(R.color.line)),shape(c,primary?R.color.accent:android.R.color.transparent,false),null));
        b.setOnClickListener(v->action.run());return b;
    }
    static android.widget.ScrollView scroll(Context context,View content){android.widget.ScrollView scroll=new android.widget.ScrollView(context);scroll.addView(content);return scroll;}
    static void pad(View v,int horizontal,int vertical){v.setPadding(dp(v.getContext(),horizontal),dp(v.getContext(),vertical),dp(v.getContext(),horizontal),dp(v.getContext(),vertical));}
    static void gap(LinearLayout parent,int dp){View v=new View(parent.getContext());parent.addView(v,new LinearLayout.LayoutParams(1,dp(parent.getContext(),dp)));}
}
