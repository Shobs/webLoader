package main.java;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.concurrent.Semaphore;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

public class WebFrame extends JFrame {

  private DefaultTableModel model;
  private JTable table;
  private JPanel panel;
  private JButton single,concurrent,stop;
  private JTextField field;
  private JLabel running,completed,elapsed;
  private JProgressBar progress;
  private int runningThreads;
  private Semaphore sem;
  private int completedWorks;
  private Thread t;


  public WebFrame(String filename) {

    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setLayout(new BoxLayout(this.getContentPane(), BoxLayout.Y_AXIS));
    panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    model = new DefaultTableModel(new String[] { "url", "status"}, 0);
    table = new JTable(model);
    table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
    JScrollPane scrollpane = new JScrollPane(table);
    scrollpane.setPreferredSize(new Dimension(600,300));
    panel.add(scrollpane);
    ReadFile(filename);
    single = new JButton("Single Thread Fetch");
    single.addActionListener(new SingleThread());
    concurrent = new JButton("Concurrent Fetch");
    concurrent.addActionListener(new Concurrent());
    stop = new JButton("Stop");
    stop.addActionListener(new StopAction());
    stop.setEnabled(false);
    field = new JTextField();
    field.setMaximumSize(new Dimension(50,JTextField.HEIGHT));
    running = new JLabel("Running: 0");
    completed = new JLabel("Completed: 0");
    elapsed = new JLabel("Elapsed: 0");
    progress = new JProgressBar();

    panel.add(Box.createRigidArea(new Dimension(0, 5)));
    panel.add(single);
    panel.add(Box.createRigidArea(new Dimension(0, 5)));
    panel.add(concurrent);
    panel.add(Box.createRigidArea(new Dimension(0, 5)));
    panel.add(field);
    panel.add(Box.createRigidArea(new Dimension(0, 5)));
    panel.add(running);
    panel.add(Box.createRigidArea(new Dimension(0, 5)));
    panel.add(completed);
    panel.add(Box.createRigidArea(new Dimension(0, 5)));
    panel.add(elapsed);
    panel.add(Box.createRigidArea(new Dimension(0, 5)));
    panel.add(progress);
    panel.add(Box.createRigidArea(new Dimension(0, 5)));
    panel.add(stop);

    this.add(panel);
    this.pack();
    this.setVisible(true);
  }

  public void increaseThreads(){
    runningThreads++;
    updateRunningLabel();
  }

  public synchronized void decreaseThreads(){
    runningThreads--;
    updateRunningLabel();
    sem.release();
  }

  private void increaseCompledted(){
    completedWorks++;
    completed.setText("Completed: " + completedWorks);
  }

  private void updateRunningLabel(){
    running.setText("Running: " + runningThreads);
  }

  public void updateTable(int rowNum,String str){
    increaseCompledted();
    progress.setValue(completedWorks);
    model.setValueAt(str, rowNum, 1);
  }

  private void ReadFile(String filename) {
    try {
      BufferedReader buff = new BufferedReader(new FileReader("link.txt"));
      try {
        String line = buff.readLine();
        while(true){
          if(line == null)
            break;
          model.addRow(new String[]{line, ""});
          line = buff.readLine();
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
  }

  public static void main(String[] args) {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    } catch (InstantiationException e) {
      e.printStackTrace();
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    } catch (UnsupportedLookAndFeelException e) {
      e.printStackTrace();
    }
    WebFrame w = new WebFrame("linktxt");
  }

  private void statusReset(){
    completedWorks = 0;
    progress.setValue(0);
    running.setText("Running: 0");
    completed.setText("Completed: 0");
    elapsed.setText("Elapsed: 0");
    progress.setValue(0);
  }

  private void fetchButtonClicked(){
    statusReset();
    stop.setEnabled(true);
    single.setEnabled(false);
    concurrent.setEnabled(false);
    progress.setMaximum(model.getRowCount());
  }

  private class SingleThread implements ActionListener{

    @Override
    public void actionPerformed(ActionEvent e) {
      fetchButtonClicked();
      t = new Thread(new Launcher(1));
      t.start();
    }
  }

  private class Concurrent implements ActionListener{

    @Override
    public void actionPerformed(ActionEvent e) {
      fetchButtonClicked();
      t = new Thread(new Launcher(Integer.parseInt(field.getText())));
      t.start();
    }

  }

  private class StopAction implements ActionListener{
    @Override
    public void actionPerformed(ActionEvent arg0) {
      if(t!=null)t.interrupt();
      t = null;
      statusReset();
      single.setEnabled(true);
      concurrent.setEnabled(true);

    }

  }

  ArrayList<WebWorker> workers;
  private class Launcher implements Runnable{
    private int workerLimit;

    public Launcher(int workerLimit) {
      this.workerLimit = workerLimit;
    }

    @Override
    public void run() {
      long start = System.currentTimeMillis();
      runningThreads = 1;
      updateRunningLabel();
      sem = new Semaphore(workerLimit);
      workers = new ArrayList<WebWorker>();
      for (int i = 0; i < model.getRowCount(); i++) {
        try {
          sem.acquire();
        } catch (InterruptedException e) {
          break;
        }
        WebWorker w = new WebWorker((String)model.getValueAt(i, 0), i, WebFrame.this);
        workers.add(w);
        w.start();
      }

      for (int i = 0; i < workers.size(); i++) {
        try {
          workers.get(i).join();
        } catch(InterruptedException e) {

        }
      }

      runningThreads--;
      updateRunningLabel();
      long end = System.currentTimeMillis();
      elapsed.setText("Elapsed: " + (end-start));
      stop.setEnabled(false);
      single.setEnabled(true);
      concurrent.setEnabled(true);
    }

  }
}
