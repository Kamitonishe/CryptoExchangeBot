package binance;

import binance.ways.TradeWay;
import binance.ways.TradeWayPair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.DoubleAdder;

public class Main extends JFrame {
    private static Logger profitLog = LoggerFactory.getLogger("profit");
    private JButton start, finish;
    private JLabel profitArea;

    private static DoubleAdder profit = new DoubleAdder();


    private List<TradeWayPair> tradeWays;

    public Main() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        profitArea = new JLabel("0");
        profitArea.setFont(new Font("Monospaced", Font.BOLD, 20));
        getContentPane().add(profitArea, BorderLayout.NORTH);

        start = new JButton("START");
        start.addActionListener(new StartListener());
        getContentPane().add(start, BorderLayout.WEST);

        finish = new JButton("FINISH");
        finish.addActionListener(new FinishListener());
        getContentPane().add(finish, BorderLayout.EAST);

        setPreferredSize(new Dimension(300, 100));
        setSize(new Dimension(300, 100));
        setVisible(true);

        tradeWays = new ArrayList<>();
        tradeWays.add(new TradeWayPair("IOTA", "BNB", "ETH"));
        //tradeWays.add(new TradeWayPair("WTC", "BNB", "ETH"));
        tradeWays.add(new TradeWayPair("CMT", "BNB", "ETH"));
        tradeWays.add(new TradeWayPair("STEEM", "BNB", "ETH"));
        tradeWays.add(new TradeWayPair("NULS", "BNB", "ETH"));

        new ProfitUpdater().start();
    }


    private class StartListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            for (TradeWayPair pair : tradeWays)
                pair.start();
            profitLog.info("TRADING STARTED");
            start.setVisible(false);
        }
    }

    private class FinishListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            for (TradeWayPair pair : tradeWays)
                pair.finish();
            profitLog.info("TRADING FINISHED");
            finish.setVisible(false);
        }
    }

    public static void main(String[] args) {
        Main main = new Main();
        main.setLocationRelativeTo(null);
    }

    private class ProfitUpdater extends Thread {
        @Override
        public void run() {
            while (true) {
                try {
                    sleep(15000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                profitArea.setText(profit.sum() + "");
                JPanel workingWays = new JPanel();
                workingWays.setLayout(new BoxLayout(workingWays, BoxLayout.Y_AXIS));
                workingWays.add(new JLabel("Работающие пути:"));
                for (TradeWayPair pair : tradeWays)
                    for (TradeWay way : pair.getTradeWays())
                        if (way.isPaused())
                            workingWays.add(new JLabel(way.getWayName() + ", Rub profit: " + way.getRubProfit()));
                getContentPane().add(workingWays, BorderLayout.SOUTH);
                getContentPane().repaint();
                getContentPane().revalidate();
            }
        }
    }

    public static void addProfit(double profit) {
        Main.profit.add(profit);
    }
}
