import com.twitter.finagle.Http;
import com.twitter.finagle.InetResolver;
import com.twitter.finagle.Resolver;
import com.twitter.finagle.Service;
import com.twitter.finagle.http.Request;
import com.twitter.finagle.http.Response;
import com.twitter.finagle.stats.StatsReceiver;
import com.twitter.util.*;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


public class FinagleClientApp {
    public static final Timer finagleTimer = new JavaTimer(true);

    public static void main(String[] args) {
        Long connectionTimeoutMillis = 3000L;
        Long reqTimeoutMillis = 3000L;
        Integer hostConnectionLimit  = 3000;
        String label = "google";
        //StatsReceiver statsReceiver = null;
        String host = "google.com";


        FuturePool futurePool = FuturePool$.MODULE$.apply(Executors.newFixedThreadPool(100));
        Resolver resolver = InetResolver.apply(futurePool);
        Http.Client client = Http.client()
                .withDecompression(true)
                .withHttpStats()
                //.withStatsReceiver(statsReceiver)
                .withSessionQualifier().noFailFast()
                .withSessionQualifier().noFailureAccrual()
                .withDecompression(true)
                .withHttp2()
                .withAdmissionControl().noNackAdmissionControl()
                .withLabel(label)
                .withSession().acquisitionTimeout(Duration.fromTimeUnit(connectionTimeoutMillis, TimeUnit.MILLISECONDS))
                .withTransport().connectTimeout(Duration.fromTimeUnit(connectionTimeoutMillis, TimeUnit.MILLISECONDS))
                .withRequestTimeout(Duration.fromTimeUnit(reqTimeoutMillis, TimeUnit.MILLISECONDS))
                .withSessionPool().maxSize(hostConnectionLimit);

        Service<Request, Response> service = client.newService("boundedpoolinet!" + host);

        //Future<Response> resp = service.apply(req).within(Duration.apply(reqTimeoutMillis, TimeUnit.MILLISECONDS), finagleTimer);

    }
}
