package main.java;

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.*;

/**
 * WebWorker class
 */
public class WebWorker extends Thread {
  private WebFrame frame;
  private String urlString;
  private int rowNum;
  private final String INTERRUPTED = "Interrupted";

  /**
   * Webworker constructor
   * @param  url    [String with url of file]
   * @param  rowNum [Number of rows]
   * @param  frame  [Webframe object]
   * @return        [WebWorker object]
   */
  public WebWorker(String url,int rowNum, WebFrame frame) {
    urlString = url;
    this.frame = frame;
    this.rowNum = rowNum;
  }

  /**
   * Run implementation
   */
  public void run(){
    frame.increaseThreads();

    System.out.println("Fetching...." + urlString);
    InputStream input = null;
    StringBuilder contents = null;
    try {
      URL url = new URL(urlString);
      URLConnection connection = url.openConnection();

      connection.setConnectTimeout(5000);

      connection.connect();
      input = connection.getInputStream();

      BufferedReader reader = new BufferedReader(new InputStreamReader(input));

      char[] array = new char[1000];
      int len;
      contents = new StringBuilder(1000);
      long start = System.currentTimeMillis();
      while ((len = reader.read(array, 0, array.length)) > 0) {
        System.out.println("Fetching...." + urlString + len);
        if(Thread.interrupted())
          frame.updateTable(rowNum, INTERRUPTED);
        contents.append(array, 0, len);
        Thread.sleep(100);
      }
      System.out.print(contents.toString());
      long end = System.currentTimeMillis();
      SimpleDateFormat ft = new SimpleDateFormat ("hh:mm:ss");
      Date dat = new Date();
      String curTime = ft.format(dat);
      String status = curTime + "   " + (end-start) + "ms   " + contents.length() + " bytes";
      frame.updateTable(rowNum, status);
    }

    catch(MalformedURLException ignored) {
      frame.updateTable(rowNum, "err");
      System.out.println("Exception: " + ignored.toString());
    }
    catch(InterruptedException exception) {
      frame.updateTable(rowNum, INTERRUPTED);
      System.out.println("Exception: " + exception.toString());
    }
    catch(IOException ignored) {
      frame.updateTable(rowNum, "err");
      System.out.println("Exception: " + ignored.toString());
    }
       // "finally" clause, to close the input stream
       // in any case
    finally {
      try{
        if (input != null) input.close();
      }
      catch(IOException ignored) {}
    }

    frame.decreaseThreads();
  }
}

