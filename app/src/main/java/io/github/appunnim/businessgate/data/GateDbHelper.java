package io.github.appunnim.businessgate.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public final class GateDbHelper extends SQLiteOpenHelper {
    private final Context context;
    public GateDbHelper(Context context) {
        super(context, "gate.db", null, 2, db -> {
            // The framework default deletes corrupt database files. Preserve choices for recovery.
            throw new android.database.sqlite.SQLiteDatabaseCorruptException("DATABASE_CORRUPT");
        });
        this.context = context; setWriteAheadLoggingEnabled(true);
    }
    @Override public void onConfigure(SQLiteDatabase db) { db.setForeignKeyConstraintsEnabled(true); }
    @Override public void onCreate(SQLiteDatabase db) { executeAsset(db, "schema.sql"); }
    private void executeAsset(SQLiteDatabase db, String asset) {
        try (var stream = context.getAssets().open(asset)) {
            for (String statement : new String(io.github.appunnim.businessgate.support.Bytes.read(stream), StandardCharsets.UTF_8).split(";"))
                if (!statement.trim().isEmpty()) db.execSQL(statement);
        } catch (IOException error) { throw new IllegalStateException("SCHEMA_UNAVAILABLE"); }
    }
    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion != 1 || newVersion != 2) throw new IllegalStateException("MIGRATION_UNSUPPORTED");
        // SQLiteOpenHelper wraps this migration and version update in one transaction.
        executeAsset(db, "migrations/1-2.sql");
    }
    @Override public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) { throw new IllegalStateException("DOWNGRADE_UNSUPPORTED"); }
}
