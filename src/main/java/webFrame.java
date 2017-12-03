package main.java;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;


/**
 * WebFrame Class
 */
public class WebFrame extends JFrame {

    private DefaultTableModel model;
    private JTable table;
    private JPanel panel;
    private JButton singleBtn, concurrentBtn, stopBtn;
    private JTextField textFld;
    private JLabel runningLbl, completedLbl, elapsedLbl;
    private JProgressBar progressBar;
    private Semaphore sem;
    private Thread t;
    private int completedWorks, runningThreads;
    private ArrayList<WebWorker> workers;

    /**
     * Constructor that sets Jframe and components
     * @param  filename [Name of file]
     * @return          [WebFrame object]
     */
    public WebFrame(String filename){

        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setLayout(new BoxLayout(this.getContentPane(), BoxLayout.Y_AXIS));

        panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        model = new DefaultTableModel(new String[] { "url", "status"}, 0);

        table = new JTable(model);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        JScrollPane scrollpane = new JScrollPane(table);
        scrollpane.setPreferredSize(new Dimension(600,300));
        panel.add(scrollpane);

        this.readFile(filename);

        singleBtn = new JButton("Single Thread Fetch");
        singleBtn.addActionListener(new SingleThread());

        concurrentBtn = new JButton("Concurrent Fetch");
        concurrentBtn.addActionListener(new Concurrent());

        stopBtn = new JButton("Stop");
        stopBtn.addActionListener(new StopAction());
        stopBtn.setEnabled(false);

        textFld = new JTextField();
        textFld.setMaximumSize(new Dimension(50,JTextField.HEIGHT));

        runningLbl = new JLabel("Running: 0");
        completedLbl = new JLabel("Completed: 0");
        elapsedLbl = new JLabel("Elapsed: 0");
        progressBar = new JProgressBar();


        panel.add(Box.createRigidArea(new Dimension(0, 5)));
        panel.add(singleBtn);
        panel.add(Box.createRigidArea(new Dimension(0, 5)));
        panel.add(concurrentBtn);
        panel.add(Box.createRigidArea(new Dimension(0, 5)));
        panel.add(textFld);
        panel.add(Box.createRigidArea(new Dimension(0, 5)));
        panel.add(runningLbl);
        panel.add(Box.createRigidArea(new Dimension(0, 5)));
        panel.add(completedLbl);
        panel.add(Box.createRigidArea(new Dimension(0, 5)));
        panel.add(elapsedLbl);
        panel.add(Box.createRigidArea(new Dimension(0, 5)));
        panel.add(progressBar);
        panel.add(Box.createRigidArea(new Dimension(0, 5)));
        panel.add(stopBtn);

        this.add(panel);
        this.pack();
        this.setVisible(true);
    }

    /**
     * Main Method
     * @param args [None]
     */
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.out.println(e);
        }

        WebFrame frame = new WebFrame("link.txt");

    }

    /**
     * Increase running thread count
     */
    public void increaseThreads(){
        updateRunningLabel(runningThreads++);
    }

    /**
     * Derecreases running thread count
     */
    public synchronized void decreaseThreads(){
        updateRunningLabel(runningThreads--);
        sem.release();
    }

    /**
     * Increases the completed text component
     */
    private void increaseCompleted(){
        completedWorks++;
        completedLbl.setText("Completed: " + completedWorks);
    }

    /**
     * Updates the running thread label
     * @param threadsRunning [number of running threads]
     */
    private void updateRunningLabel(int threadsRunning){
        runningLbl.setText("Running: " + threadsRunning);
    }

    /**
     * Updates table
     * @param rowNum [number of rows]
     * @param str    [name of table component]
     */
    public void updateTable(int rowNum,String str){
        increaseCompleted();
        progressBar.setValue(completedWorks);
        model.setValueAt(str, rowNum, 1);
    }

    /**
     * Read file helper method
     * @param filename [name of file to be read]
     */
    private void readFile(String filename) {
        try {
            BufferedReader buff = new BufferedReader(new FileReader(filename));
            try {
                String line;
                while((line = buff.readLine()) != null){
                    model.addRow(new String[]{line, ""});
                }
            } catch (Exception e) {
                System.out.println(e);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Resets texts and values
     */
    private void statusReset(){
        completedWorks = 0;
        progressBar.setValue(0);
        runningLbl.setText("Running: 0");
        completedLbl.setText("Completed: 0");
        elapsedLbl.setText("Elapsed: 0");
        progressBar.setValue(0);
    }

    /**
     * Start button implementation
     */
    private void fetchButtonClicked(){
        statusReset();
        stopBtn.setEnabled(true);
        singleBtn.setEnabled(false);
        concurrentBtn.setEnabled(false);
        progressBar.setMaximum(model.getRowCount());
    }

    /**
     * Single thread implementation
     */
    private class SingleThread implements ActionListener{
        @Override
        public void actionPerformed(ActionEvent e) {
            fetchButtonClicked();
            t = new Thread(new Launcher(1));
            t.start();
        }
    }

    /**
     * Concurrent thread implementation
     */
    private class Concurrent implements ActionListener{
        @Override
        public void actionPerformed(ActionEvent e) {
            fetchButtonClicked();
            t = new Thread(new Launcher(Integer.parseInt(textFld.getText())));
            t.start();
        }
    }

    /**
     * Stop button implementation
     */
    private class StopAction implements ActionListener{
        @Override
        public void actionPerformed(ActionEvent arg0) {
            if(t != null)
                t.interrupt();
            t = null;
            statusReset();
            singleBtn.setEnabled(true);
            concurrentBtn.setEnabled(true);
        }
    }

    /**
     * Private inner class Launcher
     */
    private class Launcher implements Runnable{
        private int workerLimit;

        /**
         * Constructor
         * @param  workerLimit [number of worker to be used]
         * @return             [Launcher Object]
         */
        public Launcher(int workerLimit) {
            this.workerLimit = workerLimit;
        }

        @Override
        public void run() {
            long start = System.currentTimeMillis();
            runningThreads = 1;
            updateRunningLabel(runningThreads);
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

            updateRunningLabel(runningThreads--);
            long end = System.currentTimeMillis();
            elapsedLbl.setText("Elapsed: " + (end-start));
            stopBtn.setEnabled(false);
            singleBtn.setEnabled(true);
            concurrentBtn.setEnabled(true);
        }
    }
}
