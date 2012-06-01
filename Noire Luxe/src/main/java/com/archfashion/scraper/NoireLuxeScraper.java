package com.archfashion.scraper;

import static com.archfashion.util.ScraperTextUtils.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.http.client.ClientProtocolException;
import org.jsoup.Jsoup;
import org.jsoup.Connection.Method;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.archfashion.scrapers.BaseProductScraper;
import com.archfashion.scrapers.CacheTime;
import com.archfashion.scrapers.annotations.Scraper;
import com.archfashion.scrapers.model.Product;
import com.archfashion.scrapers.model.ProductVariant;
import com.archfashion.util.ScraperTextUtils;

@Scraper(domain = "NoireLuxe", author = "Joko", store = 256070)
public class NoireLuxeScraper extends BaseProductScraper {
	
	private String BASE_URL = "http://www.noireluxe.com";
	private String BASE_CATALOGUE_URL = "http://www.noireluxe.com/catalogue";
	private String BASE_PRODUCT_LIST_AJAX_URL = "http://www.noireluxe.com/__ajax/mob_catalogue/showProductList";

	public List<Product> doScrape(boolean test) throws Exception {
		List<Product> products = new ArrayList<Product>();
		
		Document doc = http.getDocument(BASE_CATALOGUE_URL, CacheTime.LIST, "UTF-8");		
		int lastPage = Integer.parseInt(StringUtils.substringAfter(doc.select("div.pagesum").first().text(), "of "));
		
		// params
		Map<String, String> params = new HashMap<String, String>();
		params.put("cateId", "-1");
		params.put("cateName", "");		
		params.put("sortExpression", "");
		params.put("subcateId", "-1");
		params.put("subcateName", "");
		params.put("topLv", "");
		
		Document post = null;
		
		try {
			List<String> productIds = new ArrayList<String>();
			for (int i=1; i<=lastPage; i++) {
				params.put("idx", Integer.toString(i));
				
				post = Jsoup.connect(BASE_PRODUCT_LIST_AJAX_URL)
					   .data(params)
					   .method(Method.POST)
					   .post();
				
				products.addAll(getProducts(post, productIds));
				
			}
		} catch (Exception e) {
			e.printStackTrace();
			log.error(e.getMessage());
		}
		
		return products;
	}
	
	private List<Product> getProducts (Document doc, List<String> productIds) throws Exception {
		List<Product> products = new ArrayList<Product>();
		
		if (doc != null) {
			for (Element prodLink : doc.select("div.product-list div.item a")) {
				String prodUrl = BASE_URL + prodLink.attr("href");
				
				Document prodDoc = http.getDocument(prodUrl, CacheTime.PRODUCT, "UTF-8");
				String prodId = prodDoc.select("input[name=hdnId]").val();
				
				if (prodDoc != null && prodId != null && !productIds.contains(prodId)) {
					productIds.add(prodId);
					
					// get the product details
					Product product = getProduct(prodDoc);
					if (product != null) {
						product.setUrl(prodUrl);
						product.setDomainId(prodId);
						products.add(product);
					}
				}
			}
		}
		
		return products;
	}
	
	private Product getProduct(Document doc) throws UnsupportedEncodingException, ClientProtocolException, IOException {
		Product p = new Product();
		
		String catName = getCategoryName(doc.select("div.breadcrumb-link a"));
		p.setCategoryName(catName);		
		p.setFemale(catName.toLowerCase().contains("women"));
		
		p.setName(doc.select("h1").text());
		p.setDescription(ScraperTextUtils.cleanHtml(doc.select("div#tab-1-body").html()));
		
		// prices
		Element prices = doc.select("h2").first();
		if (prices.getElementsByTag("span") != null && prices.getElementsByTag("span").size() == 2) {
			p.setOldPrice(getPrice(prices.select("span.old-price").text()));
			p.setPrice(getPrice(prices.select("span.now-price").text()));
		} else {
			log.info("==> here");
			p.setPrice(getPrice(prices.text()));
		}
		
		if (p.getOldPrice() != null && p.getOldPrice().equals(p.getPrice()))
			p.setOldPrice(null);
		
		// colours
		for (Element colour : doc.select("select#ddlColor option")) {
			if (!colour.val().isEmpty()) {
				p.addColour(colour.text());
			}
		}
		
		// sizes
		for (Element size : doc.select("select#ddlSize option")) {
			if (!size.val().isEmpty()) {
				ProductVariant var = new ProductVariant();
				var.setDomainId(size.val());
				var.setName(size.text());
				
				p.addSize(var);
			}
		}
		
		// photos
		for (Element photo : doc.select("a.cloud-zoom-gallery")) {
			p.addPhoto(BASE_URL + photo.attr("href"));
		}
		
		return p;
	}
	
	private String getCategoryName(Elements breadcrumb) {
		
		if (breadcrumb == null || breadcrumb.size() == 0)
			return null;

		breadcrumb.remove(0); // home
		breadcrumb.remove(breadcrumb.size()-1); // back to list
		
		String catName = "";
		for (Element a : breadcrumb) {
			catName += a.text() + " ";
		}
		
		return catName.trim();
	}

}
