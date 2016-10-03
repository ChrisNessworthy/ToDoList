package adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.todolist.R;

import java.util.List;

import classes.CheckCallbackCallback;
import classes.ToDoItem;

public class ToDoAdapter extends ArrayAdapter<ToDoItem> {

    private Context context;
    CheckCallbackCallback cb;
    public LayoutInflater mInflater;

    public class ViewHolder {
        View view;
        CheckBox checkBox;
        TextView title;
    }

    public ToDoAdapter(List<ToDoItem> toDoItems, Context context, CheckCallbackCallback cb) {
        super(context, 0, toDoItems);
        this.cb = cb;
        mInflater = ( LayoutInflater )getContext().
                getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final ToDoItem item = getItem(position);

        ViewHolder holder;

        if(convertView == null){
            holder = getViewHolder();

            convertView = holder.view;

            convertView.setTag(R.string.TAG, holder);
        } else {
            holder = (ViewHolder) convertView.getTag(R.string.TAG);
        }

        if (holder == null) {
            holder = getViewHolder();
        }

        convertView = holder.view;

        convertView.setTag(R.string.ITEM_ID_TAG, item.getId());

        if (item.getText() != null) {
            holder.title.setText(item.getText());
        }

        if (item.isDone()) {
            holder.checkBox.setChecked(true);
        } else {
            holder.checkBox.setChecked(false);
        }

        holder.checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                cb.onCheck(item, b);
            }
        });

        holder.title.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cb.onClick(item);
            }
        });

        return convertView;
    }

    private ViewHolder getViewHolder(){
        ViewHolder holder = new ViewHolder();

        holder.view = mInflater.inflate(R.layout.list_item, null);
        holder.checkBox = (CheckBox) holder.view.findViewById(R.id.checkbox);
        holder.title = (TextView) holder.view.findViewById(R.id.title);

        return holder;
    }
}
