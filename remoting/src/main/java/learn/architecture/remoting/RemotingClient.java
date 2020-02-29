package learn.architecture.remoting;

import learn.architecture.remoting.protocol.RemotingCommand;

public interface RemotingClient extends RemotingService {

    void sendMsgSync(String addr, RemotingCommand command, long timeoutMs);
}
