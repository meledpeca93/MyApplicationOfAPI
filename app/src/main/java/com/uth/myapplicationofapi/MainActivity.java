package com.uth.myapplicationofapi;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;

public class MainActivity extends AppCompatActivity {

    private static final String BASE_URL_GET = "https://jsonplaceholder.typicode.com/posts/1";
    private static final String BASE_URL_POST = "https://jsonplaceholder.typicode.com/posts";

    // Componentes de la Interfaz de Usuario
    private MaterialCardView cardStatus;
    private ImageView ivStatusIcon;
    private ProgressBar progressBar;
    private TextView tvStatus;
    private TextView tvLatency;
    private TextView tvResponse;

    // Motores HTTP
    private OkHttpClient okHttpClient;
    private RequestQueue volleyRequestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Vistas UI
        cardStatus = findViewById(R.id.cardStatus);
        ivStatusIcon = findViewById(R.id.ivStatusIcon);
        progressBar = findViewById(R.id.progressBar);
        tvStatus = findViewById(R.id.tvStatus);
        tvLatency = findViewById(R.id.tvLatency);
        tvResponse = findViewById(R.id.tvResponse);

        // Botones de Acción
        MaterialButton btnOkHttpGet = findViewById(R.id.btnOkHttpGet);
        MaterialButton btnOkHttpPost = findViewById(R.id.btnOkHttpPost);
        MaterialButton btnVolleyGet = findViewById(R.id.btnVolleyGet);
        MaterialButton btnVolleyPost = findViewById(R.id.btnVolleyPost);
        MaterialButton btnCopy = findViewById(R.id.btnCopy);
        MaterialButton btnClear = findViewById(R.id.btnClear);

        // Inicialización de OkHttp con Timeouts
        okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .build();

        // Inicialización de Volley RequestQueue
        volleyRequestQueue = Volley.newRequestQueue(this);

        // Asignación de Click Listeners
        btnOkHttpGet.setOnClickListener(v -> makeOkHttpGetRequest());
        btnOkHttpPost.setOnClickListener(v -> makeOkHttpPostRequest());
        btnVolleyGet.setOnClickListener(v -> makeVolleyGetRequest());
        btnVolleyPost.setOnClickListener(v -> makeVolleyPostRequest());

        btnCopy.setOnClickListener(v -> copyResponseToClipboard());
        btnClear.setOnClickListener(v -> resetState());

        // Estado Inicial
        setUIStateReady();
    }

    // ==========================================
    // SECCIÓN 1: SOLICITUDES HTTP CON OKHTTP
    // ==========================================

    private void makeOkHttpGetRequest() {
        long startTime = System.currentTimeMillis();
        setUIStateLoading("OkHttp GET");

        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(BASE_URL_GET)
                .get()
                .build();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> setUIStateError("OkHttp GET", e.getLocalizedMessage()));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull okhttp3.Response response) throws IOException {
                long latency = System.currentTimeMillis() - startTime;
                if (response.isSuccessful() && response.body() != null) {
                    String jsonRaw = response.body().string();
                    int statusCode = response.code();
                    runOnUiThread(() -> setUIStateSuccess("OkHttp GET", statusCode, latency, jsonRaw));
                } else {
                    int statusCode = response.code();
                    runOnUiThread(() -> setUIStateError("OkHttp GET", "Código HTTP: " + statusCode));
                }
            }
        });
    }

    private void makeOkHttpPostRequest() {
        long startTime = System.currentTimeMillis();
        setUIStateLoading("OkHttp POST");

        try {
            JSONObject postData = new JSONObject();
            postData.put("title", "Post desde OkHttp (Java UI)");
            postData.put("body", "Demostración de envío POST con OkHttp y diseño Material 3.");
            postData.put("userId", 1);

            MediaType JSON = MediaType.parse("application/json; charset=utf-8");
            RequestBody body = RequestBody.create(postData.toString(), JSON);

            okhttp3.Request request = new okhttp3.Request.Builder()
                    .url(BASE_URL_POST)
                    .post(body)
                    .build();

            okHttpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    runOnUiThread(() -> setUIStateError("OkHttp POST", e.getLocalizedMessage()));
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull okhttp3.Response response) throws IOException {
                    long latency = System.currentTimeMillis() - startTime;
                    if (response.isSuccessful() && response.body() != null) {
                        String jsonRaw = response.body().string();
                        int statusCode = response.code();
                        runOnUiThread(() -> setUIStateSuccess("OkHttp POST", statusCode, latency, jsonRaw));
                    } else {
                        int statusCode = response.code();
                        runOnUiThread(() -> setUIStateError("OkHttp POST", "Código HTTP: " + statusCode));
                    }
                }
            });

        } catch (JSONException e) {
            setUIStateError("OkHttp POST", "Error estructurando JSON: " + e.getMessage());
        }
    }

    // ==========================================
    // SECCIÓN 2: SOLICITUDES HTTP CON VOLLEY
    // ==========================================

    private void makeVolleyGetRequest() {
        long startTime = System.currentTimeMillis();
        setUIStateLoading("Volley GET");

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.GET,
                BASE_URL_GET,
                null,
                response -> {
                    long latency = System.currentTimeMillis() - startTime;
                    setUIStateSuccess("Volley GET", 200, latency, response.toString());
                },
                error -> {
                    String msg = error.getMessage() != null ? error.getMessage() : "Error de red o timeout";
                    if (error.networkResponse != null) {
                        msg = "Código HTTP: " + error.networkResponse.statusCode;
                    }
                    setUIStateError("Volley GET", msg);
                }
        );

        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(
                10000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        volleyRequestQueue.add(jsonObjectRequest);
    }

    private void makeVolleyPostRequest() {
        long startTime = System.currentTimeMillis();
        setUIStateLoading("Volley POST");

        try {
            JSONObject postData = new JSONObject();
            postData.put("title", "Post desde Volley (Java UI)");
            postData.put("body", "Demostración de envío POST con Volley y diseño Material 3.");
            postData.put("userId", 1);

            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                    Request.Method.POST,
                    BASE_URL_POST,
                    postData,
                    response -> {
                        long latency = System.currentTimeMillis() - startTime;
                        setUIStateSuccess("Volley POST", 201, latency, response.toString());
                    },
                    error -> {
                        String msg = error.getMessage() != null ? error.getMessage() : "Error de red o timeout";
                        if (error.networkResponse != null) {
                            msg = "Código HTTP: " + error.networkResponse.statusCode;
                        }
                        setUIStateError("Volley POST", msg);
                    }
            );

            volleyRequestQueue.add(jsonObjectRequest);

        } catch (JSONException e) {
            setUIStateError("Volley POST", "Error JSON: " + e.getMessage());
        }
    }

    // ==========================================
    // GESTIÓN DE ESTADOS Y COLORES DE LA UI
    // ==========================================

    private void setUIStateReady() {
        cardStatus.setCardBackgroundColor(ContextCompat.getColor(this, R.color.status_ready_bg));
        ivStatusIcon.setImageResource(R.drawable.ic_api);
        ivStatusIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.status_ready_text)));
        ivStatusIcon.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.GONE);
        tvStatus.setText(R.string.status_ready);
        tvStatus.setTextColor(ContextCompat.getColor(this, R.color.status_ready_text));
        tvLatency.setVisibility(View.GONE);
    }

    private void setUIStateLoading(String tag) {
        cardStatus.setCardBackgroundColor(ContextCompat.getColor(this, R.color.status_loading_bg));
        ivStatusIcon.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
        tvStatus.setText("Ejecutando " + tag + "...");
        tvStatus.setTextColor(ContextCompat.getColor(this, R.color.status_loading_text));
        tvLatency.setVisibility(View.GONE);
    }

    private void setUIStateSuccess(String tag, int statusCode, long latencyMs, String rawJson) {
        cardStatus.setCardBackgroundColor(ContextCompat.getColor(this, R.color.status_success_bg));
        ivStatusIcon.setImageResource(R.drawable.ic_check);
        ivStatusIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.status_success_text)));
        ivStatusIcon.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.GONE);

        tvStatus.setText(tag + " completado • HTTP " + statusCode);
        tvStatus.setTextColor(ContextCompat.getColor(this, R.color.status_success_text));

        tvLatency.setText(latencyMs + " ms");
        tvLatency.setVisibility(View.VISIBLE);

        tvResponse.setText(formatJsonString(rawJson));
    }

    private void setUIStateError(String tag, String errorMessage) {
        cardStatus.setCardBackgroundColor(ContextCompat.getColor(this, R.color.status_error_bg));
        ivStatusIcon.setImageResource(R.drawable.ic_error);
        ivStatusIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.status_error_text)));
        ivStatusIcon.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.GONE);

        tvStatus.setText(tag + " falló");
        tvStatus.setTextColor(ContextCompat.getColor(this, R.color.status_error_text));
        tvLatency.setVisibility(View.GONE);

        tvResponse.setText("// ❌ " + tag + " Error:\n// " + errorMessage);
        Toast.makeText(this, "Error en " + tag, Toast.LENGTH_SHORT).show();
    }

    private void resetState() {
        setUIStateReady();
        tvResponse.setText(R.string.response_placeholder);
    }

    private void copyResponseToClipboard() {
        String textToCopy = tvResponse.getText().toString();
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("JSON_Response", textToCopy);
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, R.string.copied_toast, Toast.LENGTH_SHORT).show();
        }
    }

    private String formatJsonString(String rawJson) {
        try {
            if (rawJson.startsWith("{")) {
                JSONObject jsonObject = new JSONObject(rawJson);
                return jsonObject.toString(4);
            } else if (rawJson.startsWith("[")) {
                org.json.JSONArray jsonArray = new org.json.JSONArray(rawJson);
                return jsonArray.toString(4);
            }
        } catch (JSONException ignored) {
        }
        return rawJson;
    }
}