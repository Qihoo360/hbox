package net.qihoo.hbox.jobhistory;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import net.qihoo.hbox.conf.HboxConfiguration;
import org.apache.hadoop.classification.InterfaceAudience.Private;
import org.apache.hadoop.classification.InterfaceStability.Evolving;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.http.HttpConfig;
import org.apache.hadoop.mapreduce.JobID;
import org.apache.hadoop.mapreduce.TypeConverter;
import org.apache.hadoop.net.NetUtils;
import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.conf.YarnConfiguration;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Iterator;

import static org.apache.hadoop.http.HttpConfig.Policy;

@Private
@Evolving
public class HboxWebAppUtil {
    private static final Splitter ADDR_SPLITTER = Splitter.on(':').trimResults();
    private static final Joiner JOINER = Joiner.on("");

    private static Policy httpPolicyInYarn;
    private static Policy httpPolicyInJHS;

    public static void initialize(Configuration conf) {
        setHttpPolicyInYARN(conf.get(
                YarnConfiguration.YARN_HTTP_POLICY_KEY,
                YarnConfiguration.YARN_HTTP_POLICY_DEFAULT));
        setHttpPolicyInJHS(conf.get(HboxConfiguration.HBOX_HS_HTTP_POLICY,
                HboxConfiguration.DEFAULT_HBOX_HS_HTTP_POLICY));
    }

    private static void setHttpPolicyInJHS(String policy) {
        HboxWebAppUtil.httpPolicyInJHS = Policy.fromString(policy);
    }

    private static void setHttpPolicyInYARN(String policy) {
        HboxWebAppUtil.httpPolicyInYarn = Policy.fromString(policy);
    }

    public static Policy getJHSHttpPolicy() {
        return HboxWebAppUtil.httpPolicyInJHS;
    }

    public static Policy getYARNHttpPolicy() {
        return HboxWebAppUtil.httpPolicyInYarn;
    }

    public static String getYARNWebappScheme() {
        return httpPolicyInYarn == HttpConfig.Policy.HTTPS_ONLY ? "https://"
                : "http://";
    }

    public static String getJHSWebappScheme() {
        return httpPolicyInJHS == HttpConfig.Policy.HTTPS_ONLY ? "https://"
                : "http://";
    }

    public static void setJHSWebappURLWithoutScheme(Configuration conf,
                                                    String hostAddress) {
        if (httpPolicyInJHS == Policy.HTTPS_ONLY) {
            conf.set(HboxConfiguration.HBOX_HISTORY_WEBAPP_HTTPS_ADDRESS, hostAddress);
        } else {
            conf.set(HboxConfiguration.HBOX_HISTORY_WEBAPP_ADDRESS, hostAddress);
        }
    }

    public static String getJHSWebappURLWithoutScheme(Configuration conf) {
        if (httpPolicyInJHS == Policy.HTTPS_ONLY) {
            return conf.get(HboxConfiguration.HBOX_HISTORY_WEBAPP_HTTPS_ADDRESS,
                    HboxConfiguration.DEFAULT_HBOX_HISTORY_WEBAPP_HTTPS_ADDRESS);
        } else {
            return conf.get(HboxConfiguration.HBOX_HISTORY_WEBAPP_ADDRESS,
                    HboxConfiguration.DEFAULT_HBOX_HISTORY_WEBAPP_ADDRESS);
        }
    }

    public static String getJHSWebappURLWithScheme(Configuration conf) {
        return getJHSWebappScheme() + getJHSWebappURLWithoutScheme(conf);
    }

    public static InetSocketAddress getJHSWebBindAddress(Configuration conf) {
        if (httpPolicyInJHS == Policy.HTTPS_ONLY) {
            return conf.getSocketAddr(
                    HboxConfiguration.HBOX_HISTORY_BIND_HOST,
                    HboxConfiguration.HBOX_HISTORY_WEBAPP_HTTPS_ADDRESS,
                    conf.get(HboxConfiguration.HBOX_HISTORY_WEBAPP_HTTPS_ADDRESS, HboxConfiguration.DEFAULT_HBOX_HISTORY_WEBAPP_HTTPS_ADDRESS),
                    conf.getInt(HboxConfiguration.HBOX_HISTORY_WEBAPP_HTTPS_PORT, HboxConfiguration.DEFAULT_HBOX_HISTORY_WEBAPP_HTTPS_PORT));
        } else {
            return conf.getSocketAddr(
                    HboxConfiguration.HBOX_HISTORY_BIND_HOST,
                    HboxConfiguration.HBOX_HISTORY_WEBAPP_ADDRESS,
                    conf.get(HboxConfiguration.HBOX_HISTORY_WEBAPP_ADDRESS, HboxConfiguration.DEFAULT_HBOX_HISTORY_WEBAPP_ADDRESS),
                    conf.getInt(HboxConfiguration.HBOX_HISTORY_WEBAPP_PORT, HboxConfiguration.DEFAULT_HBOX_HISTORY_WEBAPP_PORT));
        }
    }

    public static String getApplicationWebURLOnJHSWithoutScheme(Configuration conf,
                                                                ApplicationId appId)
            throws UnknownHostException {
        //construct the history url for job
        String addr = getJHSWebappURLWithoutScheme(conf);
        Iterator<String> it = ADDR_SPLITTER.split(addr).iterator();
        it.next(); // ignore the bind host
        String port = it.next();
        // Use hs address to figure out the host for webapp
        addr = conf.get(HboxConfiguration.HBOX_HISTORY_ADDRESS,
                HboxConfiguration.DEFAULT_HBOX_HISTORY_ADDRESS);
        String host = ADDR_SPLITTER.split(addr).iterator().next();
        String hsAddress = JOINER.join(host, ":", port);
        InetSocketAddress address = NetUtils.createSocketAddr(
                hsAddress, getDefaultJHSWebappPort(),
                getDefaultJHSWebappURLWithoutScheme());
        StringBuffer sb = new StringBuffer();
        if (address.getAddress().isAnyLocalAddress() ||
                address.getAddress().isLoopbackAddress()) {
            sb.append(InetAddress.getLocalHost().getCanonicalHostName());
        } else {
            sb.append(address.getHostName());
        }
        sb.append(":").append(address.getPort());
        sb.append("/jobhistory/job/");
        JobID jobId = TypeConverter.fromYarn(appId);
        sb.append(jobId.toString());
        return sb.toString();
    }

    public static String getApplicationWebURLOnJHSWithScheme(Configuration conf,
                                                             ApplicationId appId) throws UnknownHostException {
        return getJHSWebappScheme()
                + getApplicationWebURLOnJHSWithoutScheme(conf, appId);
    }

    private static int getDefaultJHSWebappPort() {
        return httpPolicyInJHS == Policy.HTTPS_ONLY ?
                HboxConfiguration.DEFAULT_HBOX_HISTORY_WEBAPP_HTTPS_PORT :
                HboxConfiguration.DEFAULT_HBOX_HISTORY_WEBAPP_PORT;
    }

    private static String getDefaultJHSWebappURLWithoutScheme() {
        return httpPolicyInJHS == Policy.HTTPS_ONLY ?
                HboxConfiguration.DEFAULT_HBOX_HISTORY_WEBAPP_HTTPS_ADDRESS :
                HboxConfiguration.DEFAULT_HBOX_HISTORY_WEBAPP_ADDRESS;
    }

    public static String getAMWebappScheme(Configuration conf) {
        return "http://";
    }
}