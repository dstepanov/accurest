package com.ofg.twitter.place;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.web.bind.annotation.RequestMethod.PUT;

@RestController
@RequestMapping("/api")
public class PairIdController {

	Logger log = LoggerFactory.getLogger(PairIdController.class);

	@RequestMapping(value = "{pairId}", method = PUT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public String getPlacesFromTweets(@PathVariable long pairId, @RequestBody final List<Tweet> tweets) {
		log.info("Inside PairIdController, doing very important logic");
		if (!tweets.get(0).getText().equals("Gonna see you at Warsaw")) {
			throw new IllegalArgumentException("Wrong text in tweet: " + tweets.get(0).getText());
		}
		return "{ \"path\" : \"/api/" + pairId + "\", \"correlationId\" : 123456 }";
	}

}
