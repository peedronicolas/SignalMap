package es.um.inf.signalmap;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AnalysisActivity extends AppCompatActivity {

    private String filePath;
    private NetworkData[] networkData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analysis);

        Log.d("DEBUG", "Hemos llegado a onCreate() en AnalysisActivity");

        // Obtenemos la ruta y el contenido del fichero
        filePath = getIntent().getStringExtra("filepath");
        getFileContent(filePath);

        // Establecemos el nombre del recorrido en la actividad
        TextView textViewFileName = findViewById(R.id.textViewFileName);
        textViewFileName.setText(new File(filePath).getName());

        mostrarGraficoBarrasSenal();

        // Establecemos el contenido del fichero
        TextView textViewFileContent = findViewById(R.id.textViewFileContent);
        textViewFileContent.setText(new GsonBuilder().setPrettyPrinting().create().toJson(networkData));
    }

    public void mostrarGraficoBarrasSenal() {
        BarChart barChart = findViewById(R.id.chart1);

        // Configuración del eje x
        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f); // Distancia entre cada entrada de datos

        // Configuración del eje y
        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setInverted(true); // Invertir eje y
        leftAxis.setDrawGridLines(true);
        leftAxis.setDrawLabels(true);
        YAxis rightAxis = barChart.getAxisRight();
        rightAxis.setEnabled(false); // Quitamos el eje Y del lado izquierdo

        // Añadimos las barras con los datos
        List<BarEntry> entries = new ArrayList<>();
        for (int i = 0; i < networkData.length; i++)
            entries.add(new BarEntry(i, (float) networkData[i].getMaxSignal()));

        BarDataSet dataSet = new BarDataSet(entries, "Valores");

        // Configuración de los colores de las barras segun el valor
        List<Integer> colors = new ArrayList<>();
        for (BarEntry entry : entries) {
            if (entry.getY() >= -70)
                colors.add(Color.parseColor("#43AC3B")); // EXCELENT -> Verde
            else if (entry.getY() < -70 && entry.getY() >= -85)
                colors.add(Color.parseColor("#fef500")); // GOOD -> Amarillo
            else if (entry.getY() < -85 && entry.getY() >= -100)
                colors.add(Color.parseColor("#ff9500")); // FAIR -> Naranja
            else if (entry.getY() < -100)
                colors.add(Color.parseColor("#ff2b00")); // POOR -> Rojo
        }
        dataSet.setColors(colors);

        BarData data = new BarData(dataSet);
        barChart.setData(data);
        barChart.getLegend().setEnabled(false); // Desactivamos la leyenda
        barChart.setVisibleXRangeMinimum(8); // Minimo de barras visibles
        barChart.setDescription(null); // Quitar la descripcion
        barChart.setScaleYEnabled(false); // Quitar zoom en el eje Y
        barChart.animateY(1000); // Agregamos animación de entrada
        barChart.invalidate(); // Actualizar gráfico
    }

    public void getFileContent(String filename) {
        try {
            networkData = new Gson().fromJson(StorageHelper.readStringFromFile(filename, this), NetworkData[].class);
        } catch (Exception e) {
            Log.e("DEBUG", "[getFileContent()] Error leyendo o deserializando el fichero");
            Toast.makeText(getApplicationContext(), getResources().getString(R.string.error_fichero), Toast.LENGTH_SHORT).show();
            finish();
        }
    }
}