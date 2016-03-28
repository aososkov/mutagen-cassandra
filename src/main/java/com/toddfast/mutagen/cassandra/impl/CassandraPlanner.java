package com.toddfast.mutagen.cassandra.impl;

import com.toddfast.mutagen.Coordinator;
import com.toddfast.mutagen.MutagenException;
import com.toddfast.mutagen.Mutation;
import com.toddfast.mutagen.Plan;
import com.toddfast.mutagen.Subject;
import com.toddfast.mutagen.basic.BasicPlanner;
import com.toddfast.mutagen.cassandra.AbstractCassandraMutation;
import com.toddfast.mutagen.cassandra.dao.SchemaVersionDao;
import org.springframework.context.ApplicationContext;
import org.springframework.data.cassandra.core.CassandraOperations;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 *
 * @author Todd Fast
 */
public class CassandraPlanner extends BasicPlanner<Integer> {

	/**
	 *
	 *
	 */
	protected CassandraPlanner(List<Mutation<Integer>> mutations) {
		super(mutations,null);
	}


	/**
	 *
	 *
	 */
	public static List<Mutation<Integer>> loadMutations(CassandraOperations cassandraOperations, SchemaVersionDao schemaVersionDao, CassandraMutagenConfig config, Collection<String> resources) {

		List<Mutation<Integer>> result = new ArrayList<>();

		for (String resource: resources) {

			// Allow .sql files because some editors have syntax highlighting
			// for SQL but not CQL
			if (resource.endsWith(".cql") || resource.endsWith(".sql")) {
				CQLMutation mutation = new CQLMutation(cassandraOperations, schemaVersionDao, resource);
				mutation.setConfig(config);
				result.add(mutation);
			}
			else
			if (resource.endsWith(".class")) {
				result.add(
					loadMutationClass(cassandraOperations, schemaVersionDao, config, resource));
			}
			else {
				throw new IllegalArgumentException("Unknown type for "+
					"mutation resource \""+resource+"\"");
			}
		}

		return result;
	}


	/**
	 *
	 *
	 */
	private static Mutation<Integer> loadMutationClass(CassandraOperations cassandraOperations, SchemaVersionDao schemaVersionDao, CassandraMutagenConfig config, String resource) {

		assert resource.endsWith(".class"):
			"Class resource name \""+resource+"\" should end with .class";

		int index=resource.indexOf(".class");
		String className=resource.substring(0,index).replace('/','.');

		// Load the class specified by the resource
		Class<?> clazz=null;
		try {
			clazz=Class.forName(className);
            if (!AbstractCassandraMutation.class.isAssignableFrom(clazz)) {
                throw new MutagenException("Class [" + resource + "] doesn't inherit AbstractCassandraMutation");
            }
		}
		catch (ClassNotFoundException e) {
			// Should never happen
			throw new MutagenException("Could not load mutagen class \""+
				resource+"\"",e);
		}

		// Instantiate the class
		try {
			Constructor<?> constructor;
			AbstractCassandraMutation mutation=null;

			try {
				// Try a constructor taking a keyspace
				constructor = clazz.getConstructor(CassandraOperations.class, SchemaVersionDao.class);
				mutation = (AbstractCassandraMutation) constructor.newInstance(cassandraOperations, schemaVersionDao);
			}
			catch (NoSuchMethodException e) {
				// Wrong assumption
			}

			if (mutation==null) {
				// Try the null constructor
				try {
					constructor=clazz.getConstructor();
					mutation=(AbstractCassandraMutation) constructor.newInstance();
				}
				catch (NoSuchMethodException e) {
					throw new MutagenException("Could not find comparible "+
						"constructor for class \""+className+"\"",e);
				}
			}

            mutation.setConfig(config);

			return mutation;
		}
		catch (InstantiationException e) {
			throw new MutagenException("Could not instantiate class \""+
				className+"\"",e);
		}
		catch (InvocationTargetException e) {
			if (e.getTargetException() instanceof RuntimeException) {
				throw (RuntimeException)e.getTargetException();
			}
			else {
				throw new MutagenException("Exception instantiating class \""+
					className+"\"",e);
			}
		}
		catch (IllegalAccessException e) {
			throw new MutagenException("Could not access constructor for "+
				"mutation class \""+className+"\"",e);
		}
	}


	/**
	 *
	 *
	 */
	@Override
	protected Mutation.Context createContext(Subject<Integer> subject,
		Coordinator<Integer> coordinator) {
		return new CassandraContext(subject,coordinator);
	}


	/**
	 *
	 * 
	 */
	@Override
	public Plan<Integer> getPlan(Subject<Integer> subject,
			Coordinator<Integer> coordinator) {
		return super.getPlan(subject,coordinator);
	}
}
