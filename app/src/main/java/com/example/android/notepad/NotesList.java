package com.example.android.notepad;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import java.util.Date;

public class NotesList extends ListActivity {

    private Cursor mCursor;
    private SimpleCursorAdapter mAdapter;
    private static final String[] PROJECTION = new String[] {
            NotePad.Notes._ID,
            NotePad.Notes.COLUMN_NAME_TITLE,
            NotePad.Notes.COLUMN_NAME_NOTE,
            NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadNotes();
    }

    private void loadNotes() {
        // 查询 notes
        mCursor = managedQuery(NotePad.Notes.CONTENT_URI,
                PROJECTION, null, null, NotePad.Notes.DEFAULT_SORT_ORDER);

        setupListAdapter(mCursor);
    }

    private void searchNotes(String query) {
        // 构建搜索条件 - 在标题或内容中搜索
        String selection = NotePad.Notes.COLUMN_NAME_TITLE + " LIKE ? OR " +
                NotePad.Notes.COLUMN_NAME_NOTE + " LIKE ?";
        String[] selectionArgs = new String[] { "%" + query + "%", "%" + query + "%" };

        // 执行查询
        mCursor = managedQuery(NotePad.Notes.CONTENT_URI,
                PROJECTION, selection, selectionArgs, NotePad.Notes.DEFAULT_SORT_ORDER);

        setupListAdapter(mCursor);
        setTitle(getString(R.string.search_title) + ": " + query);
    }

    private void setupListAdapter(Cursor cursor) {
        String[] from = new String[] {
                NotePad.Notes.COLUMN_NAME_TITLE,
                NotePad.Notes.COLUMN_NAME_NOTE,
                NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE
        };
        int[] to = new int[] {
                R.id.title,
                R.id.text,
                R.id.modified
        };

        mAdapter = new SimpleCursorAdapter(this, R.layout.noteslist_item, cursor, from, to, 0);

        // 设置 ViewBinder 以格式化时间戳为可读时间
        mAdapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
            @Override
            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                if (view.getId() == R.id.modified) {
                    long millis = 0;
                    try {
                        millis = cursor.getLong(columnIndex);
                    } catch (Exception e) {
                        // fallback 0
                    }
                    if (millis > 0) {
                        String formatted = DateFormat.getDateFormat(NotesList.this).format(new Date(millis))
                                + " " + DateFormat.getTimeFormat(NotesList.this).format(new Date(millis));
                        ((TextView) view).setText(formatted);
                    } else {
                        ((TextView) view).setText("");
                    }
                    return true;
                }
                return false;
            }
        });

        setListAdapter(mAdapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.list_options_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_add) {
            // 创建新笔记的现有逻辑
            Intent intent = new Intent(Intent.ACTION_INSERT, NotePad.Notes.CONTENT_URI);
            startActivity(intent);
            return true;
        } else if (id == R.id.menu_paste) {
            // 粘贴的现有逻辑
            Intent intent = new Intent(Intent.ACTION_PASTE, NotePad.Notes.CONTENT_URI);
            startActivity(intent);
            return true;
        } else if (id == R.id.menu_search) {
            showSearchDialog();
            return true;
        }
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
                dialog.cancel();
            }
        });

        builder.show();
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        // 保持现有跳转逻辑
        Intent intent = new Intent(this, NoteEditor.class);
        intent.setAction(Intent.ACTION_EDIT);
        intent.setData(android.content.ContentUris.withAppendedId(NotePad.Notes.CONTENT_URI, id));
        startActivity(intent);
    }
}
