package net.qihoo.hbox.jobhistory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.AccessControlException;
import java.security.PrivilegedExceptionAction;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;

import net.qihoo.hbox.conf.HboxConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.classification.InterfaceAudience.Private;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.CommonConfigurationKeysPublic;
import org.apache.hadoop.http.HttpServer2;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.ipc.Server;
import org.apache.hadoop.mapreduce.JobACL;
import org.apache.hadoop.mapreduce.TypeConverter;
import org.apache.hadoop.mapreduce.v2.api.HSClientProtocol;
import org.apache.hadoop.mapreduce.v2.api.MRDelegationTokenIdentifier;
import org.apache.hadoop.mapreduce.v2.api.protocolrecords.*;
import org.apache.hadoop.mapreduce.v2.api.records.JobId;
import org.apache.hadoop.mapreduce.v2.api.records.TaskAttemptId;
import org.apache.hadoop.mapreduce.v2.api.records.TaskId;
import org.apache.hadoop.mapreduce.v2.api.records.TaskType;
import org.apache.hadoop.mapreduce.v2.app.job.Job;
import org.apache.hadoop.mapreduce.v2.app.job.Task;
import org.apache.hadoop.mapreduce.v2.app.security.authorize.ClientHSPolicyProvider;
import org.apache.hadoop.mapreduce.v2.hs.*;
import org.apache.hadoop.mapreduce.v2.hs.HistoryContext;
import org.apache.hadoop.net.NetUtils;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.security.token.Token;
import org.apache.hadoop.service.AbstractService;
import org.apache.hadoop.yarn.factories.RecordFactory;
import org.apache.hadoop.yarn.factory.providers.RecordFactoryProvider;
import org.apache.hadoop.yarn.ipc.YarnRPC;
import org.apache.hadoop.yarn.util.Records;
import org.apache.hadoop.yarn.webapp.WebApp;
import org.apache.hadoop.yarn.webapp.WebAppException;
import org.apache.hadoop.yarn.webapp.WebApps;

import com.google.common.annotations.VisibleForTesting;
import org.mortbay.jetty.servlet.DefaultServlet;
import org.mortbay.jetty.servlet.FilterHolder;
import org.mortbay.jetty.webapp.WebAppContext;

/**
 * This module is responsible for talking to the
 * JobClient (user facing).
 *
 */
public class HistoryClientService extends AbstractService {

  private static final Log LOG = LogFactory.getLog(HistoryClientService.class);

  private HSClientProtocol protocolHandler;
  private Server server;
  private WebApp webApp;
  private InetSocketAddress bindAddress;
  private HistoryContext history;
  private JHSDelegationTokenSecretManager jhsDTSecretManager;

  public HistoryClientService(HistoryContext history,
                              JHSDelegationTokenSecretManager jhsDTSecretManager) {
    super("HistoryClientService");
    this.history = history;
    this.protocolHandler = new HSClientProtocolHandler();
    this.jhsDTSecretManager = jhsDTSecretManager;
  }

  protected void serviceStart() throws Exception {
    Configuration conf = new HboxConfiguration();
    YarnRPC rpc = YarnRPC.create(conf);
    initializeWebApp(conf);
    InetSocketAddress address = conf.getSocketAddr(
        HboxConfiguration.HBOX_HISTORY_BIND_HOST,
        HboxConfiguration.HBOX_HISTORY_ADDRESS,
        conf.get(HboxConfiguration.HBOX_HISTORY_ADDRESS, HboxConfiguration.DEFAULT_HBOX_HISTORY_ADDRESS),
        conf.getInt(HboxConfiguration.HBOX_HISTORY_PORT, HboxConfiguration.DEFAULT_HBOX_HISTORY_PORT));

    server =
        rpc.getServer(HSClientProtocol.class, protocolHandler, address,
            conf, jhsDTSecretManager,
            conf.getInt(HboxConfiguration.HBOX_HISTORY_CLIENT_THREAD_COUNT,
                HboxConfiguration.DEFAULT_HBOX_HISTORY_CLIENT_THREAD_COUNT));

    // Enable service authorization?
    if (conf.getBoolean(
        CommonConfigurationKeysPublic.HADOOP_SECURITY_AUTHORIZATION,
        false)) {
      server.refreshServiceAcl(conf, new ClientHSPolicyProvider());
    }

    server.start();
    this.bindAddress = conf.updateConnectAddr(HboxConfiguration.HBOX_HISTORY_BIND_HOST,
        HboxConfiguration.HBOX_HISTORY_ADDRESS,
        conf.get(HboxConfiguration.HBOX_HISTORY_ADDRESS, HboxConfiguration.DEFAULT_HBOX_HISTORY_ADDRESS),
        server.getListenerAddress());
    LOG.info("Instantiated HistoryClientService at " + this.bindAddress);

    super.serviceStart();
  }

  @VisibleForTesting
  protected void initializeWebApp(Configuration conf) {
    webApp = new HsWebApp(history);
    InetSocketAddress bindAddress = HboxWebAppUtil.getJHSWebBindAddress(conf);
    // NOTE: there should be a .at(InetSocketAddress)
    WebApps
        .$for("jobhistory", HistoryClientService.class, this, "ws")
        .with(conf)
        .withHttpSpnegoKeytabKey(
            HboxConfiguration.HBOX_WEBAPP_SPNEGO_KEYTAB_FILE_KEY)
        .withHttpSpnegoPrincipalKey(
            HboxConfiguration.HBOX_WEBAPP_SPNEGO_USER_NAME_KEY)
        .at(NetUtils.getHostPortString(bindAddress)).build(webApp);

    HttpServer2 httpServer = webApp.httpServer();

    WebAppContext webAppContext = httpServer.getWebAppContext();
    WebAppContext appWebAppContext = new WebAppContext();
    appWebAppContext.setContextPath("/appResource");
    String appDir = getClass().getClassLoader().getResource("hboxWeb").toString();
    appWebAppContext.setResourceBase(appDir + "/static");
    appWebAppContext.addServlet(DefaultServlet.class, "/*");
    final String[] ALL_URLS = { "/*" };
    FilterHolder[] filterHolders =
        webAppContext.getServletHandler().getFilters();
    for (FilterHolder filterHolder: filterHolders) {
      if (!"guice".equals(filterHolder.getName())) {
        HttpServer2.defineFilter(appWebAppContext, filterHolder.getName(),
            filterHolder.getClassName(), filterHolder.getInitParameters(),
            ALL_URLS);
      }
    }
    httpServer.addContext(appWebAppContext, true);
    try {
      httpServer.start();
      LOG.info("Web app " + webApp.name() + " started at "
          + httpServer.getConnectorAddress(0).getPort());
    } catch (IOException e) {
      throw new WebAppException("Error starting http server", e);
    }

    String connectHost = HboxWebAppUtil.getJHSWebappURLWithoutScheme(conf).split(":")[0];
    HboxWebAppUtil.setJHSWebappURLWithoutScheme(conf,
        connectHost + ":" + webApp.getListenerAddress().getPort());
  }

  @Override
  protected void serviceStop() throws Exception {
    if (server != null) {
      server.stop();
    }
    if (webApp != null) {
      webApp.stop();
    }
    super.serviceStop();
  }

  @Private
  public InetSocketAddress getBindAddress() {
    return this.bindAddress;
  }


  private class HSClientProtocolHandler implements HSClientProtocol {

    private RecordFactory recordFactory = RecordFactoryProvider.getRecordFactory(null);

    public InetSocketAddress getConnectAddress() {
      return getBindAddress();
    }

    private Job verifyAndGetJob(final JobId jobID, boolean exceptionThrow)
        throws IOException {
      UserGroupInformation loginUgi = null;
      Job job = null;
      try {
        loginUgi = UserGroupInformation.getLoginUser();
        job = loginUgi.doAs(new PrivilegedExceptionAction<Job>() {

          @Override
          public Job run() throws Exception {
            Job job = history.getJob(jobID);
            return job;
          }
        });
      } catch (InterruptedException e) {
        throw new IOException(e);
      }

      if (job == null && exceptionThrow) {
        throw new IOException("Unknown Job " + jobID);
      }

      if (job != null) {
        JobACL operation = JobACL.VIEW_JOB;
        checkAccess(job, operation);
      }
      return job;
    }

    @Override
    public GetCountersResponse getCounters(GetCountersRequest request)
        throws IOException {
      JobId jobId = request.getJobId();
      Job job = verifyAndGetJob(jobId, true);
      GetCountersResponse response = recordFactory.newRecordInstance(GetCountersResponse.class);
      response.setCounters(TypeConverter.toYarn(job.getAllCounters()));
      return response;
    }

    @Override
    public GetJobReportResponse getJobReport(GetJobReportRequest request)
        throws IOException {
      JobId jobId = request.getJobId();
      Job job = verifyAndGetJob(jobId, false);
      GetJobReportResponse response = recordFactory.newRecordInstance(GetJobReportResponse.class);
      if (job != null) {
        response.setJobReport(job.getReport());
      } else {
        response.setJobReport(null);
      }
      return response;
    }

    @Override
    public GetTaskAttemptReportResponse getTaskAttemptReport(
        GetTaskAttemptReportRequest request) throws IOException {
      TaskAttemptId taskAttemptId = request.getTaskAttemptId();
      Job job = verifyAndGetJob(taskAttemptId.getTaskId().getJobId(), true);
      GetTaskAttemptReportResponse response = recordFactory.newRecordInstance(GetTaskAttemptReportResponse.class);
      response.setTaskAttemptReport(job.getTask(taskAttemptId.getTaskId()).getAttempt(taskAttemptId).getReport());
      return response;
    }

    @Override
    public GetTaskReportResponse getTaskReport(GetTaskReportRequest request)
        throws IOException {
      TaskId taskId = request.getTaskId();
      Job job = verifyAndGetJob(taskId.getJobId(), true);
      GetTaskReportResponse response = recordFactory.newRecordInstance(GetTaskReportResponse.class);
      response.setTaskReport(job.getTask(taskId).getReport());
      return response;
    }

    @Override
    public GetTaskAttemptCompletionEventsResponse
    getTaskAttemptCompletionEvents(
        GetTaskAttemptCompletionEventsRequest request) throws IOException {
      JobId jobId = request.getJobId();
      int fromEventId = request.getFromEventId();
      int maxEvents = request.getMaxEvents();

      Job job = verifyAndGetJob(jobId, true);
      GetTaskAttemptCompletionEventsResponse response = recordFactory.newRecordInstance(GetTaskAttemptCompletionEventsResponse.class);
      response.addAllCompletionEvents(Arrays.asList(job.getTaskAttemptCompletionEvents(fromEventId, maxEvents)));
      return response;
    }

    @Override
    public KillJobResponse killJob(KillJobRequest request) throws IOException {
      throw new IOException("Invalid operation on completed job");
    }

    @Override
    public KillTaskResponse killTask(KillTaskRequest request)
        throws IOException {
      throw new IOException("Invalid operation on completed job");
    }

    @Override
    public KillTaskAttemptResponse killTaskAttempt(
        KillTaskAttemptRequest request) throws IOException {
      throw new IOException("Invalid operation on completed job");
    }

    @Override
    public GetDiagnosticsResponse getDiagnostics(GetDiagnosticsRequest request)
        throws IOException {
      TaskAttemptId taskAttemptId = request.getTaskAttemptId();

      Job job = verifyAndGetJob(taskAttemptId.getTaskId().getJobId(), true);

      GetDiagnosticsResponse response = recordFactory.newRecordInstance(GetDiagnosticsResponse.class);
      response.addAllDiagnostics(job.getTask(taskAttemptId.getTaskId()).getAttempt(taskAttemptId).getDiagnostics());
      return response;
    }

    @Override
    public FailTaskAttemptResponse failTaskAttempt(
        FailTaskAttemptRequest request) throws IOException {
      throw new IOException("Invalid operation on completed job");
    }

    @Override
    public GetTaskReportsResponse getTaskReports(GetTaskReportsRequest request)
        throws IOException {
      JobId jobId = request.getJobId();
      TaskType taskType = request.getTaskType();

      GetTaskReportsResponse response = recordFactory.newRecordInstance(GetTaskReportsResponse.class);
      Job job = verifyAndGetJob(jobId, true);
      Collection<Task> tasks = job.getTasks(taskType).values();
      for (Task task : tasks) {
        response.addTaskReport(task.getReport());
      }
      return response;
    }

    @Override
    public GetDelegationTokenResponse getDelegationToken(
        GetDelegationTokenRequest request) throws IOException {

      UserGroupInformation ugi = UserGroupInformation.getCurrentUser();

      // Verify that the connection is kerberos authenticated
      if (!isAllowedDelegationTokenOp()) {
        throw new IOException(
            "Delegation Token can be issued only with kerberos authentication");
      }

      GetDelegationTokenResponse response = recordFactory.newRecordInstance(
          GetDelegationTokenResponse.class);

      String user = ugi.getUserName();
      Text owner = new Text(user);
      Text realUser = null;
      if (ugi.getRealUser() != null) {
        realUser = new Text(ugi.getRealUser().getUserName());
      }
      MRDelegationTokenIdentifier tokenIdentifier =
          new MRDelegationTokenIdentifier(owner, new Text(
              request.getRenewer()), realUser);
      Token<MRDelegationTokenIdentifier> realJHSToken =
          new Token<MRDelegationTokenIdentifier>(tokenIdentifier,
              jhsDTSecretManager);
      org.apache.hadoop.yarn.api.records.Token mrDToken =
          org.apache.hadoop.yarn.api.records.Token.newInstance(
              realJHSToken.getIdentifier(), realJHSToken.getKind().toString(),
              realJHSToken.getPassword(), realJHSToken.getService().toString());
      response.setDelegationToken(mrDToken);
      return response;
    }

    @Override
    public UpdateJobMaxRunningResponse updateJobMaxRunning(UpdateJobMaxRunningRequest request) throws IOException {
      return null;
    }

    @Override
    public RenewDelegationTokenResponse renewDelegationToken(
        RenewDelegationTokenRequest request) throws IOException {
      if (!isAllowedDelegationTokenOp()) {
        throw new IOException(
            "Delegation Token can be renewed only with kerberos authentication");
      }

      org.apache.hadoop.yarn.api.records.Token protoToken = request.getDelegationToken();
      Token<MRDelegationTokenIdentifier> token =
          new Token<MRDelegationTokenIdentifier>(
              protoToken.getIdentifier().array(), protoToken.getPassword()
              .array(), new Text(protoToken.getKind()), new Text(
              protoToken.getService()));

      String user = UserGroupInformation.getCurrentUser().getShortUserName();
      long nextExpTime = jhsDTSecretManager.renewToken(token, user);
      RenewDelegationTokenResponse renewResponse = Records
          .newRecord(RenewDelegationTokenResponse.class);
      renewResponse.setNextExpirationTime(nextExpTime);
      return renewResponse;
    }

    @Override
    public CancelDelegationTokenResponse cancelDelegationToken(
        CancelDelegationTokenRequest request) throws IOException {
      if (!isAllowedDelegationTokenOp()) {
        throw new IOException(
            "Delegation Token can be cancelled only with kerberos authentication");
      }

      org.apache.hadoop.yarn.api.records.Token protoToken = request.getDelegationToken();
      Token<MRDelegationTokenIdentifier> token =
          new Token<MRDelegationTokenIdentifier>(
              protoToken.getIdentifier().array(), protoToken.getPassword()
              .array(), new Text(protoToken.getKind()), new Text(
              protoToken.getService()));

      String user = UserGroupInformation.getCurrentUser().getUserName();
      jhsDTSecretManager.cancelToken(token, user);
      return Records.newRecord(CancelDelegationTokenResponse.class);
    }

    private void checkAccess(Job job, JobACL jobOperation)
        throws IOException {

      UserGroupInformation callerUGI;
      callerUGI = UserGroupInformation.getCurrentUser();

      if (!job.checkAccess(callerUGI, jobOperation)) {
        throw new IOException(new AccessControlException("User "
            + callerUGI.getShortUserName() + " cannot perform operation "
            + jobOperation.name() + " on " + job.getID()));
      }
    }

    private boolean isAllowedDelegationTokenOp() throws IOException {
      if (UserGroupInformation.isSecurityEnabled()) {
        return EnumSet.of(UserGroupInformation.AuthenticationMethod.KERBEROS,
            UserGroupInformation.AuthenticationMethod.KERBEROS_SSL,
            UserGroupInformation.AuthenticationMethod.CERTIFICATE)
            .contains(UserGroupInformation.getCurrentUser()
                .getRealAuthenticationMethod());
      } else {
        return true;
      }
    }
  }
}
