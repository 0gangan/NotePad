package com.example.android.notepad;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.view.View;
import android.widget.RemoteViews;

public class NoteWidgetProvider extends AppWidgetProvider {

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {

        // 获取 Widget 存储的 Note ID
        long noteId = NoteWidgetConfigureActivity.loadNoteIdPref(context, appWidgetId);
        if (noteId == -1) {
            return;
        }

        // 查询笔记内容
        Uri noteUri = android.content.ContentUris.withAppendedId(NotePad.Notes.CONTENT_URI, noteId);
        Cursor cursor = context.getContentResolver().query(
                noteUri,
                new String[] { NotePad.Notes.COLUMN_NAME_TITLE, NotePad.Notes.COLUMN_NAME_NOTE, NotePad.Notes.COLUMN_NAME_BACK_COLOR },
                null,
                null,
                null
        );

        String title = "";
        String text = "";
        int color = Color.WHITE;

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                title = cursor.getString(cursor.getColumnIndexOrThrow(NotePad.Notes.COLUMN_NAME_TITLE));
                text = cursor.getString(cursor.getColumnIndexOrThrow(NotePad.Notes.COLUMN_NAME_NOTE));
                color = cursor.getInt(cursor.getColumnIndexOrThrow(NotePad.Notes.COLUMN_NAME_BACK_COLOR));
            }
            cursor.close();
        }

        // 构建 RemoteViews
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_note);
        views.setTextViewText(R.id.widget_title, title);
        views.setTextViewText(R.id.widget_text, text);
        
        // 设置背景颜色
        // 注意：RemoteViews 不支持直接设置 GradientDrawable 的颜色，需要使用 setInt 方法修改 ImageView/Layout 的背景色
        // 但这里背景是一个 shape drawable。
        // 简单起见，这里可以设置 layout 的背景色，但这会覆盖掉圆角效果。
        // 为了保留圆角，通常需要为每种颜色准备 drawable 资源，或者使用 setInt 调用 setColorFilter (如果是纯色图片)。
        // 鉴于 NotePad 的实现，我们可以尝试直接设置背景色，虽然可能会丢失圆角，或者我们可以不做处理，默认为白色。
        // 更好的方式是生成一个 Bitmap。
        // 这里为了简单，如果颜色不是白色，我们就设置背景色。
        if (color != 0 && color != Color.WHITE) {
            views.setInt(R.id.widget_layout, "setBackgroundColor", color);
        } else {
            views.setInt(R.id.widget_layout, "setBackgroundResource", R.drawable.card_background);
        }

        // 点击 Widget 跳转到编辑页面
        Intent intent = new Intent(context, NoteEditor.class);
        intent.setAction(Intent.ACTION_EDIT);
        intent.setData(noteUri);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_layout, pendingIntent);

        // 更新 Widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // 更新所有 Widget
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        // 删除 Widget 时清除 Preference
        for (int appWidgetId : appWidgetIds) {
            NoteWidgetConfigureActivity.deleteNoteIdPref(context, appWidgetId);
        }
    }
}
