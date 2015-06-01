package ca.usask.chl848.btserver;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.DisplayMetrics;
import android.view.View;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by chl848 on 05/11/2015.
 */
public class MainView extends View {

    Paint m_paint;

    private String m_status;

    public class ClientInfo {
        float x;
        float y;
        float z;
        int color;

        boolean isSendingBall;
        String ballId;
        int ballColor;
        String receiverName;
    }
    private Map<String, ClientInfo> m_clients = new HashMap<>();

    public MainView(Context context) {
        super(context);

        m_paint = new Paint();
    }

    public void updateClientInfo(String senderName, int color, float x, float y, float z, boolean isSendingBall, String ballId, int ballColor, String receiverName) {
        ClientInfo clientInfo = new ClientInfo();
        clientInfo.x = x;
        clientInfo.y = y;
        clientInfo.z = z;
        clientInfo.color = color;
        clientInfo.isSendingBall = isSendingBall;
        clientInfo.ballId = ballId;
        clientInfo.ballColor = ballColor;
        clientInfo.receiverName = receiverName;
        m_clients.put(senderName, clientInfo);
    }

    public String cookMessage() {
        JSONArray msg = new JSONArray();
        for(Map.Entry<String,ClientInfo> entry : m_clients.entrySet()){
            JSONObject record = new JSONObject();
            String strkey = entry.getKey();
            ClientInfo strval = entry.getValue();
            try {
                record.put("name", strkey);
                record.put("color", strval.color);
                record.put("x", strval.x);
                record.put("y", strval.y);
                record.put("z", strval.z);
                record.put("isSendingBall", strval.isSendingBall);
                record.put("ballId", strval.ballId);
                record.put("ballColor", strval.ballColor);
                record.put("receiverName", strval.receiverName);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            msg.put(record);
        }

        return msg.toString();
    }

    public void setStatus(String status) {
        m_status = status;
    }

    @Override
    protected  void onDraw(Canvas canvas) {
        showConnections(canvas);
    }

    private void showConnections(Canvas canvas) {
        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();

        m_paint.setTextSize(30);
        m_paint.setColor(Color.RED);

        float x = 0.05f;
        float y = 0.2f;

        for(Map.Entry<String,ClientInfo> entry : m_clients.entrySet()){
            String strkey = entry.getKey();
            ClientInfo strval = entry.getValue();
            String output = strkey + " - Angle : " + strval.z;
            canvas.drawText(output, (int) (displayMetrics.widthPixels * x), (int) (displayMetrics.heightPixels * y), m_paint);
            y += 0.05f;
        }
    }

    private void showStatus(){
        MainActivity ma = (MainActivity)getContext();

        ma.setStatus(m_status);
    }

    public void removeDevice(String id) {
        if (m_clients.containsKey(id)) {
            m_clients.remove(id);
        }
    }
}
