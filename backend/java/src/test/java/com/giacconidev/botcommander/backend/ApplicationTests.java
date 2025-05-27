package com.giacconidev.botcommander.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class ApplicationTests {

	@Test
	void contextLoads() {
	}
	
	@Test
	void testSimpleCalculation() {
		assertEquals(2, 1 + 1);
		assertEquals(5, 2 + 3);
		assertNotEquals(3, 1 + 1);
	}
}
