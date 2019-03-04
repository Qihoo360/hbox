package net.qihoo.xlearning.AM;

import net.qihoo.xlearning.api.ApplicationContext;
import net.qihoo.xlearning.api.ApplicationMessageProtocol;
import net.qihoo.xlearning.common.Message;
import net.qihoo.xlearning.common.SecurityUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.ipc.ProtocolSignature;
import org.apache.hadoop.ipc.RPC;
import org.apache.hadoop.ipc.Server;
import org.apache.hadoop.net.NetUtils;
import org.apache.hadoop.service.AbstractService;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;

public class ApplicationMessageService extends AbstractService implements
    ApplicationMessageProtocol {

  private static final Log LOG = LogFactory.getLog(ApplicationMessageService.class);

  private final ApplicationContext applicationContext;

  private InetSocketAddress serverAddress;

  public ApplicationMessageService(ApplicationContext applicationContext, Configuration conf) {
    super(ApplicationMessageService.class.getSimpleName());
    this.setConfig(conf);
    this.applicationContext = applicationContext;
  }

  @Override
  public void start() {
    LOG.info("Starting application message server");
    Configuration conf = SecurityUtil.disableSecureRpc(getConfig());
    RPC.Builder builder = new RPC.Builder(conf);
    builder.setProtocol(ApplicationMessageProtocol.class);
    builder.setInstance(this);
    builder.setBindAddress("0.0.0.0");
    builder.setPort(0);
    Server server;
    try {
      server = builder.build();
    } catch (Exception e) {
      LOG.error("Error starting message server!", e);
      e.printStackTrace();
      return;
    }
    server.start();

    serverAddress = NetUtils.getConnectAddress(server);
    LOG.info("Started application message server at " + serverAddress);
  }

  @Override
  public Message[] fetchApplicationMessages() {
    int defaultMaxBatch = 100;
    return fetchApplicationMessages(defaultMaxBatch);
  }

  @Override
  public Message[] fetchApplicationMessages(int maxBatch) {
    BlockingQueue<Message> msgs = applicationContext.getMessageQueue();
    ArrayList<Message> result = new ArrayList<>();
    int count = 0;
    while (count < maxBatch) {
      Message line = msgs.poll();
      if (null == line) {
        break;
      }
      result.add(line);
      count++;
    }

    if (result.size() == 0) {
      return null;
    }
    Message[] resultArray = new Message[result.size()];
    result.toArray(resultArray);
    return resultArray;
  }

  public InetSocketAddress getServerAddress() {
    return serverAddress;
  }

  @Override
  public long getProtocolVersion(String protocol, long clientVersion) throws IOException {
    return ApplicationMessageProtocol.versionID;
  }

  @Override
  public ProtocolSignature getProtocolSignature(String protocol,
                                                long clientVersion, int clientMethodsHash) throws IOException {
    return ProtocolSignature.getProtocolSignature(this, protocol,
        clientVersion, clientMethodsHash);
  }

}
