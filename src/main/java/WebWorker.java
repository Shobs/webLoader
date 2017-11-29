package main.java;

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.*;

public class WebWorker extends Thread {
  private WebFrame frame;
  private String urlString;
  private int rowNum;
  private final String INTERRUPTED = "Interrupted";

  public WebWorker(String url,int rowNum, WebFrame frame) {
    urlString = url;
    this.frame = frame;
    this.rowNum = rowNum;
  }

  public void run(){
    frame.increaseThreads();
    download();
    frame.decreaseThreads();
  }

  private void download(){
    InputStream input = null;
    StringBuilder contents = null;
    try {
      URL url = new URL(urlString);
      URLConnection connection = url.openConnection();

           // Set connect() to throw an IOException
           // if connection does not succeed in this many msecs.
      connection.setConnectTimeout(5000);

      connection.connect();
      input = connection.getInputStream();

      BufferedReader reader = new BufferedReader(new InputStreamReader(input));

      char[] array = new char[1000];
      int len;
      contents = new StringBuilder(1000);
      long start = System.currentTimeMillis();
      while ((len = reader.read(array, 0, array.length)) > 0) {
        if(Thread.interrupted()){
          frame.updateTable(rowNum, INTERRUPTED);
        }
        contents.append(array, 0, len);
        Thread.sleep(100);
      }
      long end = System.currentTimeMillis();
      SimpleDateFormat ft = new SimpleDateFormat ("hh:mm:ss");
      Date dat = new Date();
      String curTime = ft.format(dat);
      String status = curTime + "   " + (end-start) + "ms   " + contents.length() + " bytes";
      frame.updateTable(rowNum, status);
    }
       // Otherwise control jumps to a catch...
    catch(MalformedURLException ignored) {
      frame.updateTable(rowNum, "err");
    }
    catch(InterruptedException exception) {
      frame.updateTable(rowNum, INTERRUPTED);
    }
    catch(IOException ignored) {
      frame.updateTable(rowNum, "err");
    }
       // "finally" clause, to close the input stream
       // in any case
    finally {
      try{
        if (input != null) input.close();
      }
      catch(IOException ignored) {}
    }
  }
}

