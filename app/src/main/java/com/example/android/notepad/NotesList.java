package com.example.android.notepad;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import android.content.ContentValues;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.Context;
import android.net.Uri;

import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.Locale;

// 1. 实现 LoaderManager.LoaderCallbacks<Cursor> 接口
public class NotesList extends ListActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    // 2. 移除 mCursor 成员变量，LoaderManager 会管理游标
    private SimpleCursorAdapter mAdapter;

    private static final String[] PROJECTION = new String[] {
            NotePad.Notes._ID,
            NotePad.Notes.COLUMN_NAME_TITLE,
            NotePad.Notes.COLUMN_NAME_NOTE,
            NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE,
            NotePad.Notes.COLUMN_NAME_BACK_COLOR
    };

    // 3. 定义 Loader ID
    private static final int NOTES_LIST_LOADER_ID = 1;

    // 用于搜索查询的 Bundle 键
    private static final String LOADER_ARG_QUERY = "query";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 4. 初始化 Adapter，使用 null 游标
        setupListAdapter(null);

        // 5. 启动初始加载器
        loadNotes();
    }

    /**
     * 启动或重新启动主笔记列表的加载器。
     */
    private void loadNotes() {
        setTitle(R.string.app_name); // 重置标题为应用名
        if (getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(false);
            getActionBar().setHomeButtonEnabled(false);
        }
        // 初始化 Loader。如果 Loader 已存在，则不进行任何操作；如果不存在，则创建并启动。
        // 第三个参数 'this' 指向本 Activity，即 LoaderManager.LoaderCallbacks 的实现者
        getLoaderManager().restartLoader(NOTES_LIST_LOADER_ID, null, this);
    }

    /**
     * 执行笔记搜索。
     */
    private void searchNotes(String query) {
        setTitle(getString(R.string.search_title) + ": " + query);
        if (getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
            getActionBar().setHomeButtonEnabled(true);
        }

        Bundle args = new Bundle();
        args.putString(LOADER_ARG_QUERY, query);

        // 重新启动 Loader 以执行搜索。
        // 使用与主列表相同的 ID，但传递参数
        getLoaderManager().restartLoader(NOTES_LIST_LOADER_ID, args, this);
    }

    /**
     * 初始化 SimpleCursorAdapter。
     * @param cursor 初始游标（通常为 null）
     */
    private void setupListAdapter(Cursor cursor) {
        String[] from = new String[] {
                NotePad.Notes.COLUMN_NAME_TITLE,
                NotePad.Notes.COLUMN_NAME_NOTE,
                NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE,
                NotePad.Notes.COLUMN_NAME_BACK_COLOR
        };
        int[] to = new int[] {
                R.id.title,
                R.id.text,
                R.id.modified,
                R.id.back_color
        };

        // 6. Adapter 只初始化一次，游标由 Loader 异步提供
        if (mAdapter == null) {
            // 参数 flag 设置为 0 (无需额外的游标管理)
            mAdapter = new SimpleCursorAdapter(this, R.layout.noteslist_item, cursor, from, to, 0);

            // 设置 ViewBinder 以格式化时间戳为可读时间
            mAdapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
                @Override
                public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                    if (view.getId() == R.id.modified) {
                        long millis = 0;
                        try {
                            // 确保列数据是 long 类型的时间戳
                            millis = cursor.getLong(columnIndex);
                        } catch (Exception e) {
                            // 捕获异常，例如游标数据为空或类型不匹配
                            // Log.e("NotesList", "Error getting timestamp: " + e.getMessage());
                        }

                        if (millis > 0) {
                            // 格式化日期和时间
                            // String formatted = DateFormat.getDateFormat(NotesList.this).format(new Date(millis))
                            //        + " " + DateFormat.getTimeFormat(NotesList.this).format(new Date(millis));
                            
                            // 修改为北京时间格式
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                            sdf.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
                            String formatted = sdf.format(new Date(millis));
                            
                            ((TextView) view).setText(formatted);
                        } else {
                            ((TextView) view).setText("");
                        }
                        return true;
                    } else if (view.getId() == R.id.back_color) {
                        int x = cursor.getInt(columnIndex);
                        if (x != 0) {
                            view.setBackgroundColor(x);
                        }
                        else {
                            view.setBackgroundColor(Color.rgb(255, 255, 255));
                        }
                        return true;
                    }
                    return false;
                }
            });

            setListAdapter(mAdapter);
        }
    }

    // =========================================================================
    // LoaderManager.LoaderCallbacks<Cursor> 实现
    // =========================================================================

    /**
     * 7. 在需要创建新的 Loader 时调用
     */
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String selection = null;
        String[] selectionArgs = null;
        String query = null;

        if (id == NOTES_LIST_LOADER_ID) {
            // 检查是否有搜索参数
            if (args != null && args.containsKey(LOADER_ARG_QUERY)) {
                query = args.getString(LOADER_ARG_QUERY);
            }

            if (query != null && !query.isEmpty()) {
                // 搜索条件：在标题或内容中搜索
                selection = NotePad.Notes.COLUMN_NAME_TITLE + " LIKE ? OR " +
                        NotePad.Notes.COLUMN_NAME_NOTE + " LIKE ?";
                selectionArgs = new String[] { "%" + query + "%", "%" + query + "%" };
            }

            // 创建并返回 CursorLoader
            return new CursorLoader(
                    this,
                    NotePad.Notes.CONTENT_URI,
                    PROJECTION,
                    selection,
                    selectionArgs,
                    NotePad.Notes.DEFAULT_SORT_ORDER
            );
        }
        return null;
    }

    /**
     * 8. 在 Loader 完成数据加载时调用
     */
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // 使用新的 Cursor 交换 Adapter 中的旧 Cursor。
        // Loader 会自动管理旧 Cursor 的关闭。
        mAdapter.swapCursor(data);
    }

    /**
     * 9. 在 Loader 被重置（例如 Activity 销毁）时调用
     */
    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // 将 Adapter 的 Cursor 设为 null，防止它试图访问已关闭的 Cursor
        mAdapter.swapCursor(null);
    }

    // =========================================================================
    // 菜单和点击事件 (保持不变)
    // =========================================================================

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.list_options_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            loadNotes();
            return true;
        }
        if (id == R.id.menu_add) {
            // 插入一条空笔记并以 ACTION_EDIT 打开，以便可以编辑标题
            ContentValues values = new ContentValues();
            long now = System.currentTimeMillis();
            values.put(NotePad.Notes.COLUMN_NAME_TITLE, "");
            values.put(NotePad.Notes.COLUMN_NAME_NOTE, "");
            values.put(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE, now);
            values.put(NotePad.Notes.COLUMN_NAME_BACK_COLOR, Color.rgb(255, 255, 255));

            Uri newUri = getContentResolver().insert(NotePad.Notes.CONTENT_URI, values);
            if (newUri != null) {
                Intent intent = new Intent(this, NoteEditor.class);
                intent.setAction(Intent.ACTION_EDIT);
                intent.setData(newUri);
                startActivity(intent);
            }
            return true;
        } else if (id == R.id.menu_paste) {
            // 从剪贴板获取文本并插入新笔记，然后以 ACTION_EDIT 打开
            String text = "";
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null && clipboard.hasPrimaryClip()) {
                ClipData cd = clipboard.getPrimaryClip();
                if (cd != null && cd.getItemCount() > 0) {
                    CharSequence cs = cd.getItemAt(0).coerceToText(this);
                    if (cs != null) text = cs.toString();
                }
            }

            ContentValues values = new ContentValues();
            long now = System.currentTimeMillis();
            values.put(NotePad.Notes.COLUMN_NAME_TITLE, "");
            values.put(NotePad.Notes.COLUMN_NAME_NOTE, text);
            values.put(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE, now);
            values.put(NotePad.Notes.COLUMN_NAME_BACK_COLOR, Color.rgb(255, 255, 255));

            Uri newUri = getContentResolver().insert(NotePad.Notes.CONTENT_URI, values);
            if (newUri != null) {
                Intent intent = new Intent(this, NoteEditor.class);
                intent.setAction(Intent.ACTION_EDIT);
                intent.setData(newUri);
                startActivity(intent);
            }
            return true;
        } else if (id == R.id.menu_search) {
            showSearchDialog();
            return true;
        }
        // 如果用户选择了“清除搜索”之类的选项，可以在这里调用 loadNotes()
        return super.onOptionsItemSelected(item);
    }

    private void showSearchDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.menu_search);

        final EditText input = new EditText(this);
        input.setHint(R.string.search_hint);
        builder.setView(input);

        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String query = input.getText().toString().trim();
                if (!query.isEmpty()) {
                    searchNotes(query);
                }
            }
        });

        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // 如果用户点击取消，并且当前是搜索结果，可以考虑重新加载所有笔记
                // loadNotes(); // (可选)
                dialog.cancel();
            }
        });

        builder.show();
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Intent intent = new Intent(this, NoteEditor.class);
        intent.setAction(Intent.ACTION_EDIT);
        intent.setData(android.content.ContentUris.withAppendedId(NotePad.Notes.CONTENT_URI, id));
        startActivity(intent);
    }
}
