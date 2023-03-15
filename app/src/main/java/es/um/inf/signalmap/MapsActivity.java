package es.um.inf.signalmap;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.telephony.CellInfo;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import es.um.inf.signalmap.databinding.ActivityMapsBinding;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    public static final int UNAVAILABLE = 2147483647;
    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    private String nombreRecorrido, tecnologia;
    private Button btn1, btn2, btn3, btn4;
    private FusedLocationProviderClient clientLocationProvider;
    private LocationCallback locationCallback;
    private LatLng currentLatLng = null;
    private int numEtapa = 1, numPunto = 0, numCambiosAntena = 0;
    private TelephonyManager telephonyManager;
    private LinkedList<NetworkData> networkData = new LinkedList<>();
    private LinkedList<Marker> marcadoresAntenas = new LinkedList<>();
    private boolean isRecorridoEmpezado = false;
    private NetworkData lastNetworkData = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Mantener la pantalla encendida
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Obtenemos los datos que vienen de MainActivity
        nombreRecorrido = getIntent().getStringExtra("nombreRecorrido");
        tecnologia = getIntent().getStringExtra("tecnologia");

        // Mostramos el nombre y tecnologia del recorrido
        TextView tv = (TextView) findViewById(R.id.textViewInfo);
        tv.setText(nombreRecorrido + " / " + tecnologia);

        // Inicializamos los botones de la actividad
        btn1 = (Button) findViewById(R.id.button_1);
        btn2 = (Button) findViewById(R.id.button_2);
        btn3 = (Button) findViewById(R.id.button_3);
        btn4 = (Button) findViewById(R.id.button_4);

        // Iniciamos lo relacionado con la Localizacion.
        clientLocationProvider = LocationServices.getFusedLocationProviderClient(this);
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);

                // No necesitamos comprobar si la ubicacion es NULL porque ya arriba
                // nos indica que no puede serlo.

                if (currentLatLng == null) { // Si es la primera vez que recibimos una ubicacion

                    // Inicializamos el resto de botonones
                    btn1.setEnabled(true);
                    btn4.setEnabled(true);

                    // Añadimos el marcador "Inicio de etapa 1"
                    mMap.addMarker(new MarkerOptions()
                            .position(new LatLng(locationResult.getLastLocation().getLatitude(), locationResult.getLastLocation().getLongitude()))
                            .title(getResources().getString(R.string.inicio_etapa) + " " + numEtapa + ".")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)));
                }

                // Se actualiza la variable donde almacenamos la ubicación actual
                currentLatLng = new LatLng(locationResult.getLastLocation().getLatitude(), locationResult.getLastLocation().getLongitude());

                // Agrega un nuevo marcador en el mapa
                addMarcador();
            }
        };

        // Iniciamos lo relacionado con la Telefonia.
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        Log.d("DEBUG", "Hemos llegado a onMapReady()");
        mMap = googleMap;

        // Mostramos la ubi actual del user
        startShowingLocation();

        // No activamos el btn hasta que el mapa este listo
        btn3.setEnabled(true);

        // Recordamos al usuario cambiar los ajustes del terminal en caso de que vaya a usar 2G.
        if (tecnologia.equals("2G"))
            Toast.makeText(getApplicationContext(), getResources().getString(R.string.aviso_2G), Toast.LENGTH_SHORT).show();

        // Creamos el InfoWindowAdapter para en los marcadores del mapa poder poner descripciones mas largas y multilinea,
        // para poder mostrar toda la info de cada punto y antena.
        mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {

            @Override
            public View getInfoWindow(Marker arg0) {
                return null;
            }

            @Override
            public View getInfoContents(Marker marker) {

                LinearLayout info = new LinearLayout(getApplicationContext());
                info.setOrientation(LinearLayout.VERTICAL);

                if (marker.getTitle() != null && !marker.getTitle().equals("")) {
                    TextView title = new TextView(getApplicationContext());
                    title.setTextColor(Color.BLACK);
                    title.setGravity(Gravity.CENTER);
                    title.setTypeface(null, Typeface.BOLD);
                    title.setText(marker.getTitle());
                    info.addView(title);
                }

                if (marker.getSnippet() != null && !marker.getSnippet().equals("")) {
                    TextView snippet = new TextView(getApplicationContext());
                    snippet.setTextColor(Color.GRAY);
                    snippet.setText(marker.getSnippet());
                    info.addView(snippet);
                }

                return info;
            }
        });
    }

    private void startShowingLocation() {

        Log.d("DEBUG", "Comprobando los permisos en startShowingLocation()");
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION}, 0);
            return;
        }

        Log.d("DEBUG", "Mostrando el mapa en startShowingLocation()");
        mMap.setMyLocationEnabled(true);
        mMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
    }

    public void getLocationUpdates() {

        LocationRequest locationRequest = new LocationRequest.Builder(3000).setPriority(Priority.PRIORITY_HIGH_ACCURACY).setMinUpdateIntervalMillis(2000).setMinUpdateDistanceMeters(50).build();

        Log.d("DEBUG", "Comprobando los permisos en metodoGetLocationUpdates()");
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
            return;
        }

        Log.d("DEBUG", "Activando los updates en metodoGetLocationUpdates()");
        clientLocationProvider.requestLocationUpdates(locationRequest, locationCallback, null);
    }

    public void addMarcador() {

        // Obtenemos la informacion de la red
        NetworkData ND = getNetworkInfo();

        if (ND == null) // Si por lo que sea no hay datos de telefonia salimos.
            return;

        // Establecemos el color del marcador en funcion del nivel maximo de señal y la tecnologia
        int color = -1;

        if (tecnologia.equals("4G"))
            if (ND.getMaxSignal() >= -80)
                color = Constantes.COLOR_VERDE; // EXCELENT -> Verde
            else if (ND.getMaxSignal() < -80 && ND.getMaxSignal() >= -90)
                color = Constantes.COLOR_AMARILLO; // GOOD -> Amarillo
            else if (ND.getMaxSignal() < -90 && ND.getMaxSignal() > -100)
                color = Constantes.COLOR_NARANJA; // FAIR -> Naranja
            else if (ND.getMaxSignal() <= -100)
                color = Constantes.COLOR_ROJO; // POOR -> Rojo

        if (tecnologia.equals("2G"))
            if (ND.getMaxSignal() >= -70)
                color = Constantes.COLOR_VERDE; // EXCELENT -> Verde
            else if (ND.getMaxSignal() < -70 && ND.getMaxSignal() >= -85)
                color = Constantes.COLOR_AMARILLO; // GOOD -> Amarillo
            else if (ND.getMaxSignal() < -85 && ND.getMaxSignal() >= -100)
                color = Constantes.COLOR_NARANJA;  // FAIR -> Naranja
            else if (ND.getMaxSignal() < -100)
                color = Constantes.COLOR_ROJO; // POOR -> Rojo

        // Definimos la informacion del marcador
        String title = String.valueOf(numPunto);
        String snippet = "\nmaxSignal: " + ND.getMaxSignal() +
                "\naverageSignal: " + ND.getAverageSignal() +
                "\ntimingAdvance: " + ND.getTA() +
                "\nnumCell: " + ND.getNumceldas() +
                "\netapa: " + ND.getEtapa() +
                "\n\nMCC: " + ND.getMCC() +
                "\nMNC: " + ND.getMNC() +
                "\nTAC: " + ND.getTAC() +
                "\nCI: " + ND.getCI();

        // Si tenemos un valor muy bajo de señal maxima, algo fué mal, lo mostramos
        if (ND.getMaxSignal() < -10000) {
            color = Color.parseColor("#000000"); // Negro
            title = getResources().getString(R.string.error_senal);
            snippet = "";
        }

        Log.d("DEBUG", "Añadiendo nuevo marcador al mapa en addMarcador()");

        // Definimos lo necesario para dibujar el marcador
        int size = 72, borderWidth = 7;
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // Dibujar el círculo
        Paint paint = new Paint();
        paint.setColor(color);
        canvas.drawCircle(size / 2f, size / 2f, size / 3f, paint);

        // Dibujar el borde alrededor del círculo si se ha producido un cambio de antena
        if (lastNetworkData != null && (!lastNetworkData.getMCC().equals(ND.getMCC()) || !lastNetworkData.getMNC().equals(ND.getMNC()) || lastNetworkData.getCI() != ND.getCI() || lastNetworkData.getTAC() != ND.getTAC())) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(borderWidth);
            paint.setColor(Color.BLACK);
            canvas.drawCircle(size / 2f, size / 2f, size / 3f - borderWidth / 2f, paint);

            numCambiosAntena++; // Contamos el cambio de antena
        }

        // Añadimos el nuevo marcador
        mMap.addMarker(new MarkerOptions()
                .position(currentLatLng)
                .title(title)
                .snippet(snippet)
                .icon(BitmapDescriptorFactory.fromBitmap(bitmap))
                .anchor(0.5f, 1f));

        lastNetworkData = ND; // Actualizamos la ultima informacion de telefonia recogida
        numPunto++; // Incrementamos el contador de marcas para ir sabiendo el id de cada toma.
    }

    public NetworkData getNetworkInfo() {

        Log.d("DEBUG", "Comprobando los permisos en getNetworkInfo()");
        if ((ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) || ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.READ_PHONE_STATE, Manifest.permission.ACCESS_FINE_LOCATION}, 2);
            return null;
        }

        double dBmTotales = 0; // Para sacar la media
        NetworkData ND = new NetworkData();
        ND.setEtapa(numEtapa);
        ND.setId_punto(numPunto);

        Log.d("DEBUG", "Obteniendo la infomacion de telefonia");
        List<CellInfo> cellInfo = telephonyManager.getAllCellInfo();
        for (CellInfo cell : cellInfo) { // Recorremos todas las celdas detectadas

            // PARA 4G
            if (cell instanceof CellInfoLte && tecnologia.equals("4G")) {
                CellInfoLte cellinfoLTE = (CellInfoLte) cell;

                // Obtengo la info de la celda LTE
                String MCC = cellinfoLTE.getCellIdentity().getMccString();
                String MNC = cellinfoLTE.getCellIdentity().getMncString();
                int TAC = cellinfoLTE.getCellIdentity().getTac();
                int CI = cellinfoLTE.getCellIdentity().getCi();
                int TA = cellinfoLTE.getCellSignalStrength().getTimingAdvance();
                double dBm = cellinfoLTE.getCellSignalStrength().getDbm();

                // Si la celda es no NULL actualizo valores del ND
                if (MCC != null && MNC != null && TAC != UNAVAILABLE && CI != UNAVAILABLE) {
                    ND.setMCC(MCC);
                    ND.setMNC(MNC);
                    ND.setTAC(TAC);
                    ND.setCI(CI);

                    // Si el Timing Advance esta disponible para esta celda lo actualizo
                    if (TA != UNAVAILABLE)
                        ND.setTA(TA);
                }

                // Actualizo la señal maxima si aplica
                if (ND.getMaxSignal() < dBm)
                    ND.setMaxSignal(dBm);

                // Tengo en cuenta que hay una celda mas
                ND.incrementarNumCeldas();

                // Para el calculo de la señal media
                dBmTotales += dBm;
                ND.setAverageSignal(Math.round((dBmTotales / (double) ND.getNumceldas()) * 100d) / 100d);
            }

            // PARA 2G
            if (cell instanceof CellInfoGsm && tecnologia.equals("2G")) {
                CellInfoGsm cellinfoGSM = (CellInfoGsm) cell;

                // Obtengo la info de la celda GSM
                String MCC = cellinfoGSM.getCellIdentity().getMccString();
                String MNC = cellinfoGSM.getCellIdentity().getMncString();
                int TAC = cellinfoGSM.getCellIdentity().getLac();
                int CI = cellinfoGSM.getCellIdentity().getCid();
                int TA = cellinfoGSM.getCellSignalStrength().getTimingAdvance();
                double dBm = cellinfoGSM.getCellSignalStrength().getDbm();

                // Si la celda no es NULL actualizo valores del ND
                if (MCC != null && MNC != null && TAC != UNAVAILABLE && CI != UNAVAILABLE) {
                    ND.setMCC(MCC);
                    ND.setMNC(MNC);
                    ND.setTAC(TAC);
                    ND.setCI(CI);

                    // Si el Timing Advance esta disponible para esta celda lo actualizo
                    if (TA != UNAVAILABLE)
                        ND.setTA(TA);
                }

                // Actualizo la señal maxima si aplica
                if (ND.getMaxSignal() < dBm)
                    ND.setMaxSignal(dBm);

                // Tengo en cuenta que hay una celda mas
                ND.incrementarNumCeldas();

                // Para el calculo de la señal media
                dBmTotales += dBm;
                ND.setAverageSignal(Math.round((dBmTotales / (double) ND.getNumceldas()) * 100d) / 100d);
            }
        }

        // Dibujamos la antena reconocida en este punto
        addAntena(ND.getMCC(), ND.getMNC(), ND.getTAC(), ND.getCI());

        // Añadimos este punto de informacion a la lista y lo delvolvemos
        Log.d("DEBUG", "Informacion de telefonia getNetworkInfo(): " + ND.toJson());
        networkData.add(ND);
        return ND;
    }

    public void addAntena(String MCC, String MNC, int TAC, int CI) {
        // Hacemos la peticion HTTP al servicio
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = "https://api.mylnikov.org/geolocation/cell?v=1.1&data=open&mcc=" + MCC + "&mnc=" + MNC + "&lac=" + TAC + "&cellid=" + CI;
        StringRequest request = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {

                            Log.d("DEBUG", "HTTP response en addAntena(): " + response);

                            JSONObject responseJSON = new JSONObject(response); // Para transformar a JSON y obtener los distintos campos
                            if (responseJSON.getInt("result") == 200) { // Si el resultado de la peticion es EXITOSO

                                // Posicion (LatLng) de la antena actual
                                LatLng latLng = new LatLng(responseJSON.getJSONObject("data").getDouble("lat"), responseJSON.getJSONObject("data").getDouble("lon"));

                                // Recorremos la lista de antenas ya dibujadas, si la que hemos detectado ya ha sido registrada le asignamos el icono de la actual, si no, el de la no actual
                                boolean existeAntena = false;
                                for (Marker marker : marcadoresAntenas)
                                    if (marker.getPosition().equals(latLng)) {
                                        marker.setIcon(BitmapDescriptorFactory.fromBitmap(Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.antena1), 100, 100, false)));
                                        existeAntena = true;
                                    } else
                                        marker.setIcon(BitmapDescriptorFactory.fromBitmap(Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.antena2), 100, 100, false)));

                                // Si esta antena es la primera vez que se detecta se crea el marcador, se dibuja, y se almacena en la lista
                                if (!existeAntena) {

                                    // Obtenemos el nombre del municipio donde esta la antena para indicarlo, # en caso de no poder obtenerlo
                                    String locationName = "#";
                                    List<Address> addresses = new Geocoder(getApplicationContext(), Locale.getDefault()).getFromLocation(responseJSON.getJSONObject("data").getDouble("lat"), responseJSON.getJSONObject("data").getDouble("lon"), 1);
                                    if (addresses != null && addresses.size() > 0 && !addresses.get(0).getLocality().equals(""))
                                        locationName = addresses.get(0).getLocality();

                                    marcadoresAntenas.add(mMap.addMarker(new MarkerOptions()
                                            .position(latLng)
                                            .icon(BitmapDescriptorFactory.fromBitmap(Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.antena1), 100, 100, false)))
                                            .title(locationName)
                                            .snippet("MCC: " + MCC + "\nMNC: " + MNC + "\nTAC: " + TAC + "\nCI: " + CI)));
                                }
                            }
                        } catch (Exception e) {
                            Log.d("DEBUG", "Error al decodificar el JSON o al usar Geocoder en addAntena()");
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) { // Manejar los errores de la solicitud HTTP, no hacemos nada si hay un error en la conexion.
                        Log.d("DEBUG", "Error al obtener HTTP response en addAntena()");
                    }
                });
        queue.add(request);
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        Log.d("DEBUG", "Comprobando los permisos en onRequestPermissionsResult()");
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 0: // Permisos de startShowingLocation()
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    // Contianuamos con la funcionalidad que dependia de este permiso
                    startShowingLocation();
                else {
                    // No nos concede el permiso habria que desactivar la funcionalidad que depende de el
                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.error_no_permisos), Toast.LENGTH_SHORT).show();
                    finish();
                }
                return;
            case 1: // Permisos de getLocationUpdates()
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    // Contianuamos con la funcionalidad que dependia de este permiso
                    getLocationUpdates();
                else {
                    // No nos concede el permiso habria que desactivar la funcionalidad que depende de el
                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.error_no_permisos), Toast.LENGTH_SHORT).show();
                    finish();
                }
                return;
            case 2: // Permisos de getNetworkInfo()
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    // Contianuamos con la funcionalidad que dependia de este permiso
                    getNetworkInfo();
                else {
                    // No nos concede el permiso habria que desactivar la funcionalidad que depende de el
                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.error_no_permisos), Toast.LENGTH_SHORT).show();
                    finish();
                }
                return;
            default:
        }
    }

    public void onClickBtn1(View v) {
        // Incrementamos el numero de la etapa, y mostramos el marcador en el mapa
        numEtapa++;
        mMap.addMarker(new MarkerOptions()
                .position(currentLatLng)
                .title(getResources().getString(R.string.fin_etapa) + " " + (numEtapa - 1) + ".")
                .snippet(getResources().getString(R.string.inicio_etapa) + " " + numEtapa + ".")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)));
    }

    public void onClickBtn2(View v) {
        String filename = nombreRecorrido + "_" + tecnologia + ".json";
        new AlertDialog.Builder(this)
                .setMessage(getResources().getString(R.string.confirmacion_guardar) + " '" + filename + "'.")
                .setPositiveButton(getResources().getString(R.string.si), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // Si el usuario confirma la salida, guardamos y cerramos la actividad
                        saveInfo(filename);
                        finish();
                    }
                })
                .setNegativeButton(getResources().getString(R.string.no), null)
                .show();
    }

    public void onClickBtn3(View v) {
        getLocationUpdates();
        btn3.setEnabled(false);
        isRecorridoEmpezado = true;
    }

    public void onClickBtn4(View v) {
        // Cambiamos el comportamiento de los botones
        btn1.setEnabled(false);
        btn2.setEnabled(true);
        btn4.setEnabled(false);

        // Añadimos el marcador de fin de ultima etapa
        mMap.addMarker(new MarkerOptions()
                .position(currentLatLng)
                .title(getResources().getString(R.string.fin_etapa) + " " + numEtapa + ".")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)));

        // Dejamos de recibir actualizaciones de ubicacion.
        clientLocationProvider.removeLocationUpdates(locationCallback);
        isRecorridoEmpezado = false;
    }

    public void onClickBtn5(View v) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.popup_leyenda, null);

        // Configurar el título, la descripción y la imagen segun la tecnologia
        TextView tvTitulo = view.findViewById(R.id.tv_titulo);
        TextView tvDescripcion = view.findViewById(R.id.tv_descripcion);
        ImageView img = view.findViewById(R.id.rangeImage);

        if (tecnologia.equals("4G")) {
            tvTitulo.setText("4G (LTE)");
            tvDescripcion.setText(getResources().getString(R.string.descripcion_4G));
            img.setImageResource(R.drawable.range4g);
        }

        if (tecnologia.equals("2G")) {
            tvTitulo.setText("2G (GSM)");
            tvDescripcion.setText(getResources().getString(R.string.descripcion_2G));
            img.setImageResource(R.drawable.range2g);
        }

        builder.setView(view);
        builder.setPositiveButton(getResources().getString(R.string.cerrar), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public void saveInfo(String filename) {
        try {
            String date = new SimpleDateFormat("dd/MM/yyyy").format(new Date());
            FileContent FC = new FileContent(filename, date, tecnologia, numEtapa, networkData.size(), marcadoresAntenas.size(), numCambiosAntena, networkData.toArray(new NetworkData[networkData.size()]));

            StorageHelper.saveStringToFile(filename, new GsonBuilder().setPrettyPrinting().create().toJson(FC), this);

            Log.d("DEBUG", "Guardada la info del recorrido en almacenamiento.");
            Toast.makeText(getApplicationContext(), getResources().getString(R.string.confirmacion_almacenamiento), Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Log.e("DEBUG", "[saveInfo()] Error saving file: ", e);
        }
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setMessage(getResources().getString(R.string.confirmacion_retroceso))
                .setPositiveButton(getResources().getString(R.string.si), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // Si el usuario confirma la salida, llamamos a la superclase para cerrar la actividad
                        MapsActivity.super.onBackPressed();
                    }
                })
                .setNegativeButton(getResources().getString(R.string.no), null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Si llegamos a onResume() y el recorrido ya estaba empezado, es decir, el usuario pauso la
        // app y volvio a entrar, entonces volvemos a activar las actualizaciones de ubicacion
        if (isRecorridoEmpezado)
            getLocationUpdates();
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Si la aplicacion se pausa paramos de recibir las actualizaciones de ubicacion.
        clientLocationProvider.removeLocationUpdates(locationCallback);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Quitar la bandera de mantener la pantalla encendida para restaurar el comportamiento normal de la pantalla.
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
}