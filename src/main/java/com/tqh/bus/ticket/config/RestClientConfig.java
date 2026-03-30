package com.tqh.bus.ticket.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.http.HttpClient;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

@Configuration
public class RestClientConfig {

    private static final String USER_AGENT = "Mozilla/5.0 (iPhone; CPU iPhone OS 18_0_1 like Mac OS X) "
            + "AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148 "
            + "MicroMessenger/8.0.69(0x18004539) NetType/WIFI Language/zh_CN";

    private static final String REFERER = "https://servicewechat.com/wx98d802f3b76e9491/24/page-frame.html";

    @Bean
    public RestClient restClient(TqhProperties properties) {
        return RestClient.builder()
                .requestFactory(createRequestFactory())
                .baseUrl(properties.getBaseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("X-Auth-Token", properties.getAuthToken())
                .defaultHeader(HttpHeaders.USER_AGENT, USER_AGENT)
                .defaultHeader(HttpHeaders.REFERER, REFERER)
                .build();
    }

    /**
     * 创建 HTTP 请求工厂，配置 SSL 上下文。
     *
     * <p>⚠️ 安全说明：</p>
     * <p>此实现使用了自定义的 TrustManager 来绕过 SSL 证书验证。这样做的原因是：</p>
     * <ul>
     *   <li>目标 API 服务器的 SSL 证书配置可能不符合标准（例如使用自签名证书或证书链不完整）</li>
     *   <li>这是为了解决特定环境下的 SSL 握手失败问题，确保应用能够正常调用 API</li>
     * </ul>
     *
     * <p>⚠️ 安全风险：</p>
     * <ul>
     *   <li>此实现会信任所有证书，包括无效、过期或自签名的证书</li>
     *   <li>在生产环境中，这会使应用容易受到中间人攻击（MITM）</li>
     *   <li>不应在处理敏感数据的场景中使用</li>
     * </ul>
     *
     * <p>🔧 改进建议：</p>
     * <ul>
     *   <li>优先方案：联系 API 提供方修复 SSL 证书配置，使用受信任的 CA 签发的证书</li>
     *   <li>替代方案：导入目标服务器的证书到 Java TrustStore，而非禁用验证</li>
     *   <li>临时方案：仅在开发/测试环境使用此配置，生产环境必须使用正确的 SSL 验证</li>
     * </ul>
     */
    private JdkClientHttpRequestFactory createRequestFactory() {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{trustAllManager()}, new SecureRandom());

            HttpClient httpClient = HttpClient.newBuilder()
                    .sslContext(sslContext)
                    .build();

            return new JdkClientHttpRequestFactory(httpClient);
        } catch (Exception e) {
            throw new RuntimeException("SSL 配置失败", e);
        }
    }

    /**
     * 创建一个信任所有证书的 TrustManager。
     *
     * <p>⚠️ 严重安全警告：此实现完全禁用了 SSL 证书验证，仅用于解决临时环境问题。</p>
     * <p>在生产环境中使用此实现将导致严重的安全风险。</p>
     *
     * @return 一个信任所有证书的 TrustManager
     */
    private X509TrustManager trustAllManager() {
        return new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) {
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        };
    }
}
