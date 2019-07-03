package postgres.gen;

import java.sql.Connection;
import java.sql.SQLException;

import org.postgresql.util.PSQLException;

import lama.Query;
import lama.QueryAdapter;
import lama.Randomly;
import postgres.PostgresSchema;
import postgres.PostgresSchema.PostgresTable.TableType;

public class PostgresDiscardGenerator {

	public static Query create(PostgresSchema s) {
		StringBuilder sb = new StringBuilder();
		sb.append("DISCARD ");
		// prevent that DISCARD discards all tables (if they are TEMP tables)
		boolean hasNonTempTables = s.getDatabaseTables().stream().anyMatch(t -> t.getTableType() == TableType.STANDARD);
		String what;
		if (hasNonTempTables) {
			what = Randomly.fromOptions("ALL", "PLANS", "SEQUENCES", "TEMPORARY", "TEMP");
		} else {
			what = Randomly.fromOptions("PLANS", "SEQUENCES");
		}
		sb.append(what);
		return new QueryAdapter(sb.toString()) {
			@Override
			public void execute(Connection con) throws SQLException {
				try {
					super.execute(con);
				} catch (PSQLException e) {
					if (e.getMessage().contains("cannot run inside a transaction block")) {

					} else {
						throw e;
					}
				}
			}

			@Override
			public boolean couldAffectSchema() {
				return canDiscardTemporaryTables(what);
			}
		};
	}

	private static boolean canDiscardTemporaryTables(String what) {
		return what.contentEquals("TEMPORARY") || what.contentEquals("TEMP") || what.contentEquals("ALL");
	}
}
