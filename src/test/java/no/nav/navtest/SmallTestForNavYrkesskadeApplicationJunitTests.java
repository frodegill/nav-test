package no.nav.navtest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;


@ActiveProfiles("junit")
@SpringBootTest
class SmallTestForNavYrkesskadeApplicationJunitTests {

	@Test
	void testWeeknumberHappyday() {
		Assertions.assertEquals("2021-35", SmallTestForNavYrkesskadeApplication.toWeekNumber(LocalDate.of(2021, 8, 31)));
	}

	@Test
	void testWeek53InJanuary() {
		Assertions.assertEquals("2020-53", SmallTestForNavYrkesskadeApplication.toWeekNumber(LocalDate.of(2021, 1, 1)));
	}

	@Test
	void testWeek1InDecember() {
		Assertions.assertEquals("2020-01", SmallTestForNavYrkesskadeApplication.toWeekNumber(LocalDate.of(2019, 12, 31)));
	}

	@Test
	void testNorwegianLocale() {
		Assertions.assertEquals("no_NO", SmallTestForNavYrkesskadeApplication.getNorwegianLocale().toString());
	}

}
