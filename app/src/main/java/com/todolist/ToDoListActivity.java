package com.todolist;

import android.app.Dialog;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.support.ConnectionSource;
import com.mobeta.android.dslv.DragSortListView;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.Callable;

import ORMlite.DatabaseHelper;
import adapters.ToDoAdapter;
import classes.CheckCallbackCallback;
import classes.ToDoItem;

public class ToDoListActivity extends AppCompatActivity {

    private DatabaseHelper databaseHelper;
    private ConnectionSource connection;
    private Dao<ToDoItem, Integer> toDoItemDao;
    ArrayList<ToDoItem> toDoItems;

    ToDoAdapter adapter;

    DragSortListView sortListView;
    TextView noItems;

    //On Drop listener for when item position changes
    private DragSortListView.DropListener onDrop =
            new DragSortListView.DropListener() {
                @Override
                public void drop(int from, int to) {
                    if (from != to) {
                        ToDoItem item = adapter.getItem(from);
                        adapter.remove(item);
                        adapter.insert(item, to);
                        updateItemPosition();
                    }
                }
            };

    //Listener for when an item is removed
    private DragSortListView.RemoveListener onRemove =
            new DragSortListView.RemoveListener() {
                @Override
                public void remove(int which) {
                    ToDoItem item = adapter.getItem(which);
                    adapter.remove(item);
                    deleteItem(item);
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_to_do_list);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int itemsInList;
                if (toDoItems != null && !toDoItems.isEmpty()) {
                    itemsInList = 0;
                } else {
                    itemsInList = toDoItems.size();
                }
                displayToDoItemDialog(true, null, itemsInList);
            }
        });
        databaseHelper = getHelper();
        connection = databaseHelper.getConnectionSource();

        noItems = (TextView) findViewById(R.id.no_items);

        sortListView = (DragSortListView) findViewById(R.id.sort_list_view);
        sortListView.setDropListener(onDrop);
        sortListView.setRemoveListener(onRemove);

        getToDoData();

    }

    //Gets the DB data and sends it to the adapter
    private void getToDoData(){
        try {
            toDoItemDao = databaseHelper.getDao(ToDoItem.class);
            toDoItems = (ArrayList<ToDoItem>) toDoItemDao.queryForAll();

            if (toDoItems.isEmpty()) {
                displayNoItemMessage();
            } else {
                noItems.setVisibility(View.GONE);
                Collections.sort(toDoItems, new toDoItemComparator());
                buildAdapter(toDoItems);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    //Builds the adapter and listen for clicks and check/uncheck events
    private void buildAdapter(ArrayList<ToDoItem> items){
        adapter = new ToDoAdapter(items, this, new CheckCallbackCallback() {
            @Override
            public void onCheck(ToDoItem item, boolean checkStatus) {
                item.setDone(checkStatus);
                updateItem(item);
            }

            @Override
            public void onClick(ToDoItem item) {
                int itemsInList;
                if (toDoItems != null && !toDoItems.isEmpty()) {
                    itemsInList = 0;
                } else {
                    itemsInList = toDoItems.size();
                }
                displayToDoItemDialog(false, item, itemsInList);
            }
        });
        sortListView.setAdapter(adapter);
    }

    private void displayNoItemMessage() {
        noItems.setVisibility(View.VISIBLE);
    }

    private void deleteItem(ToDoItem item) {
        try {
            toDoItemDao.delete(item);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateItemPosition() {
        for (int i = 0; i < toDoItems.size(); i++) {
            toDoItems.get(i).setPosition(i);
        }

        try {
            toDoItemDao.callBatchTasks(new Callable<Object>() {
                @Override
                public ToDoItem call() throws Exception {
                    for (ToDoItem toDoItem : toDoItems) {
                        toDoItemDao.createOrUpdate(toDoItem);
                    }
                    return null;
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    };

    public DatabaseHelper getHelper() {
        if (databaseHelper == null) {
            databaseHelper = OpenHelperManager.getHelper(this,
                    DatabaseHelper.class);
        }
        return databaseHelper;
    }

    public class toDoItemComparator implements Comparator<ToDoItem> {
        public int compare(ToDoItem obj1, ToDoItem obj2) {
            return (obj1.getPosition() + "").compareToIgnoreCase((obj2.getPosition() + ""));
        }
    }

    //Displays a dialog to add or edit a new item
    public void displayToDoItemDialog(final boolean newItem, final ToDoItem item, final int itemsInList){
        final Dialog itemDialog = new Dialog(this);
        itemDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        itemDialog.getWindow().getAttributes().width = WindowManager.LayoutParams.MATCH_PARENT;
        View itemDialogView = ((LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE)).inflate(R.layout.dialog_item, null, false);
        final EditText item_title = (EditText) itemDialogView.findViewById(R.id.item_title);
        final TextView cancel = (TextView) itemDialogView.findViewById(R.id.cancel);
        final TextView save = (TextView) itemDialogView.findViewById(R.id.save);

        if (!newItem) {
            item_title.setText(item.getText());
        }

        itemDialog.setCancelable(true);
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (newItem) {
                    ToDoItem newItem = new ToDoItem();
                    newItem.setText(item_title.getText().toString());
                    newItem.setPosition(itemsInList + 2);
                    newItem.setDone(false);
                    addItem(newItem);
                    itemDialog.dismiss();
                } else {
                    item.setText(item_title.getText().toString());
                    updateItem(item);
                    itemDialog.dismiss();
                }
            }
        });

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                itemDialog.dismiss();
            }
        });

        itemDialog.setContentView(itemDialogView);
        itemDialog.show();
    }

    private void updateItem(ToDoItem item) {
        try {
            toDoItemDao.createOrUpdate(item);
            getToDoData();
            adapter.notifyDataSetChanged();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addItem(ToDoItem item) {
        try {
            toDoItemDao.createOrUpdate(item);
            getToDoData();
            adapter.notifyDataSetChanged();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
