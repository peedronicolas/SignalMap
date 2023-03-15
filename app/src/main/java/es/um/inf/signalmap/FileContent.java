package es.um.inf.signalmap;

import com.google.gson.GsonBuilder;

class FileContent {

    // ATRIBUTOS
    private String name;
    private String date;
    private String tecnologia;
    private int numEtapas;
    private int numPuntos;
    private int numAntenas;
    private int numCambiosAntena;
    private NetworkData x_networkData[];

    // CONSTRUCTOR
    public FileContent(String name, String date, String tecnologia, int numEtapas, int numPuntos, int numAntenas, int numCambiosAntena, NetworkData networkData[]) {
        this.name = name;
        this.date = date;
        this.tecnologia = tecnologia;
        this.numEtapas = numEtapas;
        this.numPuntos = numPuntos;
        this.numAntenas = numAntenas;
        this.numCambiosAntena = numCambiosAntena;
        this.x_networkData = networkData;
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

    public int getNumCambiosAntena() {
        return numCambiosAntena;
    }

    public void setNumCambiosAntena(int numCambiosAntena) {
        this.numCambiosAntena = numCambiosAntena;
    }

    public void setNetworkData(NetworkData[] networkData) {
        this.x_networkData = networkData;
    }

    public NetworkData[] getNetworkData() {
        return x_networkData;
    }

    // OTROS
    public String toJson() {
        return new GsonBuilder().create().toJson(this);
    }
}

class NetworkData {

    // ATRIBUTOS
    private int etapa = -1;
    private String MCC = "-1";
    private String MNC = "-1";
    private int TAC = -1;
    private int CI = -1;
    private int TA = -1;
    private double averageSignal = -1;
    private double maxSignal = -999999999;
    private int numceldas = 0;
    private int id_punto = -1;

    // CONSTRUCTOR
    public NetworkData(int etapa, String MCC, String MNC, int TAC, int CI, int TA, double averageSignal, double maxSignal, int numCeldas) {
        this.etapa = etapa;
        this.MCC = MCC;
        this.MNC = MNC;
        this.TAC = TAC;
        this.CI = CI;
        this.TA = TA;
        this.averageSignal = averageSignal;
        this.maxSignal = maxSignal;
        this.numceldas = numCeldas;
    }

    public NetworkData() {
    }

    // GETTERS / SETTERS
    public int getEtapa() {
        return etapa;
    }

    public void setEtapa(int etapa) {
        this.etapa = etapa;
    }

    public String getMCC() {
        return MCC;
    }

    public void setMCC(String MCC) {
        this.MCC = MCC;
    }

    public String getMNC() {
        return MNC;
    }

    public void setMNC(String MNC) {
        this.MNC = MNC;
    }

    public int getTAC() {
        return TAC;
    }

    public void setTAC(int TAC) {
        this.TAC = TAC;
    }

    public int getCI() {
        return CI;
    }

    public void setCI(int CI) {
        this.CI = CI;
    }

    public int getTA() {
        return TA;
    }

    public void setTA(int TA) {
        this.TA = TA;
    }

    public double getAverageSignal() {
        return averageSignal;
    }

    public void setAverageSignal(double averageSignal) {
        this.averageSignal = averageSignal;
    }

    public double getMaxSignal() {
        return maxSignal;
    }

    public void setMaxSignal(double maxSignal) {
        this.maxSignal = maxSignal;
    }

    public int getNumceldas() {
        return numceldas;
    }

    public void setNumceldas(int numceldas) {
        this.numceldas = numceldas;
    }

    public void incrementarNumCeldas() {
        numceldas++;
    }

    public int getId_punto() {
        return id_punto;
    }

    public void setId_punto(int id_punto) {
        this.id_punto = id_punto;
    }

    // OTROS
    public String toJson() {
        return new GsonBuilder().create().toJson(this);
    }
}