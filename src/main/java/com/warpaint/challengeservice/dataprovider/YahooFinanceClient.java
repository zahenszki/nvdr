package com.warpaint.challengeservice.dataprovider;

import com.google.common.collect.Lists;
import com.warpaint.challengeservice.model.Pricing;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.*;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.Collections.emptyList;

/**
 * Stable client libraries (e.g. https://financequotes-api.com/) are broken since Yahoo discontinued
 * support for the public API.
 * The below code works for now -- if not, please reach out to us.
 */
@Service
@Slf4j
public class YahooFinanceClient {

	private static final String PRICE_FORMAT_URL = "https://query1.finance.yahoo.com/v7/finance/download/%s?period1=%d&period2=%d&interval=1d&events=history&interval=1d&crumb=%s";
	private static final String DIVIDEND_FORMAT_URL = "https://query1.finance.yahoo.com/v7/finance/download/%s?period1=%d&period2=%d&interval=1d&events=div&interval=1d&crumb=%s";
    private static final String DATE = "Date";
    private static final String CLOSE = "Close" ;
    private static final String DELIMITER = ",";

    @Setter
    private YahooFinanceSession session;
	private HttpHandler httpHandler;

	public YahooFinanceClient(HttpHandler httpHandler) {
	    this.httpHandler = httpHandler;

		this.session = new YahooFinanceSession(httpHandler);
	}

	private String constructURL(String formatURL, String ticker, LocalDate from, LocalDate to) {
		long fromEpoch = from.atStartOfDay(ZoneOffset.UTC).toEpochSecond();
		long toEpoch = to.atStartOfDay(ZoneOffset.UTC).toEpochSecond();
		String crumb = (session.getCrumb() != null) ? HttpHandler.urlEncodeString(session.getCrumb()) : "";
		String encodedTicker = HttpHandler.urlEncodeString(ticker);
		return String.format(formatURL, encodedTicker, fromEpoch, toEpoch, crumb);
	}

    private HttpEntity fetchURL(String url, String symbol, LocalDate fromDate, LocalDate toDate) {

        HttpGet request = new HttpGet(url);
        HttpResponse response = httpHandler.fetchResponse(request);
        HttpStatus statusCode = HttpStatus.valueOf(response.getStatusLine().getStatusCode());
        if (statusCode == HttpStatus.UNAUTHORIZED) {
            log.debug("Unauthorized response using crumb and cookies:");
            log.debug("crumb: {} cookies: {}", session.getCrumb(), httpHandler.getCookieStore().getCookies());
            session.invalidate();
            session.acquireCrumbWithTicker(symbol);
            log.info("Retrying connection after unauthorized response");

            request.setURI(URI.create(constructURL(PRICE_FORMAT_URL, symbol, fromDate, toDate))); // Acquire new crumb
            request.reset();
            EntityUtils.consumeQuietly(response.getEntity());
            response = httpHandler.fetchResponse(request);
        } else if (statusCode == HttpStatus.NOT_FOUND) {
            EntityUtils.consumeQuietly(response.getEntity());
            return null;
        }
        return response.getEntity();
    }

    private OptionalInt findTokenPos(final String token, final  String[] line)
    {
        for (int i=0; i<line.length; i++)
        {
           if(line[i].equals(token))
                return OptionalInt.of(i);
        }
        return OptionalInt.empty();
    }

	public List<Pricing> fetchPriceData(String symbol, LocalDate fromDate, LocalDate toDate) {
		log.info("Acquiring price data for {} from {} to {}", symbol, fromDate, toDate);
		session.acquireCrumbWithTicker(symbol);

		String priceURL = constructURL(PRICE_FORMAT_URL, symbol, fromDate, toDate);
		HttpEntity entity = fetchURL(priceURL, symbol, fromDate, toDate);
		if (entity == null) {
            log.warn("No price data available for {} from {} to {}", symbol, fromDate, toDate);
            return emptyList();
        }

        return parseContent(entity ,Math.toIntExact(DAYS.between(fromDate, toDate)));
	}

    private ArrayList<Pricing> parseContent(HttpEntity entity, int size) {
        ArrayList<Pricing> priceList = Lists.newArrayList();
        try ( final BufferedReader reader = new BufferedReader(new InputStreamReader(entity.getContent())) )
        {
            String header = reader.readLine();
            String[] tokens = header.split(DELIMITER);
            OptionalInt dateIndex = findTokenPos(DATE, tokens);
            if(!dateIndex.isPresent())
                throw new ParseException();

            OptionalInt closePriceIndex = findTokenPos(CLOSE, tokens);
            if(!dateIndex.isPresent())
                throw new ParseException();

            priceList = new ArrayList<>(size);

            while(reader.ready())
            {
                String[] line = reader.readLine().split(DELIMITER);
                if(line.length != tokens.length)
                {
                   log.warn("Unexpected format of line " + line);
                   continue;
                }

                try {
                    priceList.add(Pricing.builder()
                            .tradeDate(LocalDate.parse(line[dateIndex.getAsInt()]))
                            .closePrice(new BigDecimal(line[closePriceIndex.getAsInt()]))
                            .build());
                }
                catch(DateTimeParseException | NumberFormatException e)
                {
                    log.warn("Wrong line", e);
                }
            }
        } catch (final IOException e) {
            log.error("Parsing error", e);
        }

        return priceList;

    }


    public List<?> fetchDividendData(String symbol, LocalDate fromDate, LocalDate toDate) {
		log.info("Acquiring dividend data for {} from {} to {}", symbol, fromDate, toDate);
		session.acquireCrumbWithTicker(symbol);

		String dividendURL = constructURL(DIVIDEND_FORMAT_URL, symbol, fromDate, toDate);
        HttpEntity entity = fetchURL(dividendURL, symbol, fromDate, toDate);
        if (entity == null) {
            log.warn("No dividend data available for {} from {} to {}", symbol, fromDate, toDate);
            return emptyList();
        }

        //TODO parse entity.getContent() and return data
        return emptyList();
	}
}
