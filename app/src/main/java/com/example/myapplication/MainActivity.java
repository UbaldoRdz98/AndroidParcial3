package com.example.myapplication;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

public class MainActivity extends AppCompatActivity {
    private FirebaseFirestore db;
    private CollectionReference infoCollection;
    private EditText txtName;
    private EditText txtHeight;
    private EditText txtWeight;
    private EditText txtImc;
    private Button btnCalc;
    private TableLayout tableLayout;
    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        txtName = findViewById(R.id.txtName);
        txtHeight = findViewById(R.id.txtHeight);
        txtWeight = findViewById(R.id.txtWeight);
        txtImc = findViewById(R.id.txtImc);
        btnCalc =  findViewById(R.id.btnCalc);
        tableLayout = findViewById(R.id.tableLayout);
        txtHeight.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        txtWeight.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        txtImc.setEnabled(false);

        db = FirebaseFirestore.getInstance();
        infoCollection = db.collection("users");
        btnCalc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = txtName.getText().toString();
                Double height = Double.parseDouble(txtHeight.getText().toString());
                Double weight = Double.parseDouble(txtWeight.getText().toString());
                Double imc = weight / (height * height);

                DecimalFormat df = new DecimalFormat("#.##");
                String redNumber = df.format(imc);
                imc = Double.parseDouble(redNumber);
                txtImc.setText(imc.toString());
                saveInfo(name, imc);
            }
        });
        readInfo();
    }
    private void saveInfo(String name, double imc) {
        if (!name.isEmpty()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            String mess = "";

            if(imc < 18.5) {
                mess = "Su IMC es de: " + imc + " Tienes bajo peso";
            } else if (imc >= 18.5 && imc <= 24.9) {
                mess = "Su IMC es de: " + imc + " Tiene peso normal";
            } else if (imc >= 25.0 && imc <= 29.9) {
                mess = "Su IMC es de: " + imc + " Tiene sobrepeso";
            } else if (imc >= 30.0) {
                mess = "Su IMC es de: " + imc + " Tiene obesidad";
            }

            builder.setMessage(mess)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.dismiss();
                        }
                    });
            AlertDialog dialog = builder.create();
            dialog.show();

            Date date = new Date();
            Map<String, Object> user = new HashMap<>();
            user.put("Name", name);
            user.put("IMC", imc);
            user.put("Date", date);
            infoCollection.add(user)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(MainActivity.this, "Información guardada en Firestore", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(MainActivity.this, "Error al guardar información: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
            updateTable(name, imc, date);
        } else {
            Toast.makeText(MainActivity.this, "Por favor ingresa texto", Toast.LENGTH_SHORT).show();
        }
    }
    private void readInfo() {
        infoCollection.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        String name = document.getString("Name");
                        double imc = document.getDouble("IMC");
                        Date date = document.getDate("Date");
                        updateTable(name, imc, date);
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Error getting documents: " + task.getException(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    private void updateTable(String name, double imc, Date date) {
        TableRow newRow = new TableRow(this);
        newRow.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
        TableRow.LayoutParams params = new TableRow.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        params.setMarginStart(10);
        params.setMarginEnd(10);

        TextView nameTextView = new TextView(this);
        nameTextView.setLayoutParams(params);
        nameTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 25);
        nameTextView.setText(name);
        newRow.addView(nameTextView);

        TextView imcTextView = new TextView(this);
        imcTextView.setLayoutParams(params);
        imcTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 25);
        imcTextView.setText(String.valueOf(imc));
        newRow.addView(imcTextView);

        TextView dateTextView = new TextView(this);
        dateTextView.setLayoutParams(params);
        dateTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 25);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String formattedDate = dateFormat.format(date);
        dateTextView.setText(String.valueOf(formattedDate));
        newRow.addView(dateTextView);

        tableLayout.addView(newRow);
    }

    public class DecimalDigitsInputFilter implements InputFilter {
        private static final int DECIMAL_DIGITS = 2;

        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            StringBuilder builder = new StringBuilder(dest);
            builder.replace(dstart, dend, source.subSequence(start, end).toString());
            if (!builder.toString().matches("^\\d+(\\.\\d{0," + DECIMAL_DIGITS + "})?$")) {
                if (source.length() == 0)
                    return dest.subSequence(dstart, dend);
                return "";
            }
            return null;
        }
    }

}