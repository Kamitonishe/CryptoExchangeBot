public class FindingWays {

    public static void main(String[] args) {
        TradeWayThread rcn = new TradeWayThread("RCN");
        rcn.start();
        TradeWayThread knc = new TradeWayThread("BAT");
        knc.start();
        TradeWayThread ltc = new TradeWayThread("DLT");
        ltc.start();
        TradeWayThread neo = new TradeWayThread("TRIG");
        neo.start();
        TradeWayThread yoyo = new TradeWayThread("YOYO");
        yoyo.start();
        TradeWayThread snm = new TradeWayThread("GTO");
        snm.start();
    }

}
