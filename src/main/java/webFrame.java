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


public class WebFrame extends JFrame {
    
    private DefaultTableModel model;
    private JTable table;
    private JPanel panel;
    private JButton single, concurrent, stop;
    private JTextField field;
    private JLabel running, completed, elapsed;
    private JProgressBar progress;
    private Semaphore sem;
    private Thread t;
    private int completedWorks, runningThreads;
    
    public WebFrame(String filename){
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
        updateRunningLabel(runningThreads++);
    }
    
    public synchronized void decreaseThreads(){
        updateRunningLabel(runningThreads--);
        sem.release();
    }
    
    private void increaseCompledted(){
        completedWorks++;
        completed.setText("Completed: " + completedWorks);
    }
    
    private void updateRunningLabel(int threadsRunning){
        running.setText("Running: " + threadsRunning);
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
    
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.out.println(e);
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
            if(t != null)
                t.interrupt();
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
            elapsed.setText("Elapsed: " + (end-start));
            stop.setEnabled(false);
            single.setEnabled(true);
            concurrent.setEnabled(true);
        }  
    }
}
