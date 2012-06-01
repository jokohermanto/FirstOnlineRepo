package com.archfashion.scraper;

import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.archfashion.scraper.NoireLuxeScraper;
import com.archfashion.scrapers.model.Product;
import com.archfashion.scrapers.validation.ProductValidator;
import com.archfashion.util.Json;

public class NoireLuxeScraperTest {

	Logger log = LoggerFactory.getLogger(this.getClass());
	
	@Test
	public void testScrape() throws Exception {
		List<Product> products = new NoireLuxeScraper().scrape(false);
		log.trace(Json.outPretty(products));
		ProductValidator.validate(products);
	}

}
