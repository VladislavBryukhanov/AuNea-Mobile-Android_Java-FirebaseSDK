import java.net.InetAddress;

public class ClientToClient {

    private InetAddress firstUserIP;
    private int firstUserPort;
    private InetAddress secondUserIP;
    private int secondUserPort;

    ClientToClient(InetAddress firstUserIP, int firstUserPort) {
        this.firstUserIP = firstUserIP;
        this.firstUserPort = firstUserPort;
    }

    public InetAddress getFirstUserIP() {
        return firstUserIP;
    }

    public void setFirstUserIP(InetAddress firstUserIP) {
        this.firstUserIP = firstUserIP;
    }

    public int getFirstUserPort() {
        return firstUserPort;
    }

    public void setFirstUserPort(int firstUserPort) {
        this.firstUserPort = firstUserPort;
    }

    public InetAddress getSecondUserIP() {
        return secondUserIP;
    }

    public void setSecondUserIP(InetAddress secondUserIP) {
        this.secondUserIP = secondUserIP;
    }

    public int getSecondUserPort() {
        return secondUserPort;
    }

    public void setSecondUserPort(int secondUserPort) {
        this.secondUserPort = secondUserPort;
    }
}
