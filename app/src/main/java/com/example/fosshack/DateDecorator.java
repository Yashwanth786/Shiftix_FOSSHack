package com.example.fosshack;

import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.Color;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.ForegroundColorSpan;
import android.graphics.Typeface;

import androidx.annotation.ColorInt;

import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;

import java.util.HashSet;

public class DateDecorator implements DayViewDecorator {
    private final HashSet<CalendarDay> dates;
    private final Drawable highlightDrawable;
    private final int textColor;

    public DateDecorator(HashSet<CalendarDay> dates, @ColorInt int circleColor, @ColorInt int textColor) {
        this.dates = dates;
        this.highlightDrawable = createCircleDrawable(circleColor);
        this.textColor = textColor;
    }

    @Override
    public boolean shouldDecorate(CalendarDay day) {
        return dates.contains(day);
    }

    @Override
    public void decorate(DayViewFacade view) {
        view.setSelectionDrawable(highlightDrawable);
        view.addSpan(new ForegroundColorSpan(textColor));
        view.addSpan(new StyleSpan(Typeface.BOLD));
        view.addSpan(new RelativeSizeSpan(1.2f));
    }

    private Drawable createCircleDrawable(@ColorInt int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(color);
        drawable.setSize(100, 100);
        return drawable;
    }
}
