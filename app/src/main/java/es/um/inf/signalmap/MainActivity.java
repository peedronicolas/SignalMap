package es.um.inf.signalmap;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Toast;

import com.developer.filepicker.controller.DialogSelectionListener;
import com.developer.filepicker.model.DialogConfigs;
import com.developer.filepicker.model.DialogProperties;
import com.developer.filepicker.view.FilePickerDialog;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    private FilePickerDialog dialogFilePicker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void onClick_btn1_mainActivity(View v) {

        // Si hacemos click mostramos por pantalla el Cuadro de Dialogo para solicitar la info del recorrido
        createInfoDialog();

        // TODO: Quitar bypass.
        // Intent intent = new Intent(MainActivity.this, MapsActivity.class);
        // intent.putExtra("nombreRecorrido", "Recorrido_UMU");
        // intent.putExtra("tecnologia", "2G");
        // startActivity(intent);
    }

    public void onClick_btn2_mainActivity(View v) {
        // Configuramos las propiedades del FilePicker
        // https://github.com/TutorialsAndroid/FilePicker
        DialogProperties properties = new DialogProperties();
        properties.selection_mode = DialogConfigs.SINGLE_MODE;
        properties.selection_type = DialogConfigs.FILE_SELECT;
        properties.root = new File(DialogConfigs.DEFAULT_DIR);
        properties.error_dir = new File(DialogConfigs.DEFAULT_DIR);
        properties.offset = new File(DialogConfigs.DEFAULT_DIR);
        properties.extensions = new String[]{"json"};
        properties.show_hidden_files = false;
        dialogFilePicker = new FilePickerDialog(MainActivity.this, properties);
        dialogFilePicker.setTitle(getResources().getString(R.string.seleccion_recorrido));
        dialogFilePicker.setDialogSelectionListener(new DialogSelectionListener() {
            @Override
            public void onSelectedFilePaths(String[] files) {

                // Debe de haber uno y solo uno, obligatoriamente.
                Log.d("DEBUG", "Seleccionado el fichero: " + files[0]);

                // Abrimos la actividad de ANALISIS
                // Intent intent = new Intent(MainActivity.this, AnalysisActivity.class);
                // intent.putExtra("filepath", files[0]);
                // startActivity(intent);
            }
        });
        // dialogFilePicker.show();

        // TODO: Quitar bypass.
        Intent intent = new Intent(MainActivity.this, AnalysisActivity.class);
        intent.putExtra("filepath", "/mnt/sdcard/Android/data/es.um.inf.signalmap/files/AMolina_4G.json");
        startActivity(intent);
    }

    private void createInfoDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        final View view = inflater.inflate(R.layout.info_dialog, null);
        builder.setView(view);

        AlertDialog dialog = builder.create();
        dialog.show();

        Button signup = (Button) view.findViewById(R.id.btn_info_dialog);
        signup.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        // Obtenemos el texto introducido por el usuario
                        EditText et = (EditText) view.findViewById(R.id.text_info_dialog);
                        String nombreRecorrido = et.getText().toString();

                        // Obtenemos los RadioButtons
                        RadioButton radioButton4G = (RadioButton) view.findViewById(R.id.radioButton_4G_info_dialog);
                        RadioButton radioButton2G = (RadioButton) view.findViewById(R.id.radioButton_2G_info_dialog);

                        if (nombreRecorrido.equals("")) // Si no se ha introducido el nombre mostramos el error
                            Toast.makeText(getApplicationContext(), getResources().getString(R.string.error_no_name), Toast.LENGTH_SHORT).show();
                        else if (!radioButton4G.isChecked() && !radioButton2G.isChecked())// Si no se ha seleccionado una tecnologia mostramos el error
                            Toast.makeText(getApplicationContext(), getResources().getString(R.string.error_no_tecnologia), Toast.LENGTH_SHORT).show();
                        else { // En otro caso

                            // Obtenemos la tecnologia
                            String tecnologia = "";
                            if (radioButton4G.isChecked())
                                tecnologia += radioButton4G.getText().toString();
                            if (radioButton2G.isChecked())
                                tecnologia += radioButton2G.getText().toString();

                            Log.d("DEBUG", "Info Recorrido / Nombre: " + nombreRecorrido + " | Tecnologia: " + tecnologia);

                            // Map Activity con la info del user
                            Intent intent = new Intent(MainActivity.this, MapsActivity.class);
                            intent.putExtra("nombreRecorrido", nombreRecorrido);
                            intent.putExtra("tecnologia", tecnologia);
                            startActivity(intent);

                            // Cerramos el Cuadro de Dialogo
                            dialog.dismiss();
                        }
                    }
                }
        );
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        Log.d("DEBUG", "Comprobando los permisos en onRequestPermissionsResult() para el FilePicker");
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case FilePickerDialog.EXTERNAL_READ_PERMISSION_GRANT: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (dialogFilePicker != null)
                        dialogFilePicker.show();
                } else
                    Toast.makeText(MainActivity.this, getResources().getString(R.string.error_no_permisos_almacenamiento), Toast.LENGTH_SHORT).show();
            }
        }
    }
}