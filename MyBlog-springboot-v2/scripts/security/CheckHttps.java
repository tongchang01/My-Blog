import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLHandshakeException;
import java.net.URI;
import java.security.cert.CertPathValidatorException;

/** 手工 TLS 冒烟：不携带凭据、不读取响应正文，不把超时/DNS 错误当作证书被拒绝。 */
public class CheckHttps {
    public static void main(String[] args) throws Exception {
        if (args.length != 2 || !(args[0].equals("accept") || args[0].equals("reject"))) {
            throw new IllegalArgumentException("Usage: CheckHttps <accept|reject> <https-url>");
        }
        URI uri = URI.create(args[1]);
        if (!"https".equals(uri.getScheme()) || uri.getUserInfo() != null || uri.getQuery() != null) {
            throw new IllegalArgumentException("Only credential-free HTTPS URLs are allowed");
        }
        HttpsURLConnection connection = (HttpsURLConnection) uri.toURL().openConnection();
        connection.setConnectTimeout(10_000);
        connection.setReadTimeout(10_000);
        connection.setInstanceFollowRedirects(false);
        try {
            connection.connect();
            if (args[0].equals("reject")) {
                throw new IllegalStateException("Unexpectedly trusted: " + uri.getHost());
            }
            System.out.println("TLS accepted: " + uri.getHost());
        } catch (SSLHandshakeException failure) {
            boolean certificateFailure = false;
            for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
                if (cause instanceof CertPathValidatorException
                        || cause.getClass().getName().equals("sun.security.provider.certpath.SunCertPathBuilderException")) {
                    certificateFailure = true;
                }
            }
            if (!args[0].equals("reject") || !certificateFailure) {
                throw failure;
            }
            System.out.println("TLS certificate rejected: " + uri.getHost());
        } finally {
            connection.disconnect();
        }
    }
}
