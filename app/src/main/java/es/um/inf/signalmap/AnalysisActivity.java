package es.um.inf.signalmap;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AnalysisActivity extends AppCompatActivity {

    private String filePath;
    private String tecnologia;
    private NetworkData[] networkData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analysis);

        Log.d("DEBUG", "Hemos llegado a onCreate() en AnalysisActivity");

        // Obtenemos la ruta y el contenido del fichero
        filePath = getIntent().getStringExtra("filepath");
        getFileContent(filePath);

        // Obtenemos la tecnologia
        // TODO Obtener del fichero
        tecnologia = "4G";

        // Establecemos el nombre del recorrido en la actividad
        TextView textViewFileName = findViewById(R.id.textViewFileName);
        textViewFileName.setText(new File(filePath).getName());

        // Establecemos el contenido del layout
        mostrarAviso();
        mostrarResumen();
        mostrarGraficoBarrasSenal();
        mostrarGraficoBarrasHorizontal();
        mostrarPieChart();
        mostrarFileContent();
    }

    public void mostrarAviso() {
        TextView textViewAviso = findViewById(R.id.textViewAviso);
        textViewAviso.setText("Aviso: Recuerda que todos los valores de señal están medidos en dBm, por lo que los mejores puntos donde se obtuvo señal es en los minimos de las gráficas, no en los máximos.");
    }

    public void mostrarResumen() {
        TextView textViewResumen = findViewById(R.id.textViewResumen);
        String resumen = "";

        resumen += "Aquí un resumen de tu recorrido:\n\n";
        resumen += "- Total de puntos: " + networkData.length + "\n";
        resumen += "- Total de etapas: " + (int) Arrays.stream(networkData).mapToDouble(NetworkData::getEtapa).max().orElse(0.0) + "\n";
        resumen += "- Maxima señal detectada del recorrido: " + Arrays.stream(networkData).mapToDouble(NetworkData::getMaxSignal).max().orElse(0.0) + " dBm" + "\n";
        resumen += "- Media de los máximos detectados: " + new DecimalFormat("#.##").format(Arrays.stream(networkData).mapToDouble(NetworkData::getMaxSignal).average().orElse(0.0)) + " dBm" + "\n";
        resumen += "- Minimo de los máximos detectados: " + (int) Arrays.stream(networkData).mapToDouble(NetworkData::getMaxSignal).min().orElse(0.0) + " dBm" + "\n";
        resumen += "\n\n";
        resumen += "Distribucion de puntos por etapa:\n";

        // Para agrupar por etapas y obtener el idPunto maximo y minimo de cada una (donde empieza y donde acaba)
        Map<Integer, IntSummaryStatistics> map = Arrays.stream(networkData).collect(Collectors.groupingBy(NetworkData::getEtapa, Collectors.summarizingInt(NetworkData::getId_punto)));
        for (Map.Entry<Integer, IntSummaryStatistics> entry : map.entrySet())
            resumen += "\n- Etapa " + entry.getKey() + ": del " + entry.getValue().getMin() + " al " + entry.getValue().getMax() + ".";

        textViewResumen.setText(resumen);
    }

    public void mostrarGraficoBarrasHorizontal() {
        HorizontalBarChart horizontalBarChart = findViewById(R.id.horizontalBarChart);

        int numEtapas = (int) Arrays.stream(networkData).mapToDouble(NetworkData::getEtapa).max().orElse(0.0);

        // Personalizar eje X
        XAxis xAxis = horizontalBarChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setDrawAxisLine(true);
        xAxis.setGranularity(1f); // Establecer el espacio entre cada barra
        xAxis.setLabelCount(numEtapas); // Establecer el número de etiquetas
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return getResources().getString(R.string.etapa) + " " + (int) value;
            }
        });

        // Personalizar eje Y
        YAxis leftAxis = horizontalBarChart.getAxisLeft();
        leftAxis.setDrawAxisLine(false);
        leftAxis.setDrawGridLines(false);
        leftAxis.setDrawLabels(true);

        // Añadimos las barras con los datos
        ArrayList<BarEntry> entries = new ArrayList<>();
        for (int i = 1; i <= numEtapas; i++) {
            final int etapa = i;
            double maxEtapa = Arrays.stream(networkData).filter(ND -> ND.getEtapa() == etapa).mapToDouble(NetworkData::getMaxSignal).max().orElse(0.0);
            entries.add(new BarEntry(i, (float) maxEtapa));
        }

        // Crear conjunto de datos y personalizar colores
        BarDataSet dataSet = new BarDataSet(entries, "Signal Values");
        dataSet.setColor(Constantes.COLOR_AZUL);

        // Crear objeto BarData con el conjunto de datos y etiquetas
        BarData barData = new BarData(dataSet);

        // Personaliza y muestra el grafico
        horizontalBarChart.setData(barData);
        horizontalBarChart.getLegend().setEnabled(false); // Desactivamos la leyenda
        horizontalBarChart.setDescription(null);
        horizontalBarChart.setDrawGridBackground(false);
        horizontalBarChart.setTouchEnabled(false);
        horizontalBarChart.setDragEnabled(false);
        horizontalBarChart.setScaleEnabled(false);
        horizontalBarChart.setPinchZoom(false);
        horizontalBarChart.setDrawValueAboveBar(true);
        horizontalBarChart.invalidate(); // Actualizar grafico
    }

    public void mostrarGraficoBarrasSenal() {
        BarChart barChart = findViewById(R.id.barChartSignal);

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

        BarDataSet dataSet = new BarDataSet(entries, "Signal Values");

        // Configuración de los colores de las barras segun el valor y la tecnologia
        List<Integer> colors = new ArrayList<>();
        for (BarEntry entry : entries) {
            if (tecnologia.equals("4G")) {
                if (entry.getY() >= -80)
                    colors.add(Constantes.COLOR_VERDE); // EXCELENT -> Verde
                else if (entry.getY() < -80 && entry.getY() >= -90)
                    colors.add(Constantes.COLOR_AMARILLO); // GOOD -> Amarillo
                else if (entry.getY() < -90 && entry.getY() > -100)
                    colors.add(Constantes.COLOR_NARANJA); // FAIR -> Naranja
                else if (entry.getY() <= -100)
                    colors.add(Constantes.COLOR_ROJO); // POOR -> Rojo
            } else if (tecnologia.equals("2G")) {
                if (entry.getY() >= -70)
                    colors.add(Constantes.COLOR_VERDE); // EXCELENT -> Verde
                else if (entry.getY() < -70 && entry.getY() >= -85)
                    colors.add(Constantes.COLOR_AMARILLO); // GOOD -> Amarillo
                else if (entry.getY() < -85 && entry.getY() >= -100)
                    colors.add(Constantes.COLOR_NARANJA); // FAIR -> Naranja
                else if (entry.getY() < -100)
                    colors.add(Constantes.COLOR_ROJO); // POOR -> Rojo
            } else {
                Log.d("DEBUG", "ERROR en mostrarGraficoBarrasSenal(), tecnologia no valida.");
                return;
            }
        }
        dataSet.setColors(colors);

        // Personaliza y muestra el grafico
        BarData data = new BarData(dataSet);
        barChart.setData(data);
        barChart.getLegend().setEnabled(false); // Desactivamos la leyenda
        barChart.setVisibleXRangeMinimum(8); // Minimo de barras visibles
        barChart.setDescription(null); // Quitar la descripcion
        barChart.setScaleYEnabled(false); // Quitar zoom en el eje Y
        barChart.animateY(1000); // Agregamos animación de entrada
        barChart.invalidate(); // Actualizar gráfico
    }

    public void mostrarPieChart() {
        // Calculamos los datos de nuestro recorrido
        int numPuntosTotales = (int) Arrays.stream(networkData).count();
        int numPuntosExcelent, numPuntosGood, numPuntosFair, numPuntosPoor;
        if (tecnologia.equals("4G")) {
            numPuntosExcelent = (int) Arrays.stream(networkData).filter(ND -> ND.getMaxSignal() >= -80).count();
            numPuntosGood = (int) Arrays.stream(networkData).filter(ND -> ND.getMaxSignal() < -80 && ND.getMaxSignal() >= -90).count();
            numPuntosFair = (int) Arrays.stream(networkData).filter(ND -> ND.getMaxSignal() < -90 && ND.getMaxSignal() > -100).count();
            numPuntosPoor = (int) Arrays.stream(networkData).filter(ND -> ND.getMaxSignal() <= -100).count();
        } else if (tecnologia.equals("2G")) {
            numPuntosExcelent = (int) Arrays.stream(networkData).filter(ND -> ND.getMaxSignal() >= -70).count();
            numPuntosGood = (int) Arrays.stream(networkData).filter(ND -> ND.getMaxSignal() < -70 && ND.getMaxSignal() >= -85).count();
            numPuntosFair = (int) Arrays.stream(networkData).filter(ND -> ND.getMaxSignal() < -85 && ND.getMaxSignal() >= -100).count();
            numPuntosPoor = (int) Arrays.stream(networkData).filter(ND -> ND.getMaxSignal() < -100).count();
        } else {
            Log.d("DEBUG", "ERROR en mostrarPieChart(), tecnologia no valida.");
            return;
        }

        // Almacenar los valores (calculando el %) y etiquetas del gráfico
        ArrayList<PieEntry> pieEntries = new ArrayList<>();
        pieEntries.add(new PieEntry(((float) numPuntosExcelent / (float) numPuntosTotales) * 100, "EXCELENT"));
        pieEntries.add(new PieEntry(((float) numPuntosGood / (float) numPuntosTotales) * 100, "GOOD"));
        pieEntries.add(new PieEntry(((float) numPuntosFair / (float) numPuntosTotales) * 100, "FAIR"));
        pieEntries.add(new PieEntry(((float) numPuntosPoor / (float) numPuntosTotales) * 100, "POOR"));

        // Crear el PieDataSet con los valores y etiquetas
        PieDataSet pieDataSet = new PieDataSet(pieEntries, "");
        pieDataSet.setColors(Constantes.COLOR_VERDE, Constantes.COLOR_AMARILLO, Constantes.COLOR_NARANJA, Constantes.COLOR_ROJO);

        PieData pieData = new PieData(pieDataSet);
        pieData.setValueFormatter(new PercentFormatter()); // Formato de los valores como porcentajes

        // Configurar el objeto PieChart
        PieChart pieChart = findViewById(R.id.pieChart);
        pieChart.setData(pieData);
        pieChart.getDescription().setEnabled(false); // Desactivar la descripción
        pieChart.setCenterText("%"); // Establecer el texto central
        pieChart.setDrawEntryLabels(false); // Desactivar las etiquetas de los valores
        pieChart.setUsePercentValues(true); // Utilizar porcentajes en lugar de valores absolutos
        pieChart.animateY(1000, Easing.EaseInOutCubic); // Animación del gráfico
        pieChart.invalidate();
    }

    public void mostrarFileContent() {
        TextView textViewFileContent = findViewById(R.id.textViewFileContent);
        textViewFileContent.setText(new GsonBuilder().setPrettyPrinting().create().toJson(networkData));
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