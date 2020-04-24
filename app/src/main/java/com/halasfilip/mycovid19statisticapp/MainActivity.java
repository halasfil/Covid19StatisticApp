package com.halasfilip.mycovid19statisticapp;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.graphics.Color;
import android.icu.text.DecimalFormat;
import android.os.Build;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IValueFormatter;
import com.github.mikephil.charting.utils.ViewPortHandler;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.*;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private ImageButton getBtn;
    private TextView casesTxt;
    private TextView deathsTxt;
    private TextView recoveredTxt;
    private TextView regionName;
    String selectedRegionName;
    private TextView newCases;
    private TextView activeCases;
    private PieChart pieChart;
    private ImageButton regionButton;
    private int deaths;
    private int recovered;
    private int activeSick;

    @Override
    protected void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        loadRegionInfo();

        casesTxt = findViewById(R.id.result1);
        newCases = findViewById(R.id.newCases);
        deathsTxt = findViewById(R.id.result2);
        recoveredTxt = findViewById(R.id.result3);
        regionName = findViewById(R.id.regionName);
        getBtn = findViewById(R.id.getBtn);
        activeCases = findViewById(R.id.activeCases);
        regionButton = findViewById(R.id.regionButton);
        pieChart = findViewById(R.id.pieChart);
        pieChart.setVisibility(View.INVISIBLE);


        regionName.setText(selectedRegionName);
        getDataForCountry(selectedRegionName);

        getBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getDataForCountry(regionName.getText().toString());
            }
        });

        regionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder regions = new AlertDialog.Builder(MainActivity.this);
                View mView = getLayoutInflater().inflate(R.layout.alert_spinner, null);
                regions.setTitle("Select region");
                //after creating mView we have acces to different View layout where we look for elements
                final Spinner mSpinner = mView.findViewById(R.id.regionSpinner);
                //populating selection options in spinner
                ArrayAdapter<String> regionsAdapter = new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_spinner_item, getResources().getStringArray(R.array.regionList));
                regionsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                mSpinner.setAdapter(regionsAdapter);
                regions.setPositiveButton("Apply", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (!mSpinner.getSelectedItem().toString().equals("Choose region…")) {
                            selectedRegionName = mSpinner.getSelectedItem().toString();
                            regionName.setText(selectedRegionName);
                            saveRegionInfo();
                            getDataForCountry(selectedRegionName);
                        }
                    }
                });
                regions.setNegativeButton("Cancel", null);
                regions.setView(mView);
                AlertDialog dialog = regions.create();
                dialog.show();


            }
        });

    }


    private void saveRegionInfo() {
        try {
            File file = new File(this.getFilesDir(), "file");
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(fileOutputStream));
            bufferedWriter.write(selectedRegionName);
            bufferedWriter.close();
            fileOutputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private String loadRegionInfo() {
        File file = new File(this.getFilesDir(), "file");
        if (!file.exists()) {
            return selectedRegionName = "World";
        }

        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fileInputStream));
            return selectedRegionName = bufferedReader.readLine();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;

    }

    private void addDataToPieChart(int deaths, int recovered, int active) {

        ArrayList<PieEntry> numbers = new ArrayList<>();
        ArrayList<String> tags = new ArrayList<>();
        numbers.add(new PieEntry(deaths));
        numbers.add(new PieEntry(recovered));
        numbers.add(new PieEntry(active));
        tags.add("Deaths");
        tags.add("Recovered");
        tags.add("Active cases");

        //adding data and changing properties to pie
        PieDataSet pieDataSet = new PieDataSet(numbers, "pieDataSetLaber");
        pieDataSet.setSliceSpace(2);
        pieDataSet.setValueTextSize(12);
        pieDataSet.setLabel(null);

        //colors defining
        ArrayList<Integer> colors = new ArrayList<>();
        colors.add(Color.rgb(125, 15, 15));
        colors.add(Color.rgb(0, 90, 90));
        colors.add(Color.rgb(100, 70, 130));
        pieDataSet.setColors(colors);
        pieDataSet.setValueTextColor(Color.WHITE);

        //creating pieData object
        PieData pieData = new PieData(pieDataSet);
        //adjusting pieData obj
        pieChart.setData(pieData);
        pieChart.setRotationEnabled(false);
        pieChart.setHoleRadius(0);
        pieChart.setTransparentCircleAlpha(0);
        pieChart.setDescription(null);
        pieData.setValueFormatter(new MyFormatter());
        pieChart.invalidate();
        pieChart.getLegend().setEnabled(false);
    }


    private void getDataForCountry(final String regionGiven) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final StringBuilder country = new StringBuilder();
                final StringBuilder numberOfCases = new StringBuilder();
                final StringBuilder numberOfDeaths = new StringBuilder();
                final StringBuilder numberOfRecovered = new StringBuilder();
                final StringBuilder numberOfNewCases = new StringBuilder();
                final StringBuilder numberOfActiveCases = new StringBuilder();

                country.append(regionGiven);

                try {
                    //connect to main worldofmeters website
                    Document document = Jsoup.connect("https://www.worldometers.info/coronavirus/").get();

                    //go thru table in website to find the element we want to extract
                    for (Element row : document.select("table#main_table_countries_today tr")) {
                        //arguments that we are looking for
                        if (row.select("td:nth-of-type(1)").text().equals(country.toString())) {
                            //extracting particular data from the row
                            //creating String with StringBuilder
                            if (row.select("td:nth-of-type(2)").text().equals("")) {
                                numberOfCases.append("0");
                            } else {
                                numberOfCases.append(row.select("td:nth-of-type(2)").text());
                            }
                            if (row.select("td:nth-of-type(4)").text().equals("")) {
                                numberOfDeaths.append("0");
                            } else {
                                numberOfDeaths.append(row.select("td:nth-of-type(4)").text());
                            }
                            if (row.select("td:nth-of-type(6)").text().equals("")) {
                                numberOfRecovered.append("0");
                            } else {
                                numberOfRecovered.append(row.select("td:nth-of-type(6)").text());
                            }
                            if (row.select("td:nth-of-type(3)").text().equals("")) {
                                numberOfNewCases.append("0");
                            } else {
                                numberOfNewCases.append(row.select("td:nth-of-type(3)").text());
                            }
                            if (row.select("td:nth-of-type(7)").text().equals("")) {
                                numberOfActiveCases.append("0");
                            } else {
                                numberOfActiveCases.append(row.select("td:nth-of-type(7)").text());
                            }
                        }
                    }
                    //setting data needed for the chart. migrating from string to integer
                    deaths = Integer.parseInt(numberOfDeaths.toString().replace(",", ""));
                    recovered = Integer.parseInt(numberOfRecovered.toString().replace(",", ""));
                    activeSick = Integer.parseInt(numberOfActiveCases.toString().replace(",", ""));


                } catch (IOException e) {
                    e.printStackTrace();
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        regionName.setText(country.toString());
                        casesTxt.setText("Cases: " + numberOfCases.toString());
                        deathsTxt.setText("Deaths: " + numberOfDeaths.toString());
                        recoveredTxt.setText("Recovered: " + numberOfRecovered.toString());
                        newCases.setText("New cases: " + numberOfNewCases.toString());
                        activeCases.setText("Active cases: " + numberOfActiveCases.toString());
                        pieChart.setVisibility(View.VISIBLE);
                        addDataToPieChart(deaths, recovered, activeSick);

                    }
                });
            }
        }).start();

    }


}

class MyFormatter implements IValueFormatter {

    private DecimalFormat mFormat;

    public MyFormatter() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mFormat = new DecimalFormat("###,###,###,###");
        }
    }

    @SuppressLint("NewApi")
    @Override
    public String getFormattedValue(float value, Entry entry, int dataSetIndex, ViewPortHandler viewPortHandler) {
        return mFormat.format(value);
    }


}