package no.nav.navtest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.Period;
import java.util.Map;
import java.util.SortedMap;


@ActiveProfiles("integration")
@SpringBootTest
class SmallTestForNavYrkesskadeApplicationIntegrationTests {

	@Test
	void testFetchCurrentWeek() {
		SortedMap<String, Map<String,Integer>> weekCounts = SmallTestForNavYrkesskadeApplication.searchInAds(Period.ofDays(0));

		Assertions.assertNotNull(weekCounts); //Should return something
		Assertions.assertEquals(1, weekCounts.size()); //With a period of 0 days back, we should get exactly one week back

		final String CurrentWeek = SmallTestForNavYrkesskadeApplication.toWeekNumber(LocalDate.now());
		Assertions.assertEquals(CurrentWeek, weekCounts.lastKey()); //We should get current week back
	}

}
