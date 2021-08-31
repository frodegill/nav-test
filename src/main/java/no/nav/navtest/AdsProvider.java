package no.nav.navtest;

import no.nav.navtest.dto.Ads;
import no.nav.navtest.dto.Content;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


@Component
public class AdsProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(AdsProvider.class);

    public static final String NAV_SERVER = "https://arbeidsplassen.nav.no";
    public static final String ADS_ENDPOINT = "/public-feed/api/v1/ads";
    public static final String PUBLIC_AUTHENTICATION_TOKEN = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJwdWJsaWMudG9rZW4udjFAbmF2Lm5vIiwiYXVkIjoiZmVlZC1hcGktdjEiLCJpc3MiOiJuYXYubm8iLCJpYXQiOjE1NTc0NzM0MjJ9.jNGlLUF9HxoHo5JrQNMkweLj_91bgk97ZebLdfx3_UQ";

    public static final Set<String> ADS_KEYWORDS = Set.of("java", "kotlin");

    private final WebClient.Builder webClientBuilder;


    public AdsProvider(final WebClient.Builder webClientBuilder) { //Prepare for mocking
        this.webClientBuilder = webClientBuilder;
    }

    public Map<String,Integer> getKeywordCountForWeek(final LocalDate fromIncluding, final LocalDate toIncluding) {
        Map<String,Integer> result = new HashMap<>();

        final String queryPeriod = "["+fromIncluding.toString()+","+toIncluding.toString()+"]";
        LOGGER.info("Getting ads for period {}", queryPeriod);

        /*
         * I usually use RestTemplate. Trying the new WebClient here just for the fun of it.
         * For some reason it sometimes disconnects prematurely. I have not spent time finding
         * a proper fix for this.
         * Also, API seems to not always return consistent data? Might be WebClient (or my..) fault
         */
        HttpClient httpCLient = HttpClient.create()
                                          .responseTimeout(Duration.ofSeconds(1))
                                          .keepAlive(true);
        WebClient adsClient = webClientBuilder.baseUrl(NAV_SERVER)
                                              .clientConnector(new ReactorClientHttpConnector(httpCLient))
                                              .build();
        Ads adsPage;
        int page = 0;
        do {
            final int queryPage = page; //queryParam object must be final? Small hack to make code compile
            adsPage = adsClient.get()
                               .uri(uriBuilder -> uriBuilder.path(ADS_ENDPOINT)
                                       .queryParam("page", queryPage) // <-- This one insists on being final
                                       .queryParam("published", queryPeriod)
                                       .build())
                               .header(HttpHeaders.AUTHORIZATION, "Bearer "+PUBLIC_AUTHENTICATION_TOKEN) //Public token, not even attempted to hide or secure it
                               .accept(MediaType.APPLICATION_JSON)
                               .retrieve()
                               .bodyToMono(Ads.class)
                               .onErrorResume(e -> Mono.empty()) //TODO: Better handling here
                               .block(); //For now, block. Could be made much smarter. WebClient looks like an interesting replacement for RestTemplate

            if (adsPage!=null && adsPage.getContent()!=null) {
                for (final String keyword : ADS_KEYWORDS) {
                    int count=0;
                    for (int index=0; index<adsPage.getContent().length; index++) {
                        Content adContent = adsPage.getContent()[index];
                        if (adContent!=null &&
                            //Just very simple String.contains. No tokenizing or anything fancy
                            (adContent.getTitle()!=null && adContent.getTitle().toLowerCase(SmallTestForNavYrkesskadeApplication.getNorwegianLocale()).contains(keyword) ||
                             adContent.getJobtitle()!=null && adContent.getJobtitle().toLowerCase(SmallTestForNavYrkesskadeApplication.getNorwegianLocale()).contains(keyword) ||
                             adContent.getDescription()!=null && adContent.getDescription().toLowerCase(SmallTestForNavYrkesskadeApplication.getNorwegianLocale()).contains(keyword)) ) {
                            LOGGER.debug("Found "+keyword+" in " + queryPeriod + " page " + queryPage + " ad " + adContent.getUuid());
                            count++;
                        }
                    }

                    int oldCount = result.getOrDefault(keyword, 0);
                    result.put(keyword, oldCount+count);
                }
            }

            if (adsPage != null) { //This is needed because of too primitive error handling in the GET call above. Only go to next page if we actually got this page
                page++;
            }
        } while (adsPage==null || !adsPage.isLast() && adsPage.getPageNumber()<adsPage.getTotalPages());

        return result;
    }

}
