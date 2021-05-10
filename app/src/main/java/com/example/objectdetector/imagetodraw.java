package com.example.objectdetector;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.View;

import androidx.core.content.res.ResourcesCompat;

public class imagetodraw extends View {
    Paint text, rect;
    String nameofobj;
    Rect boundingbox;
    public imagetodraw(Context context, String nameofobj, Rect boundingbox) {
        super(context);

        rect = new Paint();
        rect.setColor(Color.BLUE);
        rect.setStrokeWidth(15f);
        rect.setStyle(Paint.Style.STROKE);//stroke to only fill boundary

        text = new Paint();
        text.setColor(Color.BLUE);
        text.setTextSize(40f);
        text.setStrokeWidth(10f);
        text.setStyle(Paint.Style.FILL);
        //asigining
         this.nameofobj = nameofobj;
         this.boundingbox = boundingbox;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawRect(boundingbox, rect);
        canvas.drawText(nameofobj, boundingbox.centerX(), boundingbox.centerY(), text);
    }
}
