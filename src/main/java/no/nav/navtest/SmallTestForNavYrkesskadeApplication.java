package no.nav.navtest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.time.temporal.WeekFields;
import java.util.*;


@SpringBootApplication
public class SmallTestForNavYrkesskadeApplication implements CommandLineRunner {
	private static final Logger LOGGER = LoggerFactory.getLogger(SmallTestForNavYrkesskadeApplication.class);

	public static final Period SEARCH_PERIOD = Period.ofMonths(6); //How far back are we searching

	@Autowired
	private Environment environment;


	public static void main(String[] args) {
		SpringApplication.run(SmallTestForNavYrkesskadeApplication.class, args);
	}

	public static SortedMap<String, Map<String,Integer>> searchInAds(final Period period) {
		SortedMap<String, Map<String,Integer>> weekCounts = new TreeMap<>();
		AdsProvider adsProvider = new AdsProvider(WebClient.builder());

		LocalDate localNow = LocalDate.now();
		LocalDate startOfWeek = localNow.minus(localNow.getDayOfWeek().getValue()-1, ChronoUnit.DAYS);
		while (startOfWeek.plusWeeks(1).isAfter(localNow.minus(period))) { //Loop until given week is outside (/before) given period
			final String weekNumber = toWeekNumber(startOfWeek);
			if (!weekCounts.containsKey(weekNumber)) { //This should always be true. Could be useful if multiple runs should be supported
				weekCounts.put(weekNumber, new HashMap<>());
			}

			Map<String,Integer> weekCount = adsProvider.getKeywordCountForWeek(startOfWeek, startOfWeek.plusDays(6));
			for (final Map.Entry<String, Integer> entry : weekCount.entrySet()) { //Right now, this could just be set. Iterate to also support multiple runs
				int oldCount = weekCounts.get(weekNumber).getOrDefault(entry.getKey(), 0);
				weekCounts.get(weekNumber).put(entry.getKey(), oldCount + entry.getValue());
			}

			startOfWeek = startOfWeek.minus(1, ChronoUnit.WEEKS);
		}

		return weekCounts;
	}

	@Override
	public void run(String... args) {
		for (String profileName : environment.getActiveProfiles()) { //Tests should bail out before default search. They know what to do themselves.
			if ("junit".equals(profileName) || "integration".equals(profileName)) {
				return;
			}
		}

		//Do the actual search. API seems to not be able to filter on keywords, so this dumps the entire db for the given period.
		//There is a limit of 5000 ads returned. If a week has more, some will not be counted
		SortedMap<String, Map<String,Integer>> weekCounts = searchInAds(SEARCH_PERIOD);

		//"Det er tilstrekkelig at output vises som prettyfied json på kommandolinjen."  -Anders Eggum
		ObjectMapper objectMapper = new ObjectMapper();
		try {
			LOGGER.info("Result:\n\n" + objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(weekCounts));
		} catch(JsonProcessingException e) {
			LOGGER.error("JSON serialization failed", e);
		}
	}

	static public Locale getNorwegianLocale() {
		return Locale.forLanguageTag("no-NO");
	}

	public static String toWeekNumber(final LocalDate date) {
		WeekFields weekFields = WeekFields.of(getNorwegianLocale());
		int weekNumber = date.get(weekFields.weekOfWeekBasedYear()); // https://stackoverflow.com/questions/26012434/get-week-number-of-localdate-java-8/26013129
		int year = date.getYear();
		if (date.getMonthValue()==1 && weekNumber>50) { //could safely use any value between 2 and 51. 1, 52 and 53 may cross new year
			year--;
		} else if (date.getMonthValue()==12 && weekNumber<50) {
			year++;
		}

		return String.format("%04d-%02d", year, weekNumber);
	}

}
