package ca.usask.chl848.btserver;

import android.content.Context;
import android.os.Environment;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;

/**
 * Created by chl848 on 06/09/2015.
 */
public class MainLogger {
    private BufferedWriter m_bufferedWriter;

    public MainLogger(Context context, String fileName) {
        String SUFFIX = ".txt";
        Calendar calendar = Calendar.getInstance();
        String year = String.valueOf(calendar.get(Calendar.YEAR));
        String month = String.valueOf(calendar.get(Calendar.MONTH) + 1);
        String date = String.valueOf(calendar.get(Calendar.DATE));
        String hour = String.valueOf(calendar.get(Calendar.HOUR_OF_DAY));
        String minute = String.valueOf(calendar.get(Calendar.MINUTE));
        String second = String.valueOf(calendar.get(Calendar.SECOND));
        String time = year+"-"+month+"-"+date+"-"+hour+"-"+minute+"-"+second;
        String targetPath = Environment.getExternalStorageDirectory().getPath() + File.separator + fileName + "-" + time + SUFFIX;
        File targetFile = new File(targetPath);
        if (targetFile != null) {
            if (!targetFile.exists()) {
                try {
                    if (!targetFile.createNewFile())
                    {
                        Toast.makeText(context, "Can not create log file!", Toast.LENGTH_SHORT).show();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                if (!targetFile.delete()) {
                    Toast.makeText(context, "Can not delete old log file!", Toast.LENGTH_SHORT).show();
                } else {
                    try {
                        if (!targetFile.createNewFile())
                        {
                            Toast.makeText(context, "Can not create log file!", Toast.LENGTH_SHORT).show();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            try {
                m_bufferedWriter = new BufferedWriter(new FileWriter(targetPath, true));
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    public void writeHeaders(String str) {
        try {
            m_bufferedWriter.write(str);
            m_bufferedWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
            try {
                m_bufferedWriter.flush();
                m_bufferedWriter.close();
            } catch (IOException e2) {
                e2.printStackTrace();
            }
        }
    }

    public void write(String str, boolean isFlush) {
        try {
            m_bufferedWriter.write(System.getProperty("line.separator"));
            m_bufferedWriter.write(str);
            if (isFlush) {
                m_bufferedWriter.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
            try {
                m_bufferedWriter.flush();
                m_bufferedWriter.close();
            } catch (IOException e2) {
                e2.printStackTrace();
            }
        }
    }

    public void flush() {
        try {
            m_bufferedWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void close(){
        try {
            m_bufferedWriter.flush();
            m_bufferedWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
