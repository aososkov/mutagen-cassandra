package com.toddfast.mutagen.cassandra;

import com.toddfast.mutagen.Plan;

import java.io.IOException;

/**
 *
 *
 * @author Todd Fast
 */
public interface CassandraMutagen {

	/**
	 *
	 *
	 */
	void initialize(String rootResourcePath, String testResourcePath)
		throws IOException;


	/**
	 *
	 *
	 */
	Plan.Result<Integer> mutate();
}
