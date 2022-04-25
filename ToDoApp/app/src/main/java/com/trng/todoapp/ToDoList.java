package com.trng.todoapp;

import static android.graphics.Color.GREEN;
import static android.graphics.Color.RED;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class ToDoList extends AppCompatActivity {
    private Toolbar toolbar;
    private RecyclerView recyclerView;
    private FloatingActionButton floatingActionButton;

    private EditText edtStart, edtEnd, edtTask, edtDescription;
    private Button btnCancel, btnSave;

    private ProgressDialog loader;

    private String key = "";
    private String task;
    private String description;
    private String startDate;
    private String endDate;

    private DatabaseReference reference;

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_to_do_list);

        appInit();

        //Create database
        reference = FirebaseDatabase
                .getInstance("https://todo-e6ab6-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference().child("tasks");

        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addTask();
            }
        });
    }

    private void addTask() {
        //set task_input layout as a dialog showing when click on Add btn
        AlertDialog.Builder myDialog = new AlertDialog.Builder(this);
        LayoutInflater layoutInflater = LayoutInflater.from(this);

        View myView = layoutInflater.inflate(R.layout.task_input, null);
        myDialog.setView(myView);

        AlertDialog dialog = myDialog.create();
        dialog.setCancelable(false);
        dialog.show();

        myViewInit(myView);

        //pick star date time
        edtStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDateTimeDialog(edtStart);
            }
        });

        //pick end date time
        edtEnd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDateTimeDialog(edtEnd);
            }
        });

        //cancel btn event
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        //save btn event
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String mTask = edtTask.getText().toString().trim();
                String mDescription = edtDescription.getText().toString().trim();
                String id = reference.push().getKey();
                String mStartDate = edtStart.getText().toString().trim();
                String mEndDate = edtEnd.getText().toString().trim();

                //check for task, desc, date input not empty
                if (TextUtils.isEmpty(mTask)) {
                    edtTask.setError("Task Required");
                    return;
                }
                if (TextUtils.isEmpty(mDescription)) {
                    edtDescription.setError("Description Required");
                    return;
                }
                if(TextUtils.isEmpty(mStartDate)) {
                    edtStart.setError("Start Date Required");
                    return;
                }
                if(TextUtils.isEmpty(mEndDate)){
                    edtEnd.setError("End Date Required");
                    return;
                }
                try {
                    //check for valid date input
                    if(!isValidDate(mStartDate, mEndDate)){
                        Toast.makeText(ToDoList.this,
                                "Start date must be before end date",
                                Toast.LENGTH_SHORT).show();
                    }
                    //show loading dialog
                    else {
                        loader.setMessage("Adding your data");
                        loader.setCanceledOnTouchOutside(false);
                        loader.show();

                        //insert data to database
                        Items item = new Items(id, mTask, mDescription, mStartDate, mEndDate);
                        reference.child(id).setValue(item)
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if(task.isSuccessful()){
                                    Toast.makeText(ToDoList.this,
                                            "Task has been insert to database successfully",
                                            Toast.LENGTH_SHORT).show();
                                }
                                else{
                                    String error = task.getException().toString();
                                    Toast.makeText(ToDoList.this,
                                            "Failed with error: " + error,
                                            Toast.LENGTH_SHORT).show();
                                }
                                loader.dismiss();
                            }
                        });
                    }
                    dialog.dismiss();
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public boolean toBoolean(int num){
        return num!=0;
    }

    @Override
    protected void onStart() {
        super.onStart();

        FirebaseRecyclerOptions<Items> options = new FirebaseRecyclerOptions.Builder<Items>()
                .setQuery(reference, Items.class)
                .build();

        FirebaseRecyclerAdapter<Items, MyViewHolder> adapter = new FirebaseRecyclerAdapter<Items, MyViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull MyViewHolder holder, @SuppressLint("RecyclerView") int position, @NonNull Items model) {
                holder.setTask(model.getTitle());
                holder.setDate(model.getDate());
                holder.setChecked();

                holder.myView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        key = getRef(position).getKey();
                        task = model.getTitle();
                        description = model.getDescription();
                        startDate = model.getStartTime();
                        endDate = model.getEndTime();

                        updateTask();
                    }
                });
            }

            @NonNull
            @Override
            public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_showing, parent, false);
                return new MyViewHolder(view);
            }
        };
        recyclerView.setAdapter(adapter);
        adapter.startListening();
    }

    //Set task and date for items
    public class MyViewHolder extends RecyclerView.ViewHolder {
        View myView;
        TextView cbTask;
        TextView txtDate;
        ImageButton btnChecked;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            myView = itemView;
        }

        public void setTask(String task) {
            cbTask = myView.findViewById(R.id.cbTask);
            cbTask.setText(task);
        }

        public void setDate(String date){
            txtDate = myView.findViewById(R.id.txtDate);
            txtDate.setText(date);
        }

        public void setChecked(){
            final int[] click = {0};
            btnChecked = myView.findViewById(R.id.imgChecked);
            btnChecked.setOnClickListener(new View.OnClickListener() {
                @SuppressLint("ResourceAsColor")
                @Override
                public void onClick(View view) {
                    click[0] += 1;
                    if (click[0] > 2) {
                        click[0] = 1;
                    }
                    switch (click[0]){
                        case 1 : btnChecked.setBackgroundColor(GREEN);
                        break;
                        case 2: btnChecked.setBackgroundColor(RED);
                        break;
                    }
                }
            });
        }
    }

    private void updateTask(){
        //set update_task layout as a dialog showing when click on an item
        AlertDialog.Builder myDialog = new AlertDialog.Builder(this);
        LayoutInflater layoutInflater = LayoutInflater.from(this);

        View myView2 = layoutInflater.inflate(R.layout.update_task, null);
        myDialog.setView(myView2);

        AlertDialog dialog = myDialog.create();
        dialog.setCancelable(false);
        dialog.show();

        EditText edtTaskU = myView2.findViewById(R.id.edtTaskU);
        EditText edtStartU = myView2.findViewById(R.id.edtStartU);
        edtStartU.setInputType(InputType.TYPE_NULL);
        EditText edtEndU = myView2.findViewById(R.id.edtEndU);
        edtEndU.setInputType(InputType.TYPE_NULL);
        EditText edtDesU = myView2.findViewById(R.id.edtDesU);

        edtTaskU.setText(task);
        edtTaskU.setSelection(task.length());

        edtStartU.setText(startDate);
        edtStartU.setSelection(startDate.length());

        edtEndU.setText(endDate);
        edtEndU.setSelection(endDate.length());

        edtDesU.setText(description);
        edtDesU.setSelection(description.length());

        ImageButton btnBack = myView2.findViewById(R.id.btnBack);
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });

        //pick star date time
        edtStartU.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDateTimeDialog(edtStartU);
            }
        });

        //pick end date time
        edtEndU.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDateTimeDialog(edtEndU);
            }
        });

        Button btnDelete = myView2.findViewById(R.id.btnDelete);
        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                reference.child(key).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if(task.isSuccessful()){
                            Toast.makeText(ToDoList.this, "Task has been deleted successfully", Toast.LENGTH_SHORT).show();
                        }
                        else{
                            String error = task.getException().toString();
                            Toast.makeText(ToDoList.this, "Failed with error: " + error, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                dialog.dismiss();
            }
        });

        Button btnUpdate = myView2.findViewById(R.id.btnUpdate);
        btnUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String mTask = edtTaskU.getText().toString().trim();
                String mDescription = edtDesU.getText().toString().trim();
                String id = reference.push().getKey();
                String mStartDate = edtStartU.getText().toString().trim();
                String mEndDate = edtEndU.getText().toString().trim();

                //check for task, desc, date input not empty
                if (TextUtils.isEmpty(mTask)) {
                    edtTaskU.setError("Task Required");
                    return;
                }
                if (TextUtils.isEmpty(mDescription)) {
                    edtDesU.setError("Description Required");
                    return;
                }
                if(TextUtils.isEmpty(mStartDate)) {
                    edtStartU.setError("Start Date Required");
                    return;
                }
                if(TextUtils.isEmpty(mEndDate)){
                    edtEndU.setError("End Date Required");
                    return;
                }
                try {
                    //check for valid date input
                    if(!isValidDate(mStartDate, mEndDate)){
                        Toast.makeText(ToDoList.this,
                                "Start date must be before end date",
                                Toast.LENGTH_SHORT).show();
                    }
                    //show loading dialog
                    else {
                        loader.setMessage("Updating your data");
                        loader.setCanceledOnTouchOutside(false);
                        loader.show();

                        //insert data to database
                        Items item = new Items(id, mTask, mDescription, mStartDate, mEndDate);
                        reference.child(key).setValue(item)
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if(task.isSuccessful()){
                                            Toast.makeText(ToDoList.this,
                                                    "Task has been updated to database successfully",
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                        else{
                                            String error = task.getException().toString();
                                            Toast.makeText(ToDoList.this,
                                                    "Failed with error: " + error,
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                        loader.dismiss();
                                    }
                                });
                    }
                    dialog.dismiss();
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    //show time and date picker dialog
    private void showDateTimeDialog(final EditText date_time_in) {
        final Calendar calendar=Calendar.getInstance();
        DatePickerDialog.OnDateSetListener dateSetListener=new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                calendar.set(Calendar.YEAR,year);
                calendar.set(Calendar.MONTH,month);
                calendar.set(Calendar.DAY_OF_MONTH,dayOfMonth);

                TimePickerDialog.OnTimeSetListener timeSetListener=new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        calendar.set(Calendar.HOUR_OF_DAY,hourOfDay);
                        calendar.set(Calendar.MINUTE,minute);

                        SimpleDateFormat simpleDateFormat=new SimpleDateFormat("yy-MM-dd HH:mm");

                        date_time_in.setText(simpleDateFormat.format(calendar.getTime()));
                    }
                };
                new TimePickerDialog(ToDoList.this,
                        timeSetListener,
                        calendar.get(Calendar.HOUR_OF_DAY),
                        calendar.get(Calendar.MINUTE),
                        false).show();
            }
        };
        new DatePickerDialog(this,
                dateSetListener,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    //check for start date is before end date
    private boolean isValidDate(String startDate, String endDate) throws ParseException {
        Date start = new SimpleDateFormat("yy-MM-dd HH:mm").parse(startDate);
        Date end = new SimpleDateFormat("yy-MM-dd HH:mm").parse(endDate);
        if(start.before(end)){
            return true;
        }
        return false;
    }

    private void appInit(){

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Todo list");

        recyclerView = findViewById(R.id.todoList);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setReverseLayout(true);
        linearLayoutManager.setStackFromEnd(true);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(linearLayoutManager);

        floatingActionButton = findViewById(R.id.btnAdd);

        loader = new ProgressDialog(this);

    }

    private void myViewInit(View myView){
        edtStart = myView.findViewById(R.id.edtStart);
        edtStart.setInputType(InputType.TYPE_NULL);
        edtEnd = myView.findViewById(R.id.edtEnd);
        edtEnd.setInputType(InputType.TYPE_NULL);
        edtTask = myView.findViewById(R.id.edtTask);
        edtDescription = myView.findViewById(R.id.edtDes);
        btnCancel = myView.findViewById(R.id.btnCancel);
        btnSave = myView.findViewById(R.id.btnSave);
    }
}