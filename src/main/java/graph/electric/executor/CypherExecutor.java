package graph.electric.executor;

import org.neo4j.driver.v1.StatementResult;

import java.util.Iterator;
import java.util.Map;

/**
 * @author Michael Hunger @since 22.10.13
 */
public interface CypherExecutor {
    Iterator<Map<String, Object>> query(String statement, Map<String, Object> params);

    StatementResult queryResult(String statement, Map<String, Object> params);
}
