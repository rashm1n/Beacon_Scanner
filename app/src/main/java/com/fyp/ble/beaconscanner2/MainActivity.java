package com.fyp.ble.beaconscanner2;

import java.text.SimpleDateFormat;
import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.opencsv.CSVWriter;

import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class MainActivity extends AppCompatActivity {

    private Handler hand;
    private HashMap<String, BLTE_Device> mBTDevicesHashMap;
    private ArrayList<BLTE_Device> mBTDevicesArrayList;
    ListAdapter_BTLE_Devices adapter;
    private Scanner_BLTE mBTLeScanner;

    Button button;
    Button saveButton;
    Button increaseDistanceButton;
    Button decreaseDistanceButton;
    
    private boolean isMedian = false;
    private boolean isMean = false;
    private boolean isRaw = false;
    float finalValue;

    float raw_value;
    float mean_value;
    float median_value;

    //Kalman Filter
    public KalmanFilter kalmanFilter;


    public boolean isScanning = false;
    public boolean enableSave = false;

    Queue<Integer> queue = new LinkedList<>();


    private ArrayList<String> itrList = new ArrayList<>();
    private ArrayList<String> macList = new ArrayList<>();
    private ArrayList<String> allValues = new ArrayList<>();

//    final String MAC_ADDRESS ="E8:4E:53:B5:30:EB";
//    final String MAC_ADDRESS = "E7:2B:EA:2F:95:C5";

    EditText macaddrees;

    SimpleDateFormat formatter = new SimpleDateFormat("H:mm:ss");

    EditText distance;
    EditText windowSize;
    TextView t;
    TextView preview;
    TextView stdText;

    StandardDeviation standardDeviation;
    double stdValue;


    public static int count = 0;
    public String data;

    public List<String[]> stringlist;
    public List<String[]> finalValuelist;

    private FileWriter mFileWriter;

    private static final int PERMISSION_REQUEST_CODE = 200;

    public static String selected_MAC = " ";
    private String m_Text = "";

    // graph configugraphViewration
    GraphView graph1;
    private LineGraphSeries<DataPoint> mSeries1;
    public int graphCount = 0;

    GraphView graph2;
    private LineGraphSeries<DataPoint> mSeries2;
//    public int graphCount = 0;

    GraphView graph3;
    private LineGraphSeries<DataPoint> mSeries3;
//    public int graphCount = 0;

    GraphView graph4;
    private LineGraphSeries<DataPoint> mSeries4;
//    public int graphCount = 0;



    public void onRadioButtonClicked(View view) {
        // Is the button now checked?
        boolean checked = ((RadioButton) view).isChecked();

        // Check which radio button was clicked
        switch(view.getId()) {
            case R.id.radio_raw:
                if (checked)
                    isMedian = false;
                    isMean = false;
                    isRaw = true;
                    break;
            case R.id.radio_mean:
                if (checked)
                    isMedian = false;
                    isMean = true;
                    isRaw = false;
                    break;
            case R.id.radio_median:
                if (checked)
                    isMedian = true;
                    isMean = false;
                    isRaw = false;
                    break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        macaddrees = (EditText)findViewById(R.id.mac_addr);

//        stdText = (TextView)findViewById(R.id.textStdDiv);

        kalmanFilter = new KalmanFilter(0.008,1);

        standardDeviation = new StandardDeviation();
        windowSize = (EditText)findViewById(R.id.window);

//        System.out.println(date);

        //Graph Configuration

        graph1 = (GraphView)findViewById(R.id.graph1);
        mSeries1 = new LineGraphSeries<>();
        mSeries1.setTitle("RealTime RSSI Values");
        graph1.addSeries(mSeries1);

        graph1.getViewport().setScrollable(true);
        graph1.getViewport().setXAxisBoundsManual(true);
        graph1.getViewport().setMinX(0);
        graph1.getViewport().setMaxX(100);


        graph2 = (GraphView)findViewById(R.id.graph2);
        mSeries2 = new LineGraphSeries<>();
        mSeries2.setTitle("RealTime RSSI Values");
        graph2.addSeries(mSeries2);

        graph2.getViewport().setScrollable(true);
        graph2.getViewport().setXAxisBoundsManual(true);
        graph2.getViewport().setMinX(0);
        graph2.getViewport().setMaxX(100);


        // activate horizontal zooming and scrolling
        // graph.getViewport().setScalable(true);

        // activate horizontal scrolling


        // activate horizontal and vertical zooming and scrolling
        // graph.getViewport().setScalableY(true);

        // activate vertical scrolling
        // graph.getViewport().setScrollableY(true);



//        preview =(TextView)findViewById(R.id.preview);

        stringlist = new ArrayList();
        finalValuelist = new ArrayList<>();
//        movingAverage = new ArrayList();

//        t = (TextView)findViewById(R.id.queue_display);

        String[] a = new String[2];
        a[0] = "raw_distance";
        a[1] = "raw_rssi";

        String[] writeElement = new String[7];
        writeElement[0] = "distance";
        writeElement[1] = "raw_rssi";
        writeElement[2] = "mean_rssi";
        writeElement[3] = "median_rssi";
        writeElement[4] = "standard_deviation";
        writeElement[5] = "kalman filtered";
        writeElement[6] = "time";

        //Configure the Queue For moving Average

        //Create Header of the File
        stringlist.add(a);
        finalValuelist.add(writeElement);


        //Permission check
        if (!checkPermission()) {
            openActivity();
        } else {
            if (checkPermission()) {
                requestPermissionAndContinue();
            } else {
                openActivity();
            }
        }

        distance = (EditText)findViewById(R.id.distance);

        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED){
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)){
                Toast.makeText(this, "The permission to get BLE location data is required", Toast.LENGTH_SHORT).show();
            }else{
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
        }else{
            Toast.makeText(this, "Location permissions already granted", Toast.LENGTH_SHORT).show();
        }

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            System.out.println("BLE NOT SUPPORTED");
            finish();
        }

        hand = new Handler();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            mBTLeScanner = new Scanner_BLTE(this, 180000, -100);
        }

        mBTDevicesHashMap = new HashMap<>();
        mBTDevicesArrayList = new ArrayList<>();
        adapter = new ListAdapter_BTLE_Devices(MainActivity.this, R.layout.btle_device_list_item, mBTDevicesArrayList);

        button = (Button)findViewById(R.id.start_button);
        saveButton = (Button)findViewById(R.id.save_button);
        increaseDistanceButton = (Button)findViewById(R.id.increaseButton);
        decreaseDistanceButton = (Button)findViewById(R.id.decreaseButton);

        increaseDistanceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int p = Integer.parseInt(distance.getText().toString());
                p++;
                distance.setText(Integer.toString(p));
            }
        });


        decreaseDistanceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                double p = Double.parseDouble(distance.getText().toString());

                if (p<=2.0 && p!=1){
                    p = p-0.5;
                }else{
                    p = p- 1.0;
                }

                distance.setText(Double.toString(p));
            }
        });


        saveButton.setEnabled(enableSave);

        if (!isScanning){
            button.setText("Start");
        }

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mBTLeScanner.isScanning()) {
                    selected_MAC = macaddrees.getText().toString();
                    macList.clear();
                    itrList.clear();
                    allValues.clear();
                    startScan();
                    isScanning = true;
                    button.setText("Stop");
                }
                else {
                    stopScan();
                    isScanning = false;
                    button.setText("Start");
                    enableSave = true;
                    saveButton.setEnabled(enableSave);
                }
            }
        });

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveFile();
            }
        });
    }


    public synchronized void stopScan() {
        mBTLeScanner.stop();
//        finalValuelist
    }

    public synchronized void saveFile(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Input File Name");
        final EditText input = new EditText(this);

        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                m_Text = input.getText().toString();

                try {
//            writeCSV(stringlist);
                    writeCSV(finalValuelist);


//            DataPoint[] dataPoints = new DataPoint[finalValuelist.size()-1];


//            for (int p =1;p<finalValuelist.size();p++){
//                System.out.println(p);
//                DataPoint d = new DataPoint((double)p,Double.valueOf(finalValuelist.get(p)[1]));
//                dataPoints[p-1] = d;
//            }

//            System.out.println(dataPoints.length);
//            for (DataPoint dataPoint:dataPoints){
//                System.out.println(Double.toString(dataPoint.getX()));
//                System.out.println(Double.toString(dataPoint.getY()));
//            }

//            LineGraphSeries<DataPoint> series = new LineGraphSeries<>(dataPoints);
//            graph.addSeries(series);

                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    public synchronized void addDevice(BluetoothDevice device, int rssi) {
        double r;

        String address = device.getAddress();

        if (!mBTDevicesHashMap.containsKey(address)) {
            graphCount++;
            BLTE_Device btleDevice = new BLTE_Device(device);
            btleDevice.setRSSI(rssi);
            mBTDevicesHashMap.put(address, btleDevice);
            mBTDevicesArrayList.add(btleDevice);

//            data = editString(data,"23",Integer.toString(rssi));

            queue.poll();

            queue.add(rssi);

            double convertedValue = kalmanFilter.filter(rssi);


//            stdText.setText(Double.toString(stdValue));

//            float avg = 0;

//            for (int i:queue){
//                avg = avg + i;
//            }
//
//            avg = avg /queue.size();
//
//

//            String s = "";
//
//            for (int i:queue){
//                s = s+Integer.toString(i);

//            }
//
//            t.setText(s);

//            r = Math.pow(10.0,(-76.57-avg)/20);
//            movingAverage.add((float)r);

            if (isRaw & !isMean & !isMedian){
                finalValue = (float) rssi;
            }else if (!isRaw & isMean & !isMedian){
                finalValue = applyMeanFilter(queue);
            }else if (!isRaw & !isMean & isMedian){
                finalValue = applyMedianFilter(queue);
            }

            raw_value = (float)rssi;
            mean_value = applyMeanFilter(queue);
            median_value = applyMedianFilter(queue);
            stdValue = getStandardDeviation(queue,Integer.parseInt(windowSize.getText().toString()));
            Date date = new Date();





            if (graphCount>Integer.parseInt(windowSize.getText().toString())){
                mSeries1.appendData(new DataPoint(graphCount,raw_value), true, 1000);
                mSeries2.appendData(new DataPoint(graphCount,pathLossEquation(convertedValue)), true, 1000);
            }

            if (address.equals(macaddrees.getText().toString())) {
                String[] a = new String[7];
                a[0] = distance.getText().toString();
                a[1] = Float.toString(raw_value);
                a[2] = Float.toString(mean_value);
                a[3] = Float.toString(median_value);
                a[4] = Float.toString((float) stdValue);
                a[5] = Double.toString(convertedValue);
                a[6] = formatter.format(date).toString();
//              a[1] = Integer.toString(rssi);
//                a[1] = Float.toString(finalValue);
//                preview.setText(a[1]);
//                stringlist.add(a);
                finalValuelist.add(a);
            }
//            count++;

        }
        else {

            graphCount++;
            adapter.notifyDataSetChanged();
            mBTDevicesHashMap.get(address).setRSSI(rssi);

            queue.poll();
            queue.add(rssi);
            double convertedValue = kalmanFilter.filter(rssi);
            //
//            float avg = 0;
//
//            for (int i:queue){
//                avg = avg + i;
//            }
//
//            avg = avg /queue.size();
//            System.out.println("");
//            System.out.println(avg);
//            System.out.println("");
//            for (int i:queue){
//                System.out.println(i);
//            }
//
//            String s = "";
//
//            for (int i:queue){
//                s = s+Integer.toString(i);
//            }
//
//            t.setText(s);
//
//            r = Math.pow(10.0,(-76.57-avg)/20);
//            movingAverage.add((float)r);

//            stdValue = getStandardDeviation(queue,Integer.parseInt(windowSize.getText().toString()));
//            stdText.setText(Double.toString(stdValue));

            if (isRaw & !isMean & !isMedian){
                finalValue = (float) rssi;
            }else if (!isRaw & isMean & !isMedian){
                finalValue = applyMeanFilter(queue);
            }else if (!isRaw & !isMean & isMedian){
                finalValue = applyMedianFilter(queue);
            }

            raw_value = (float)rssi;
            mean_value = applyMeanFilter(queue);
            median_value = applyMedianFilter(queue);
            Date date = new Date();
            stdValue = getStandardDeviation(queue,Integer.parseInt(windowSize.getText().toString()));


            if (graphCount>Integer.parseInt(windowSize.getText().toString())){
                mSeries1.appendData(new DataPoint(graphCount,raw_value), true, 1000);
                mSeries2.appendData(new DataPoint(graphCount,pathLossEquation(convertedValue)), true, 1000);
            }



            if (address.equals(macaddrees.getText().toString()))
            {
                String[] a = new String[7];
                a[0] = distance.getText().toString();
                a[1] = Float.toString(raw_value);
                a[2] = Float.toString(mean_value);
                a[3] = Float.toString(median_value);
                a[4] = Float.toString((float) stdValue);
                a[5] = Double.toString(convertedValue);
                a[6] = formatter.format(date).toString();
//              a[1] = Integer.toString(rssi);
//                a[1] = Float.toString(finalValue);
//                preview.setText(a[1]);
//                stringlist.add(a);
                finalValuelist.add(a);
            }

        }
        adapter.notifyDataSetChanged();
    }

    public void startScan(){
        for (int i=0;i<Integer.parseInt(windowSize.getText().toString());i++){
            queue.add(0);
        }
        mBTLeScanner.start();
    }

    public void writeCSV(List<String[]> a) throws IOException {

        String topic = "";

        if (isRaw & !isMean & !isMedian){
            topic = "Raw";
        }else if (!isRaw & isMean & !isMedian){
            topic = "Mean";
        }else if (!isRaw & !isMean & isMedian){
            topic = "Median";
        }


        Random r = new Random();
        String baseDir = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
        Log.d("Write",baseDir);
        String fileName = m_Text+topic+".csv";
        String filePath = baseDir + File.separator + fileName;
        File f = new File(filePath);
        CSVWriter writer;
        // File exist
        if(f.exists()&&!f.isDirectory())
        {
            mFileWriter = new FileWriter(filePath, true);
            writer = new CSVWriter(mFileWriter);
        }
        else
        {
            writer = new CSVWriter(new FileWriter(filePath));
        }

        for (String[] s:a){
            writer.writeNext(s);
        }

        writer.close();
        Log.d("Write","written");
        System.out.println("writeeeeeeeeeeeeeeeeee");
        Toast.makeText(this, (String)("File Saved.  "+fileName), Toast.LENGTH_LONG).show();
    }


    private boolean checkPermission() {

        return ContextCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                ;
    }

    private void openActivity() {

        //add your further process after giving permission or to download images from remote server.
    }

    private void requestPermissionAndContinue() {
        if (ContextCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this, WRITE_EXTERNAL_STORAGE)
                    && ActivityCompat.shouldShowRequestPermissionRationale(this, READ_EXTERNAL_STORAGE)) {
                AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
                alertBuilder.setCancelable(true);
                alertBuilder.setTitle("permission_necessary");
                alertBuilder.setMessage("storage_permission_is_encessary_to_wrote_event");
                alertBuilder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
                    public void onClick(DialogInterface dialog, int which) {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{WRITE_EXTERNAL_STORAGE
                                , READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
                    }
                });
                AlertDialog alert = alertBuilder.create();
                alert.show();
                Log.e("", "permission denied, show dialog");
            } else {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{WRITE_EXTERNAL_STORAGE,
                        READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
            }
        } else {
            openActivity();
        }
    }

    private float applyMeanFilter(Queue<Integer> queue){
            float avg = 0;

            for (int i:queue){
                avg = avg + i;
            }

            avg = avg /queue.size();

//            r = Math.pow(10.0,(-76.57-avg)/20);
//            movingAverage.add((float)r);

            return avg;
    }

    private float pathLossEquation(double val){
        double result = Math.pow(10,(-73.68-val)/20);
        return (float)result;
    }

    private float applyMedianFilter(Queue<Integer> queue){
        List<Integer> list = new ArrayList<>();

        int[] numArray = new int[queue.size()];

        int count = 0;
        for (Integer i:queue){
            numArray[count] = i;
            count++;
        }

        Arrays.sort(numArray);

        double median;

        if (numArray.length % 2 == 0)
            median = ((double)numArray[numArray.length/2] + (double)numArray[numArray.length/2 - 1])/2;
        else
            median = (double) numArray[numArray.length/2];

        return (float)median;
    }

    private double getStandardDeviation(Queue<Integer> numQueue,int windowLength){
//      Integer.parseInt(windowSize.getText().toString())
        double[] stdArray = new double[windowLength];

        int k = 0;
        for (int i:numQueue){
            double val = (double)i;
            stdArray[k] = val;
            k++;
        }

        double std = standardDeviation.evaluate(stdArray);

        return std;
    }
}
