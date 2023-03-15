package es.um.inf.signalmap;

import com.google.gson.GsonBuilder;

public class FileContent {

    // ATRIBUTOS
    private String name;
    private String date;
    private String tecnologia;
    private int numEtapas;
    private int numPuntos;
    private int numAntenas;
    private NetworkData networkData[];

    // CONSTRUCTOR
    public FileContent(String name, String date, String tecnologia, int numEtapas, int numPuntos, int numAntenas, NetworkData networkData[]) {
        this.name = name;
        this.date = date;
        this.tecnologia = tecnologia;
        this.numEtapas = numEtapas;
        this.numPuntos = numPuntos;
        this.numAntenas = numAntenas;
        this.networkData = networkData;
    }

    public FileContent() {
    }

    // GETTERS / SETTERS
    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getDate() {
        return date;
    }

    public void setTecnologia(String tecnologia) {
        this.tecnologia = tecnologia;
    }

    public String getTecnologia() {
        return tecnologia;
    }

    public void setNumEtapas(int numEtapas) {
        this.numEtapas = numEtapas;
    }

    public int getNumEtapas() {
        return numEtapas;
    }

    public void setNumPuntos(int numPuntos) {
        this.numPuntos = numPuntos;
    }

    public int getNumPuntos() {
        return numPuntos;
    }

    public void setNumAntenas(int numAntenas) {
        this.numAntenas = numAntenas;
    }

    public int getNumAntenas() {
        return numAntenas;
    }

    public void setNetworkData(NetworkData[] networkData) {
        this.networkData = networkData;
    }

    public NetworkData[] getNetworkData() {
        return networkData;
    }

    // OTROS
    public String toJson() {
        return new GsonBuilder().create().toJson(this);
    }
}
