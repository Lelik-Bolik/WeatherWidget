package com.ezatsepin.weatherwidgetapp;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.os.StrictMode;
import android.support.annotation.RequiresApi;
import android.text.SpannableString;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.androidplot.ui.Anchor;
import com.androidplot.ui.HorizontalPositioning;
import com.androidplot.ui.Size;
import com.androidplot.ui.SizeMode;
import com.androidplot.ui.VerticalPositioning;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.StepMode;
import com.androidplot.xy.StepModel;
import com.androidplot.xy.XYGraphWidget;
import com.androidplot.xy.XYSeries;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

/**
 * Implementation of App Widget functionality.
 */
public class WeatherWidget extends AppWidgetProvider {

    private static final String WIDGET_CLICKED = "WIDGET_CLICKED";
    private static final String ACTION_APPWIDGET_UPDATE = AppWidgetManager.ACTION_APPWIDGET_UPDATE;

    protected PendingIntent getPendingSelfIntent(Context context, String action) {
        Intent intent = new Intent(context, getClass());
        intent.setAction(action);
        return PendingIntent.getBroadcast(context, 0, intent, 0);
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.weather_widget);
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }


    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.weather_widget);
        ComponentName watchWidget = new ComponentName(context, WeatherWidget.class);

        remoteViews.setOnClickPendingIntent(R.id.widget_layout, getPendingSelfIntent(context, WIDGET_CLICKED));

        appWidgetManager.updateAppWidget(watchWidget, remoteViews);

    }

    @Override
    public void onEnabled(Context context) {
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.weather_widget);
        ComponentName watchWidget = new ComponentName(context, WeatherWidget.class);

        remoteViews.setOnClickPendingIntent(R.id.widget_layout, getPendingSelfIntent(context, WIDGET_CLICKED));
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public Bitmap combinePlots(Bitmap c, Bitmap s) {
        Bitmap cs = null;

        int width = c.getWidth(), height = c.getHeight() + s.getHeight();

        cs = Bitmap.createBitmap(width, height, Bitmap.Config.RGBA_F16);

        Canvas comboImage = new Canvas(cs);

        comboImage.drawBitmap(c, 0f, 0f, null);
        comboImage.drawBitmap(s, 0f, c.getHeight(), null);

        return cs;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        final String action = intent.getAction();

        if (WIDGET_CLICKED.equals(action) || ACTION_APPWIDGET_UPDATE.equals(action)) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.weather_widget);
            ComponentName watchWidget = new ComponentName(context, WeatherWidget.class);

            Integer height = 190,
                    width = 440;

            Format daysFormat = new Format() {
                @Override
                public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
                    long date = ((long) ((Number) obj).intValue() + 3600) * 1000;
                    java.util.Date time = new java.util.Date(date);
                    SimpleDateFormat simpleDateformat;
                    simpleDateformat = new SimpleDateFormat("E");
                    String dow = simpleDateformat.format(date);
                    String add = "";
                    if (dow.equals("Sun")) {
                        add = "❗";
                    }

                    return toAppendTo.append(time.getDate()).append(add);
                }

                @Override
                public Object parseObject(String source, ParsePosition pos) {
                    // unused
                    return null;
                }
            };

            ///////////////////////////////////////////////

            XYPlot plot = new XYPlot(context, "");
            plot.setDrawingCacheEnabled(true);

            plot.setBackgroundColor(Color.TRANSPARENT);
            plot.getGraph().getBackgroundPaint().setColor(Color.TRANSPARENT);
            plot.getGraph().getGridBackgroundPaint().setColor(Color.WHITE);
            plot.getGraph().setPadding(35, 25, 20, 20);
            plot.getGraph().setMargins(0, 0, 0, 0);

            Size sz = new Size(height, SizeMode.ABSOLUTE, width, SizeMode.ABSOLUTE);
            plot.getGraph().setSize(sz);

            plot.layout(0, 0, width, height);

            ///////////////////////////////////////////////

            XYPlot plot2 = new XYPlot(context, "");
            plot2.setDrawingCacheEnabled(true);


            plot2.setBackgroundColor(Color.TRANSPARENT);
            plot2.getGraph().getBackgroundPaint().setColor(Color.TRANSPARENT);
            plot2.getGraph().getGridBackgroundPaint().setColor(Color.WHITE);
            plot2.getGraph().setPadding(35, 25, 20, 20);
            plot2.getGraph().setMargins(0, 0, 0, 0);


            Size sz2 = new Size(height, SizeMode.ABSOLUTE, width, SizeMode.ABSOLUTE);
            plot2.getGraph().setSize(sz);
            plot2.layout(0, 0, width, height);

            ///////////////////////////////////////////////

            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);

            String str = "http://localhost/weatherapi.php";
            URLConnection urlConn = null;
            BufferedReader bufferedReader = null;
            try
            {
                URL url = new URL(str);
                urlConn = url.openConnection();
                bufferedReader = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));

                StringBuffer stringBuffer = new StringBuffer();
                String line;

                while ((line = bufferedReader.readLine()) != null)
                {
                    stringBuffer.append(line);
                }

                try {
                    JSONObject jsonObj = new JSONObject(stringBuffer.toString());

                    String timeStamp = new SimpleDateFormat("HH:mm").format(Calendar.getInstance().getTime());

                    plot.getTitle().position(18, HorizontalPositioning.ABSOLUTE_FROM_RIGHT, 3, VerticalPositioning.ABSOLUTE_FROM_TOP, Anchor.RIGHT_TOP);
                    plot.setTitle(timeStamp);

                    Integer origin = jsonObj.getInt("origin");

                    JSONArray data_hours = jsonObj.getJSONArray("d");
                    Integer[] seriesHours = new Integer[data_hours.length()];
                    for (int i = 0; i < data_hours.length(); ++i) {
                        seriesHours[i] = data_hours.optInt(i);
                    }

                    //////////////////

                    JSONArray data_tmp = jsonObj.getJSONArray("tmp");
                    Number[] seriesTmp = new Number[data_tmp.length()];
                    Number[] seriesTmpP = new Number[data_tmp.length()];
                    Number[] seriesTmpM = new Number[data_tmp.length()];
                    Integer[] seriesZero = new Integer[data_tmp.length()];
                    Integer[] series30 = new Integer[data_tmp.length()];
                    for (int i = 0; i < data_tmp.length(); ++i) {
                        seriesTmp[i] = data_tmp.optDouble(i);
                        seriesTmpP[i] = data_tmp.optDouble(i) >= 0 ? data_tmp.optDouble(i) : 0;
                        seriesTmpM[i] = data_tmp.optDouble(i) >= 0 ? 0: data_tmp.optDouble(i);
                        seriesZero[i] = 0;
                        series30[i] = 30;
                    }


                    List<Integer> ListHours = Arrays.asList(seriesHours);

                    List<Integer> List30 = Arrays.asList(series30);
                    XYSeries series30XY = new SimpleXYSeries(ListHours, List30, "");
                    LineAndPointFormatter series30Format = new LineAndPointFormatter(context, R.xml.line_formatter_tmp_30);
                    plot.addSeries(series30XY, series30Format);

                    List<Number> ListTmpP = Arrays.asList(seriesTmpP);
                    XYSeries series1 = new SimpleXYSeries(ListHours, ListTmpP, "");
                    LineAndPointFormatter series1Format = new LineAndPointFormatter(context, R.xml.line_formatter_tmp);
                    plot.addSeries(series1, series1Format);

                    List<Number> ListTmpM = Arrays.asList(seriesTmpM);
                    XYSeries series1m = new SimpleXYSeries(ListHours, ListTmpM, "");
                    LineAndPointFormatter series1mFormat = new LineAndPointFormatter(context, R.xml.line_formatter_tmp_m);
                    plot.addSeries(series1m, series1mFormat);

                    List<Integer> ListZero = Arrays.asList(seriesZero);
                    XYSeries series0 = new SimpleXYSeries(ListHours, ListZero, "");
                    LineAndPointFormatter series0Format = new LineAndPointFormatter(context, R.xml.line_formatter_tmp_zero);
                    plot.addSeries(series0, series0Format);


                    plot.getLegend().setHeight(0);
                    plot.getLegend().setVisible(false);

                    StepModel sm = new StepModel(StepMode.INCREMENT_BY_FIT, 86400);
                    plot.setDomainStepModel(sm);

                    plot.setUserDomainOrigin(origin);


                    XYGraphWidget.Edge[] edges = {XYGraphWidget.Edge.BOTTOM, XYGraphWidget.Edge.LEFT};

                    plot.getGraph().setLineLabelEdges(edges);
                    plot.getGraph().getLineLabelInsets().setBottom(-12);
                    plot.getGraph().getLineLabelStyle(XYGraphWidget.Edge.BOTTOM).getPaint().setTextAlign(Paint.Align.LEFT);
                    plot.getGraph().getLineLabelStyle(XYGraphWidget.Edge.BOTTOM).getPaint().setTextSize(13);
                    plot.getGraph().getLineLabelStyle(XYGraphWidget.Edge.BOTTOM).getPaint().setColor(Color.WHITE);


                    plot.getGraph().getLineLabelStyle(XYGraphWidget.Edge.BOTTOM).setFormat(daysFormat);

                    plot.getGraph().getLineLabelInsets().setLeft(-2);
                    plot.getGraph().getLineLabelStyle(XYGraphWidget.Edge.LEFT).getPaint().setTextAlign(Paint.Align.RIGHT);
                    plot.getGraph().getLineLabelStyle(XYGraphWidget.Edge.LEFT).getPaint().setTextSize(14);
                    plot.getGraph().getLineLabelStyle(XYGraphWidget.Edge.LEFT).getPaint().setColor(Color.WHITE);
                    plot.getGraph().getLineLabelStyle(XYGraphWidget.Edge.LEFT).setFormat(new DecimalFormat("0"));

                    Arrays.sort(seriesTmp);

                    double minval = seriesTmp[0].intValue() - 1;
                    double maxval = seriesTmp[seriesTmp.length-1].intValue() + 1;
                    plot.setRangeBoundaries(minval, maxval, BoundaryMode.FIXED);

                    plot.setUserRangeOrigin(0);


                    StepModel smr = new StepModel(StepMode.INCREMENT_BY_FIT, 3);
                    plot.setRangeStepModel(smr);




                    Bitmap bmp = plot.getDrawingCache();
                    //remoteViews.setBitmap(R.id.plot, "setImageBitmap", bmp);


                    /////////////


                    JSONArray data_prec = jsonObj.getJSONArray("prec");
                    Number[] seriesPrec = new Number[data_prec.length()];
                    for (int i = 0; i < data_prec.length(); ++i) {
                        seriesPrec[i] = data_prec.optDouble(i);
                    }

                    JSONArray data_cloud = jsonObj.getJSONArray("cloud");
                    Number[] seriesCloud = new Number[data_cloud.length()];
                    for (int i = 0; i < data_cloud.length(); ++i) {
                        seriesCloud[i] = data_cloud.optDouble(i);
                    }

                    JSONArray data_snow = jsonObj.getJSONArray("snow");
                    Number[] seriesSnow = new Number[data_snow.length()];
                    for (int i = 0; i < data_snow.length(); ++i) {
                        seriesSnow[i] = data_snow.optDouble(i);
                    }

                    JSONArray data_press = jsonObj.getJSONArray("press");
                    Number[] seriesPress = new Number[data_press.length()];
                    for (int i = 0; i < data_press.length(); ++i) {
                        seriesPress[i] = data_press.optDouble(i);
                    }

                    JSONArray data_tstorm = jsonObj.getJSONArray("tstorm");
                    Number[] seriesTStorm = new Number[data_tstorm.length()];
                    for (int i = 0; i < data_tstorm.length(); ++i) {
                        seriesTStorm[i] = data_tstorm.optDouble(i);
                        if (seriesTStorm[i].doubleValue() == 0) {
                            seriesTStorm[i] = null;
                        }
                    }

                    JSONArray data_wind = jsonObj.getJSONArray("wind");
                    Number[] seriesWind = new Number[data_wind.length()];
                    for (int i = 0; i < data_wind.length(); ++i) {
                        seriesWind[i] = data_wind.optDouble(i);
                    }


                    List<Number> ListPrec = Arrays.asList(seriesPrec);
                    XYSeries series2 = new SimpleXYSeries(ListHours, ListPrec, "");
                    LineAndPointFormatter series2Format = new LineAndPointFormatter(context, R.xml.line_formatter_prec);
                    plot2.addSeries(series2, series2Format);

                    List<Number> ListCloud = Arrays.asList(seriesCloud);
                    XYSeries series3 = new SimpleXYSeries(ListHours, ListCloud, "");
                    LineAndPointFormatter series3Format = new LineAndPointFormatter(context, R.xml.line_formatter_cloud);
                    plot2.addSeries(series3, series3Format);

                    List<Number> ListSnow = Arrays.asList(seriesSnow);
                    XYSeries series4 = new SimpleXYSeries(ListHours, ListSnow, "");
                    LineAndPointFormatter series4Format = new LineAndPointFormatter(context, R.xml.line_formatter_snow);
                    plot2.addSeries(series4, series4Format);

                    List<Number> ListTStorm = Arrays.asList(seriesTStorm);
                    XYSeries series5 = new SimpleXYSeries(ListHours, ListTStorm, "");
                    LineAndPointFormatter series5Format = new LineAndPointFormatter(context, R.xml.line_formatter_tstorm);
                    plot2.addSeries(series5, series5Format);

                    List<Number> ListPress = Arrays.asList(seriesPress);
                    XYSeries series6 = new SimpleXYSeries(ListHours, ListPress, "");
                    LineAndPointFormatter series6Format = new LineAndPointFormatter(context, R.xml.line_formatter_press);
                    plot2.addSeries(series6, series6Format);

                    List<Number> ListWind = Arrays.asList(seriesWind);
                    XYSeries series7 = new SimpleXYSeries(ListHours, ListWind, "");
                    LineAndPointFormatter series7Format = new LineAndPointFormatter(context, R.xml.line_formatter_wind);
                    plot2.addSeries(series7, series7Format);


                    plot2.getLegend().setHeight(0);
                    plot2.getLegend().setVisible(false);

                    StepModel sm2 = new StepModel(StepMode.INCREMENT_BY_FIT, 86400);
                    plot2.setDomainStepModel(sm2);

                    plot2.setUserDomainOrigin(origin);

                    XYGraphWidget.Edge[] edges2 = {XYGraphWidget.Edge.BOTTOM, XYGraphWidget.Edge.LEFT};


                    plot2.getGraph().getLineLabelStyle(XYGraphWidget.Edge.BOTTOM).setFormat(daysFormat);

                    plot2.getGraph().getLineLabelStyle(XYGraphWidget.Edge.LEFT).getPaint().setTextAlign(Paint.Align.RIGHT);
                    plot2.getGraph().getLineLabelStyle(XYGraphWidget.Edge.LEFT).getPaint().setTextSize(14);
                    plot2.getGraph().getLineLabelStyle(XYGraphWidget.Edge.LEFT).getPaint().setColor(Color.WHITE);
                    plot2.getGraph().getLineLabelStyle(XYGraphWidget.Edge.LEFT).setFormat(new DecimalFormat("0"));


                    Arrays.sort(seriesPrec);

                    plot2.setRangeBoundaries(0, 100, BoundaryMode.FIXED);


                    StepModel sm2r = new StepModel(StepMode.SUBDIVIDE, 6);
                    plot2.setRangeStepModel(sm2r);

                    plot2.getGraph().setLineLabelEdges(edges2);
                    plot2.getGraph().getLineLabelInsets().setBottom(-12);
                    plot2.getGraph().getLineLabelInsets().setLeft(-2);
                    plot2.getGraph().getLineLabelStyle(XYGraphWidget.Edge.BOTTOM).getPaint().setTextAlign(Paint.Align.LEFT);
                    plot2.getGraph().getLineLabelStyle(XYGraphWidget.Edge.BOTTOM).getPaint().setTextSize(13);
                    plot2.getGraph().getLineLabelStyle(XYGraphWidget.Edge.BOTTOM).getPaint().setColor(Color.WHITE);



                    Bitmap bmp2 = plot2.getDrawingCache();
                    Bitmap bmpComb = combinePlots(bmp, bmp2);

                    remoteViews.setBitmap(R.id.plot, "setImageBitmap", bmpComb);


                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
            catch(Exception ex)
            {
                Log.e("App", "yourDataTask", ex);
            }
            finally
            {
                if(bufferedReader != null)
                {
                    try {
                        bufferedReader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            appWidgetManager.updateAppWidget(watchWidget, remoteViews);
            Toast.makeText(context, R.string.data_updated, Toast.LENGTH_SHORT).show();
        }
    }
}

