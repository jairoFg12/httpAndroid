package com.example.jairofernandez.ejemplojson;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.apache.http.Header;
import org.json.JSONArray;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import modelos.Lamina;
import modelos.Operacion;


public class MainActivity extends ActionBarActivity {

    List<Lamina> registros;
    Spinner spinner;
    TextView tvResultado;
    EditText etAncho;
    EditText etAlto;
    Button btnCalcular;
    Float total= Float.valueOf(0);
    Float valorCm = Float.valueOf(0);
    ArrayList<Operacion> miOperacion;
    ListView lvOperacion;
    ArrayAdapter<Operacion> adapterOperacion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        spinner = (Spinner)findViewById(R.id.spLamina);
        tvResultado = (TextView)findViewById(R.id.tvTotal);
        etAncho = (EditText)findViewById(R.id.etAncho);
        etAlto = (EditText)findViewById(R.id.etAlto);
        btnCalcular = (Button)findViewById(R.id.btnCalcular);
        miOperacion = new ArrayList<Operacion>();
        lvOperacion = (ListView)findViewById(R.id.lvResultados);
        adapterOperacion = new ArrayAdapter<Operacion>(this,android.R.layout.simple_list_item_1,miOperacion);
        lvOperacion.setAdapter(adapterOperacion);

        long numberRegistro = Lamina.count(Lamina.class, null, null);
        Log.d("Cantidad", numberRegistro + "");
        if (numberRegistro == 0) {
            ObtDatos();
        }else{
            cargarSpiner();
        }

        btnCalcular.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                calcular();
            }
        });

        lvOperacion.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                Operacion borrado = (Operacion) lvOperacion.getSelectedItem();
                Integer posicion = i;//lvOperacion.getSelectedItemPosition();
                Integer conteo = miOperacion.size();

                Operacion operacionCal = miOperacion.get(posicion);

                Float valorItem = Float.valueOf(0);
                Toast.makeText(getApplicationContext(),"Has borrado "+operacionCal.getDescripcion()+".",Toast.LENGTH_LONG).show();
                valorItem = operacionCal.getValor();
                total = total - valorItem;
                DecimalFormat df = new DecimalFormat();
                df.setMaximumFractionDigits(2);
                tvResultado.setText(df.format(total).toString());
                miOperacion.remove(posicion);
                adapterOperacion.remove(miOperacion.get(posicion));
                adapterOperacion.notifyDataSetChanged();
                return false;
            }
        });
        lvOperacion.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

            }
        });
    }

    private void cargarSpiner(){
        registros = Lamina.listAll(Lamina.class);
        ArrayAdapter<Lamina> adapterF = new ArrayAdapter<Lamina>(this,android.R.layout.simple_spinner_dropdown_item,registros);
        spinner.setAdapter(adapterF);
    }

    private void Borrar(){
        total = Float.valueOf(0);
        etAncho.setText("");
        etAlto.setText("");
        tvResultado.setText("0");
        Toast.makeText(this,"Ha borrado las operaciones realizadas",Toast.LENGTH_LONG).show();
        notificacion("Borradas las operaciones.  Tucompualdia");
        //lvOperacion.setAdapter(null);
        adapterOperacion.clear();
        adapterOperacion.notifyDataSetChanged();
    }

    private void calcular(){
        Float ancho;
        Float alto;
        String anchoT;
        String altoT;
        Float totalParcial;
        DecimalFormat df = new DecimalFormat();
        df.setMaximumFractionDigits(2);
        Lamina miLamina = (Lamina) spinner.getSelectedItem();

        valorCm = miLamina.getValorCm();
        String descricpion = miLamina.toString();
        anchoT = etAncho.getText().toString();
        altoT = etAlto.getText().toString();

        if(anchoT.isEmpty() || altoT.isEmpty()){
            Toast.makeText(this,"Debe llenar todos los campos para calcular",Toast.LENGTH_SHORT).show();
        }else {
            ancho = Float.parseFloat(anchoT);
            alto = Float.parseFloat(altoT);
            totalParcial = ancho * alto * valorCm;

            total = total + totalParcial;

            tvResultado.setText(df.format(total).toString());
            miOperacion.add(new Operacion(descricpion, totalParcial));

            lvOperacion.setAdapter(adapterOperacion);

        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }


    public void ObtDatos() {
        AsyncHttpClient client = new AsyncHttpClient();
        String url = "http://multikloset.tucompualdia.net/kloset/catalogo";
        RequestParams parametros = new RequestParams();
        parametros.put("Android", "Android");
        client.post(url, parametros, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                if (statusCode == 200) {

                    obtDatosJSON(new String(responseBody));
                    Toast.makeText(getApplicationContext(), "Actualizando base de datos", Toast.LENGTH_LONG).show();

                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                notificacion("Error, revisa tu conexión.  Tucompualdia");
                Toast.makeText(getApplicationContext(), "No se ha podido actualizar la base de datos", Toast.LENGTH_LONG).show();
            }
        });
    }


    public ArrayList<String> obtDatosJSON(String response) {
        ArrayList<String> listado = new ArrayList<String>();
        try {
            JSONArray resulArray = new JSONArray(response);
            String texto;
            String tipoLamina;
            Float ancho;
            Float alto;
            Float valor;
            Float valorCm;
            Lamina.deleteAll(Lamina.class);
            for (int i = 0; i < resulArray.length(); i++) {
                texto = resulArray.getJSONObject(i).getString("tipoLamina") + " (" +
                        resulArray.getJSONObject(i).getString("alto") + "X" +
                        resulArray.getJSONObject(i).getString("ancho") + ")";
                listado.add(texto);

                tipoLamina = resulArray.getJSONObject(i).getString("tipoLamina");
                ancho = Float.parseFloat(resulArray.getJSONObject(i).getString("ancho"));
                alto = Float.parseFloat(resulArray.getJSONObject(i).getString("alto"));
                valor = Float.parseFloat(resulArray.getJSONObject(i).getString("valor"));
                valorCm = Float.parseFloat(resulArray.getJSONObject(i).getString("valorCm"));

                Lamina lamina = new Lamina(tipoLamina, ancho, alto, valor, valorCm);
                lamina.save();
                Log.d("results", texto);
                notificacion("La calculadora ha actualizado su base de datos.  Tucompualdia");
            }

            cargarSpiner();

        } catch (Exception e) {

        }


        return listado;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            ObtDatos();
            return true;
        }else if(id == R.id.action_borrar){
            Borrar();
        }else if(id == R.id.action_acerca){
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Desarrollado por tucompualdia, para Multikloset. Todos los derechos reservados")
                    .setTitle("Acerca de...")
                    .setCancelable(false)
                    .setNeutralButton("Aceptar",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                }
                            });
            AlertDialog alert = builder.create();
            alert.show();
        }else if(id == R.id.action_pagina){
            String link = "http://multikloset.tucompualdia.net/kloset/";
            showWebPage(link);
        }else if(id == R.id.action_plataforma){
            String link = "http://multikloset.tucompualdia.net/kloset/admin";
            showWebPage(link);
        }else if(id == R.id.action_sugerencias){
            sendEmail("soporte@tucompualdia.net");
        }else if(id == R.id.action_soporte){
            callPhoneNumber("+573176569802");
        }
        else if(id == R.id.action_social){
            share("http://facebook.com/tucompualdia","Síguenos en Facebook");
        }

        return super.onOptionsItemSelected(item);
    }

    public void showWebPage(String URL) {
        Intent intent = new Intent(Intent.ACTION_VIEW,
                Uri.parse(URL));

        startActivity(intent);
    }

    public void share(String linkShare, String titleShare){
        Intent intent = new Intent(Intent.ACTION_SEND);

        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, titleShare);
        intent.putExtra(Intent.EXTRA_TEXT, linkShare);

        startActivity(Intent.createChooser(intent, "Compartir"));
    }

    public void sendEmail(String Address){
        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.setType("plain/text");
        emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{Address});
        emailIntent .putExtra(android.content.Intent.EXTRA_SUBJECT, "Petición de soporte vía Android");

        //emailIntent .putExtra(android.content.Intent.EXTRA_TEXT, BodyText);
        startActivity(Intent.createChooser(emailIntent, "Enviar correo"));
    }

    public void callPhoneNumber(String phone) {
        Intent callIntent = new Intent(Intent.ACTION_CALL);
        callIntent.setData(Uri.parse("tel:" + phone));
        startActivity(callIntent);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public void notificacion(String mensaje){

        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pIntent = PendingIntent.getActivity(this, 0, intent, 0);

// build notification
// the addAction re-use the same intent to keep the example short
        Notification n  = new Notification.Builder(this)
                .setContentTitle("Multikloset")
                .setContentText(mensaje)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pIntent)
                .setAutoCancel(true)
                .build();


        NotificationManager notificationManager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        notificationManager.notify(0, n);
    }

}
